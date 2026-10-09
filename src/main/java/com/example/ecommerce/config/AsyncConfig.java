package com.example.ecommerce.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * One pool per concern (bulkhead pattern): a slow email provider can never
 * starve report generation or product-page calls.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** Copies the MDC (requestId / trace id) from the caller thread to the worker thread. */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (context != null) MDC.setContextMap(context);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    /** Bounded pool for fire-and-forget work: emails, SMS, invoices, analytics. */
    @Bean("notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor(
            TaskDecorator decorator,
            @Value("${app.executor.notification.core-size}") int core,
            @Value("${app.executor.notification.max-size}") int max,
            @Value("${app.executor.notification.queue-capacity}") int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queue);
        ex.setThreadNamePrefix("notify-");
        ex.setTaskDecorator(decorator);
        // Back-pressure: when saturated, the caller runs the task instead of dropping it.
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        return ex;
    }

    /** Pool for CPU-ish aggregation work such as per-region report generation. */
    @Bean("reportExecutor")
    public ThreadPoolTaskExecutor reportExecutor(
            TaskDecorator decorator,
            @Value("${app.executor.report.core-size}") int core,
            @Value("${app.executor.report.max-size}") int max,
            @Value("${app.executor.report.queue-capacity}") int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queue);
        ex.setThreadNamePrefix("report-");
        ex.setTaskDecorator(decorator);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        return ex;
    }

    /**
     * Java 21 virtual-thread executor for blocking I/O fan-out (REST/DB calls).
     * Cheap to create, so no pool sizing is needed.
     */
    @Bean(name = "ioExecutor", destroyMethod = "close")
    public ExecutorService ioExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("io-vt-", 0).factory());
    }
}
