package com.citymall.orderLineService;

import java.math.BigDecimal;

public record OrderLineResponse(
        Integer id,
        Integer productId,
        String productName,
        BigDecimal price,
        Integer quantity
) {
}
