package com.citymall.payment_service.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Accepts both the canonical {@code orderReference} and the legacy
 * {@code orderNumber} JSON name sent by older order-service builds.
 * Unknown fields are ignored so Feign shape drift fails open, not 400.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentRequest(
        Integer id,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Long orderId,
        @JsonAlias("orderNumber") String orderReference,
        Customer customer
) {
}
