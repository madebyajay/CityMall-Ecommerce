package com.citymall.dto.permission;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank String module,
        @NotBlank String apiEndpoint,
        String description
) {
}
