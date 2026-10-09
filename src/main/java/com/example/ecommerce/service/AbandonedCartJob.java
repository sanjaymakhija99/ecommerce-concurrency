package com.example.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** USE CASE 6: Scheduled background job (runs on the scheduler thread, not a request thread). */
@Component
@EnableScheduling
public class AbandonedCartJob {
    private static final Logger log = LoggerFactory.getLogger(AbandonedCartJob.class);

    @Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")
    public void remindAbandonedCarts() {
        log.info("Scheduled job: checking abandoned carts (would send reminders)");
    }
}
