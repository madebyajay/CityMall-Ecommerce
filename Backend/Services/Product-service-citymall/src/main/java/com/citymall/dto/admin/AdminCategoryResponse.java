package com.citymall.dto.admin;

public record AdminCategoryResponse(
        Integer id,
        String name,
        String description,
        Integer parentId,
        String parentName
) {
}
