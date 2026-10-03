package com.citymall.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Body for admin category create/update. {@code parentId} is optional —
 * null means top-level category.
 */
public record AdminCategoryRequest(

        @NotBlank(message = "Category name is required")
        String name,

        @NotBlank(message = "Category description is required")
        String description,

        @Positive(message = "Parent category ID must be greater than 0")
        Integer parentId
) {
}
