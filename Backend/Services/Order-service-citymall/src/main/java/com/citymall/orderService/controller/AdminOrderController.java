package com.citymall.orderService.controller;

import com.citymall.orderService.dto.OrderResponse;
import com.citymall.orderService.dto.OrderStatusRequest;
import com.citymall.orderService.response.ApiResponse;
import com.citymall.orderService.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Admin order surface. URL-level gate ({@code hasRole("ADMIN")}) lives in
 * SecurityConfig; {@code @PreAuthorize} is defence-in-depth.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService service;

    /** Paginated, filterable by status / customer / date range. */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable) {
        Page<OrderResponse> page = service.list(status, customerId, from, to, pageable);
        String msg = page.isEmpty() ? "No orders found" : "Orders fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<OrderResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order fetched successfully")
                .statusCode("200").success(true).data(service.getById(id)).build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order status updated successfully")
                .statusCode("200").success(true).data(service.updateStatus(id, request)).build());
    }
}
