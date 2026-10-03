package com.citymall.dto.admin;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Integer productId,
        Integer quantityChange,
        Integer resultingQuantity,
        String reason,
        LocalDateTime createdAt
) {
}
