package com.example.ecommerce.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Models {
    private Models() {}

    public record Price(String productId, BigDecimal amount, String currency) {}
    public record Stock(String productId, int available) {}
    public record Review(String productId, String user, int rating, String comment) {}
    public record Recommendation(String productId, String name) {}

    public record ProductPage(String productId, Price price, Stock stock,
                              List<Review> reviews, List<Recommendation> recommendations,
                              long timeTakenMs, String assembledBy) {}

    public record OrderRequest(@NotBlank String productId,
                               @Min(1) int quantity,
                               @NotBlank @Email String email) {}

    public record Order(String orderId, String productId, int quantity, String email,
                        BigDecimal total, Instant createdAt) {}

    public record RegionSales(String region, long orders, BigDecimal revenue) {}
    public record SalesReport(List<RegionSales> regions, BigDecimal totalRevenue, long timeTakenMs) {}
}
