package com.citymall.dto.admin;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /api/v1/admin/products/{id}/status}. */
public record ProductStatusRequest(

        @NotNull(message = "active flag is required (true=enable, false=disable)")
        Boolean active
) {
}
