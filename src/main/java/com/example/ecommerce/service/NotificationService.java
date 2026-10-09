package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * USE CASE 3: Fire-and-forget background work with @Async.
 * The customer's HTTP response does not wait for email / invoice / analytics.
 *
 * NOTE: @Async only works when called from ANOTHER bean (proxy), and the
 * method runs in its own thread, so it is NOT part of the caller's transaction.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async("notificationExecutor")
    public void sendConfirmationEmail(Order order) {
        log.info("Sending confirmation email to {} for order {}", order.email(), order.orderId());
        simulate(800);
        log.info("Email sent for order {}", order.orderId());
    }

    @Async("notificationExecutor")
    public CompletableFuture<String> generateInvoice(Order order) {
        log.info("Generating invoice for {}", order.orderId());
        simulate(1000);
        return CompletableFuture.completedFuture("INV-" + order.orderId());
    }

    @Async("notificationExecutor")
    public void publishAnalyticsEvent(Order order) {
        log.info("Publishing analytics event for {}", order.orderId());
        simulate(300);
    }

    private void simulate(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
