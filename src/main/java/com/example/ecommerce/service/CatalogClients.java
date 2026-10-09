package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulates remote microservices (pricing, reviews, recommendations) with latency.
 * In a real app these would be RestClient / JDBC calls.
 */
@Component
public class CatalogClients {

    public Price fetchPrice(String productId) {
        sleep(300);
        return new Price(productId, new BigDecimal("999.00"), "USD");
    }

    public List<Review> fetchReviews(String productId) {
        sleep(400);
        return List.of(new Review(productId, "alice", 5, "Great!"),
                       new Review(productId, "bob", 4, "Good value"));
    }

    public List<Recommendation> fetchRecommendations(String productId) {
        sleep(500);
        return List.of(new Recommendation("P-200", "Phone Case"),
                       new Recommendation("P-300", "Charger"));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
