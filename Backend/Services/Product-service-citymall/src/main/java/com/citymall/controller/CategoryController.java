package com.citymall.controller;

import com.citymall.dto.CategoryResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public category browsing — read-only. Writes live in
 * {@code AdminCategoryController} ({@code /api/v1/admin/categories}).
 */
@Slf4j
@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategory() {
        log.info("GET /api/v1/categories");
        List<CategoryResponse> allCategory = service.findAllCategories();
        String msg = allCategory.isEmpty() ? "No Category Found" : "Category fetched successfully";
        log.debug("Returning {} categories", allCategory.size());
        return ResponseEntity.ok(
                ApiResponse.<List<CategoryResponse>>builder()
                        .message(msg)
                        .success(true)
                        .data(allCategory)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Integer id) {
        log.info("GET /api/v1/categories/{}", id);
        CategoryResponse response = service.findCategoryById(id);
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .message("Category fetched successfully")
                        .data(response)
                        .success(true)
                        .build()
        );
    }
}
