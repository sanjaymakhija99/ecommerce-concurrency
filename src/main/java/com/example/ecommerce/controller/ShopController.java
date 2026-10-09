package com.example.ecommerce.controller;

import com.example.ecommerce.model.Models.*;
import com.example.ecommerce.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShopController {

    private final ProductPageService pageService;
    private final OrderService orderService;
    private final InventoryService inventory;
    private final FlashSaleService flashSale;
    private final ReportService reportService;

    public ShopController(ProductPageService pageService, OrderService orderService,
                          InventoryService inventory, FlashSaleService flashSale,
                          ReportService reportService) {
        this.pageService = pageService;
        this.orderService = orderService;
        this.inventory = inventory;
        this.flashSale = flashSale;
        this.reportService = reportService;
    }

    @GetMapping("/products/{id}/page")
    public ProductPage productPage(@PathVariable String id) {
        return pageService.buildPageParallel(id);
    }

    @GetMapping("/products/{id}/page-sequential")
    public ProductPage productPageSequential(@PathVariable String id) {
        return pageService.buildPageSequential(id);
    }

    @GetMapping("/inventory/{id}")
    public Stock stock(@PathVariable String id) {
        return inventory.getStock(id);
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> order(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req));
    }

    @PostMapping("/flash-sale/{productId}/buy")
    public ResponseEntity<?> flashBuy(@PathVariable String productId, @RequestParam String email)
            throws InterruptedException {
        return flashSale.buy(productId, email)
                .<ResponseEntity<?>>map(o -> ResponseEntity.status(HttpStatus.CREATED).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too busy, please retry")));
    }

    @GetMapping("/reports/sales")
    public SalesReport salesReport() {
        return reportService.generate();
    }

    @ExceptionHandler(OrderService.OutOfStockException.class)
    public ResponseEntity<Map<String, String>> outOfStock(OrderService.OutOfStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
