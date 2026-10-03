package com.citymall.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code PATCH /api/v1/admin/products/{id}/stock}.
 * Positive {@code quantityChange} adds stock, negative removes it.
 */
public record StockAdjustRequest(

        @NotNull(message = "quantityChange is required (positive=add, negative=remove)")
        Integer quantityChange,

        String reason
) {
}
