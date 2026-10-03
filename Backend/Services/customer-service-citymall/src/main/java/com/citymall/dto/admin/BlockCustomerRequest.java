package com.citymall.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code PATCH /api/v1/admin/customers/{id}/block}.
 * {@code blocked=true} blocks the account, {@code false} unblocks it.
 */
public record BlockCustomerRequest(

        @NotNull(message = "blocked flag is required")
        Boolean blocked,

        String reason
) {
}
