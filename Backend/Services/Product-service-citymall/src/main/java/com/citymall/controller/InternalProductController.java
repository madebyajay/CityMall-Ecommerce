package com.citymall.controller;

import com.citymall.dto.PurchaseRequest;
import com.citymall.dto.PurchaseResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Service-to-service stock operations for order-service OpenFeign
 * ({@code product-url: http://localhost:8050/api/v1/internal/products}).
 * No user JWT needed — reachable only in-cluster/dev. If this is ever exposed
 * publicly, gate it behind the shared {@code X-Internal-Secret} like
 * customer-service does for its {@code /api/v1/internal/**} surface.
 */
@Slf4j
@RestController
@RequestMapping("api/v1/internal/products")
@RequiredArgsConstructor
@Validated
public class InternalProductController {

    private final ProductService service;

    /** Availability check without touching inventory (checkout pre-step). */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> validateProduct(
            @Valid @RequestBody List<PurchaseRequest> request
    ) {
        log.info("POST /api/v1/internal/products/validate items={}", request.size());
        log.debug("Validate payload: {}", request);
        List<PurchaseResponse> productPurchaseResponses = service.validateProducts(request);
        log.info("Validated {} items", productPurchaseResponses.size());
        return ResponseEntity.ok(
                ApiResponse.<List<PurchaseResponse>>builder()
                        .message("Product list validated")
                        .success(true)
                        .data(productPurchaseResponses)
                        .build()
        );
    }

    /** Availability check + stock deduction (order commit step). */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> purchaseProduct(
            @Valid @RequestBody List<PurchaseRequest> request
    ) {
        log.info("POST /api/v1/internal/products/purchase items={}", request.size());
        log.debug("Purchase payload: {}", request);
        List<PurchaseResponse> productPurchaseResponses = service.purchaseProducts(request);
        log.info("Inventory updated for {} items", productPurchaseResponses.size());
        return ResponseEntity.ok(
                ApiResponse.<List<PurchaseResponse>>builder()
                        .message("Inventory updated successfully")
                        .success(true)
                        .data(productPurchaseResponses)
                        .build()
        );
    }
}
