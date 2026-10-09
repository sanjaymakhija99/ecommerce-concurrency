package com.example.ecommerce.service;

import com.example.ecommerce.model.Models.RegionSales;
import com.example.ecommerce.model.Models.SalesReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * USE CASE 5: Split one big job into partitions, run them on a dedicated pool, merge results.
 */
@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final List<String> REGIONS =
            List.of("NORTH", "SOUTH", "EAST", "WEST", "EUROPE", "ASIA", "AFRICA", "OCEANIA");

    private final ThreadPoolTaskExecutor reportExecutor;

    public ReportService(@Qualifier("reportExecutor") ThreadPoolTaskExecutor reportExecutor) {
        this.reportExecutor = reportExecutor;
    }

    public SalesReport generate() {
        long start = System.nanoTime();

        List<CompletableFuture<RegionSales>> futures = REGIONS.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> computeRegion(r), reportExecutor))
                .toList();

        List<RegionSales> regions = futures.stream()
                .map(f -> f.orTimeout(10, TimeUnit.SECONDS).join())
                .toList();

        BigDecimal total = regions.stream().map(RegionSales::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("Report generated in {} ms across {} regions", ms, regions.size());
        return new SalesReport(regions, total, ms);
    }

    private RegionSales computeRegion(String region) {
        log.info("Computing region {}", region);
        try {
            Thread.sleep(600);   // simulate heavy query
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long orders = Math.abs(region.hashCode() % 1000);
        return new RegionSales(region, orders, BigDecimal.valueOf(orders * 49L));
    }
}
