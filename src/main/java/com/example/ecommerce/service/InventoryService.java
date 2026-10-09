package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.Stock;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * USE CASE 2: Protecting shared state in a singleton bean.
 * Two users buying the last item must never both succeed (no overselling).
 *
 * Lock-free compare-and-set on an AtomicInteger. In production the same idea is
 * "UPDATE stock SET qty = qty - ? WHERE id = ? AND qty >= ?" or optimistic locking (@Version).
 */
@Service
public class InventoryService {

    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        stock.put("P-100", new AtomicInteger(100));
        stock.put("P-FLASH", new AtomicInteger(50));
    }

    public Stock getStock(String productId) {
        return new Stock(productId, counter(productId).get());
    }

    /** @return true if reserved, false if insufficient stock. */
    public boolean reserve(String productId, int qty) {
        AtomicInteger counter = counter(productId);
        while (true) {
            int current = counter.get();
            if (current < qty) return false;                                 // not enough stock
            if (counter.compareAndSet(current, current - qty)) return true;  // atomic
            // another thread won the race, retry with fresh value
        }
    }

    public void release(String productId, int qty) {
        counter(productId).addAndGet(qty);
    }

    public void reset(String productId, int qty) {
        stock.put(productId, new AtomicInteger(qty));
    }

    private AtomicInteger counter(String productId) {
        AtomicInteger c = stock.get(productId);
        if (c == null) throw new IllegalArgumentException("Unknown product: " + productId);
        return c;
    }
}
