package com.citymall.payment;

import com.citymall.customer.CustomerResponse;
import com.citymall.orderService.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Mirrors payment-service's {@code PaymentRequest} JSON shape field-for-field:
 * {@code amount, paymentMethod, orderId, orderReference, customer{id, firstname,
 * lastname, email}}. {@code paymentMethod} must stay name-compatible with
 * payment-service's {@code PaymentMethod} enum.
 */
public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Long orderId,
        String orderReference,
        CustomerResponse customer
) {
}
