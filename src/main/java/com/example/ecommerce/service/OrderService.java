package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.Order;
import com.example.ecommerce.model.Models.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;

/** Checkout flow: reserve stock synchronously, everything else asynchronously. */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal UNIT_PRICE = new BigDecimal("999.00");

    private final InventoryService inventory;
    private final NotificationService notifications;

    private final List<Order> orders = new CopyOnWriteArrayList<>();   // thread-safe in-memory "DB"
    private final LongAdder totalOrders = new LongAdder();             // high-contention counter

    public OrderService(InventoryService inventory, NotificationService notifications) {
        this.inventory = inventory;
        this.notifications = notifications;
    }

    public Order placeOrder(OrderRequest req) {
        if (!inventory.reserve(req.productId(), req.quantity())) {
            throw new OutOfStockException("Insufficient stock for " + req.productId());
        }

        Order order;
        try {
            order = new Order(UUID.randomUUID().toString().substring(0, 8), req.productId(),
                    req.quantity(), req.email(),
                    UNIT_PRICE.multiply(BigDecimal.valueOf(req.quantity())), Instant.now());
            orders.add(order);
            totalOrders.increment();
        } catch (RuntimeException e) {
            inventory.release(req.productId(), req.quantity());   // compensate
            throw e;
        }

        // Non-critical work leaves the request thread immediately.
        notifications.sendConfirmationEmail(order);
        notifications.generateInvoice(order);
        notifications.publishAnalyticsEvent(order);

        log.info("Order {} placed; async tasks dispatched", order.orderId());
        return order;
    }

    public List<Order> allOrders() { return List.copyOf(orders); }
    public long totalOrders() { return totalOrders.sum(); }

    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException(String msg) { super(msg); }
    }
}
