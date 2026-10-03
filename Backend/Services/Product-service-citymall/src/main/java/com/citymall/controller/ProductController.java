package com.citymall.controller;

import com.citymall.dto.ProductResponse;
import com.citymall.response.ApiResponse;
import com.citymall.response.PageResponse;
import com.citymall.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Public storefront — browsable with or without login, no writes.
 * Writes live in {@link AdminProductController}; order-service stock operations
 * live in {@link InternalProductController}.
 */
@Slf4j
@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService service;

    /** Paged active-only listing, optional category filter. */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/v1/products categoryId={} page={} size={}", categoryId, page, size);
        PageResponse<ProductResponse> response =
                service.findActiveProducts(categoryId, page, size);
        log.debug("Returning {} products (totalElements={})",
                response.content().size(), response.totalElements());
        return ResponseEntity.ok(response);
    }

    /** Storefront detail — 404s for unknown or admin-disabled products. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable("id") Integer id
    ) {
        log.info("GET /api/v1/products/{}", id);
        ProductResponse response = service.findActiveProductById(id);
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Product fetched successfully")
                        .success(true)
                        .data(response)
                        .build()
        );
    }

    /** Active-only keyword search across product + category names. */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/v1/products/search keyword='{}' page={} size={}", keyword, page, size);
        PageResponse<ProductResponse> response = service.searchProducts(keyword, page, size);
        log.debug("Search '{}' returned totalElements={}", keyword, response.totalElements());
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<ProductResponse>>builder()
                        .success(true)
                        .message("Search results fetched successfully")
                        .data(response)
                        .build()
        );
    }
}
