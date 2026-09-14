package com.citymall.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(

        @NotBlank(message = "Role name is required")
        @Size(min = 3, max = 50, message = "Role name must be 3-50 characters")
        String name,

        String description
) {
}
