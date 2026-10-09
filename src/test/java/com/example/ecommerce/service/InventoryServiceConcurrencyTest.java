package com.example.ecommerce.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryServiceConcurrencyTest {

    @Test
    void neverOversellsUnderHeavyContention() throws Exception {
        InventoryService inventory = new InventoryService();
        inventory.reset("P-TEST", 50);

        int buyers = 500;
        AtomicInteger success = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(buyers);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(buyers);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < buyers; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();                       // release all threads at the same instant
                        if (inventory.reserve("P-TEST", 1)) success.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await();
            go.countDown();
            done.await();
        }

        assertThat(success.get()).isEqualTo(50);
        assertThat(inventory.getStock("P-TEST").available()).isZero();
    }
}
