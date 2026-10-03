package com.citymall.controller;

import com.citymall.dto.admin.BlockCustomerRequest;
import com.citymall.dto.admin.CreateAdminRequest;
import com.citymall.dto.admin.DashboardSummaryResponse;
import com.citymall.dto.user.UserResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff surface for identity + customer management. ADMIN and SUPERADMIN both
 * reach these routes (SUPERADMIN-only work lives under
 * {@code /api/v1/superadmin/**}).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        UserResponse created = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .message("Admin created successfully")
                        .statusCode("201")
                        .success(true)
                        .data(created)
                        .build());
    }

    @PostMapping("/create-seller")
    public ResponseEntity<ApiResponse<UserResponse>> createSeller(
            @Valid @RequestBody CreateAdminRequest request) {
        UserResponse created = adminService.createSeller(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .message("Seller created successfully")
                        .statusCode("201")
                        .success(true)
                        .data(created)
                        .build());
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listCustomers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> page = adminService.listCustomers(pageable);
        String msg = page.isEmpty() ? "No customers found" : "Customers fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/sellers")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listSellers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> page = adminService.listSellers(pageable);
        String msg = page.isEmpty() ? "No sellers found" : "Sellers fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message("Customer fetched successfully")
                .statusCode("200").success(true).data(adminService.getCustomer(id)).build());
    }

    @PatchMapping("/customers/{id}/block")
    public ResponseEntity<ApiResponse<UserResponse>> blockCustomer(
            @PathVariable Long id,
            @Valid @RequestBody BlockCustomerRequest request) {
        UserResponse updated = adminService.blockCustomer(id, request);
        String msg = Boolean.TRUE.equals(request.blocked())
                ? "Customer blocked successfully" : "Customer unblocked successfully";
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message(msg).statusCode("200").success(true).data(updated).build());
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> dashboardSummary() {
        return ResponseEntity.ok(ApiResponse.<DashboardSummaryResponse>builder()
                .message("Dashboard summary fetched successfully")
                .statusCode("200").success(true).data(adminService.dashboardSummary()).build());
    }
}
