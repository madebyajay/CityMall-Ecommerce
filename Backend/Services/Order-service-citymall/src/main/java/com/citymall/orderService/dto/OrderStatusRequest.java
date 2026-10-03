package com.citymall.orderService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body for {@code PATCH /api/v1/admin/orders/{id}/status}.
 * Value must be a valid {@code OrderStatus} name (e.g. CONFIRMED, SHIPPED).
 */
public record OrderStatusRequest(

        @NotBlank(message = "status is required")
        @Pattern(regexp = "^(PENDING|PAYMENT_PENDING|PAID|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)$",
                message = "Invalid order status")
        String status
) {
}
