package com.citymall.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
        @NotBlank String roleName
) {
}
