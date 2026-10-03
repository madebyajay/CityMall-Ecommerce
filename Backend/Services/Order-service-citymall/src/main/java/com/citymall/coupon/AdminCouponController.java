package com.citymall.coupon;

import com.citymall.orderService.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Admin coupon surface (full CRUD). URL-level gate ({@code hasRole("ADMIN")})
 * lives in SecurityConfig; {@code @PreAuthorize} is defence-in-depth.
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @Valid @RequestBody CouponRequest request) {
        CouponResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CouponResponse>builder()
                .message("Coupon created successfully")
                .statusCode("201").success(true).data(created).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> list(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable) {
        Page<CouponResponse> page = service.list(active, pageable);
        String msg = page.isEmpty() ? "No coupons found" : "Coupons fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<CouponResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CouponResponse>builder()
                .message("Coupon fetched successfully")
                .statusCode("200").success(true).data(service.getById(id)).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.<CouponResponse>builder()
                .message("Coupon updated successfully")
                .statusCode("200").success(true).data(service.update(id, request)).build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CouponResponse>> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody CouponStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<CouponResponse>builder()
                .message(request.active() ? "Coupon enabled" : "Coupon disabled")
                .statusCode("200").success(true).data(service.setStatus(id, request)).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Coupon deleted successfully")
                .statusCode("200").success(true).data(null).build());
    }
}
