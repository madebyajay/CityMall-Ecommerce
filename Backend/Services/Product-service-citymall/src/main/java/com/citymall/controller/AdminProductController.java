package com.citymall.controller;

import com.citymall.dto.ProductRequest;
import com.citymall.dto.ProductResponse;
import com.citymall.dto.admin.ProductStatusRequest;
import com.citymall.dto.admin.StockAdjustRequest;
import com.citymall.dto.admin.StockMovementResponse;
import com.citymall.response.ApiResponse;
import com.citymall.response.PageResponse;
import com.citymall.service.AdminProductService;
import com.citymall.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin product surface. URL-level gate ({@code hasRole("ADMIN")}) lives in
 * SecurityConfig; {@code @PreAuthorize} is defence-in-depth.
 * Create/update/delete reuse {@link ProductService} (same validation + cache
 * eviction as the public controller).
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@Validated
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final AdminProductService adminService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) {
        log.info("POST /api/v1/admin/products name='{}' categoryId={}", request.name(), request.categoryId());
        ProductResponse response = productService.createProduct(request);
        log.info("Admin created product id={}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ProductResponse>builder()
                        .message("Product created successfully")
                        .success(true)
                        .data(response)
                        .build());
    }

    /** Paginated, filterable by category/status/keyword (covers disabled too). */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> list(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/admin/products categoryId={} active={} keyword='{}' page={} size={}",
                categoryId, active, keyword, page, size);
        PageResponse<ProductResponse> response =
                adminService.list(categoryId, active, keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<ProductResponse>>builder()
                        .message("Products fetched successfully")
                        .success(true)
                        .data(response)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Integer id) {
        log.info("GET /api/v1/admin/products/{}", id);
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Product fetched successfully")
                        .success(true)
                        .data(productService.findProductById(id))
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request) {
        log.info("PUT /api/v1/admin/products/{} name='{}'", id, request.name());
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Product updated successfully")
                        .success(true)
                        .data(productService.updateProduct(id, request))
                        .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        log.warn("DELETE /api/v1/admin/products/{}", id);
        productService.deleteProductById(id);
        log.info("Admin deleted product id={}", id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Product deleted successfully")
                        .success(true)
                        .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> setStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ProductStatusRequest request) {
        log.info("PATCH /api/v1/admin/products/{}/status active={}", id, request.active());
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message(request.active() ? "Product enabled" : "Product disabled")
                        .success(true)
                        .data(adminService.setStatus(id, request))
                        .build());
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
            @PathVariable Integer id,
            @Valid @RequestBody StockAdjustRequest request) {
        log.info("PATCH /api/v1/admin/products/{}/stock quantityChange={} reason='{}'",
                id, request.quantityChange(), request.reason());
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Stock adjusted successfully")
                        .success(true)
                        .data(adminService.adjustStock(id, request))
                        .build());
    }

    @GetMapping("/{id}/stock-movements")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>> stockHistory(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/admin/products/{}/stock-movements page={} size={}", id, page, size);
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<StockMovementResponse>>builder()
                        .message("Stock movements fetched successfully")
                        .success(true)
                        .data(adminService.stockHistory(id, page, size))
                        .build());
    }
}
