package com.citymall.controller;

import com.citymall.dto.admin.AdminCategoryRequest;
import com.citymall.dto.admin.AdminCategoryResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin category surface. URL-level gate ({@code hasRole("ADMIN")}) lives in
 * SecurityConfig; {@code @PreAuthorize} is defence-in-depth.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> create(
            @Valid @RequestBody AdminCategoryRequest request) {
        AdminCategoryResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AdminCategoryResponse>builder()
                        .message("Category created successfully")
                        .success(true)
                        .data(response)
                        .build());
    }

    /** List top-level categories by default; {@code ?parentId=} lists subcategories. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCategoryResponse>>> list(
            @RequestParam(required = false) Integer parentId) {
        List<AdminCategoryResponse> response = service.list(parentId);
        String msg = response.isEmpty() ? "No categories found" : "Categories fetched successfully";
        return ResponseEntity.ok(
                ApiResponse.<List<AdminCategoryResponse>>builder()
                        .message(msg)
                        .success(true)
                        .data(response)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.<AdminCategoryResponse>builder()
                        .message("Category fetched successfully")
                        .success(true)
                        .data(service.getById(id))
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody AdminCategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<AdminCategoryResponse>builder()
                        .message("Category updated successfully")
                        .success(true)
                        .data(service.update(id, request))
                        .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Category deleted successfully")
                        .success(true)
                        .build());
    }
}
