package com.citymall.dto.permission;

public record PermissionResponse(
        Long id,
        String module,
        String apiEndpoint,
        String description,
        Boolean activeStatus
) {
}
