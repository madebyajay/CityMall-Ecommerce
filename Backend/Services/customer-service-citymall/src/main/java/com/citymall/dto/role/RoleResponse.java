package com.citymall.dto.role;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Boolean activeStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
