package com.citymall.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignPermissionsRequest(
        @NotBlank String roleName,
        @NotEmpty List<Long> permissionIds
) {
}
