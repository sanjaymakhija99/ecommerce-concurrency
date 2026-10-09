package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * USE CASE 1: Parallel fan-out inside ONE request.
 * Sequential: 300 + 400 + 500 ms = ~1200 ms. Parallel: ~500 ms (slowest call).
 */
@Service
public class ProductPageService {
    private static final Logger log = LoggerFactory.getLogger(ProductPageService.class);

    private final CatalogClients clients;
    private final InventoryService inventory;
    private final ExecutorService ioExecutor;

    public ProductPageService(CatalogClients clients, InventoryService inventory,
                              @Qualifier("ioExecutor") ExecutorService ioExecutor) {
        this.clients = clients;
        this.inventory = inventory;
        this.ioExecutor = ioExecutor;
    }

    public ProductPage buildPageParallel(String productId) {
        long start = System.nanoTime();

        CompletableFuture<Price> price =
                CompletableFuture.supplyAsync(() -> clients.fetchPrice(productId), ioExecutor);
        CompletableFuture<Stock> stock =
                CompletableFuture.supplyAsync(() -> inventory.getStock(productId), ioExecutor);
        CompletableFuture<List<Review>> reviews =
                CompletableFuture.supplyAsync(() -> clients.fetchReviews(productId), ioExecutor)
                        // graceful degradation: reviews are non-critical
                        .completeOnTimeout(List.of(), 2, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("Reviews failed, degrading: {}", ex.getMessage());
                            return List.of();
                        });
        CompletableFuture<List<Recommendation>> recs =
                CompletableFuture.supplyAsync(() -> clients.fetchRecommendations(productId), ioExecutor)
                        .completeOnTimeout(List.of(), 2, TimeUnit.SECONDS)
                        .exceptionally(ex -> List.of());

        CompletableFuture.allOf(price, stock, reviews, recs)
                .orTimeout(5, TimeUnit.SECONDS)   // hard upper bound for the whole page
                .join();

        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("Product page {} built in {} ms (parallel)", productId, ms);
        return new ProductPage(productId, price.join(), stock.join(), reviews.join(), recs.join(),
                ms, Thread.currentThread().toString());
    }

    /** Same work, sequential, for comparison. */
    public ProductPage buildPageSequential(String productId) {
        long start = System.nanoTime();
        Price price = clients.fetchPrice(productId);
        Stock stock = inventory.getStock(productId);
        List<Review> reviews = clients.fetchReviews(productId);
        List<Recommendation> recs = clients.fetchRecommendations(productId);
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("Product page {} built in {} ms (sequential)", productId, ms);
        return new ProductPage(productId, price, stock, reviews, recs, ms, Thread.currentThread().toString());
    }
}
