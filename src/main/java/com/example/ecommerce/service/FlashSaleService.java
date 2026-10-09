package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.Order;
import com.example.ecommerce.model.Models.OrderRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * USE CASE 4: Throttling during a flash sale.
 * A Semaphore caps how many checkouts hit the (expensive) downstream systems at once.
 * Excess requests wait briefly, then are rejected quickly instead of piling up.
 */
@Service
public class FlashSaleService {

    private final Semaphore permits;
    private final OrderService orderService;

    public FlashSaleService(OrderService orderService,
                            @Value("${app.flash-sale.max-concurrent}") int maxConcurrent) {
        this.orderService = orderService;
        this.permits = new Semaphore(maxConcurrent, true);
    }

    public Optional<Order> buy(String productId, String email) throws InterruptedException {
        if (!permits.tryAcquire(500, TimeUnit.MILLISECONDS)) {
            return Optional.empty();                       // "too busy, try again"
        }
        try {
            return Optional.of(orderService.placeOrder(new OrderRequest(productId, 1, email)));
        } finally {
            permits.release();
        }
    }

    public int availablePermits() { return permits.availablePermits(); }
}
