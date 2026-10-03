package com.citymall.payment_service.payment;

public enum PaymentMethod {
    PAYPAL,
    CREDIT_CARD,
    VISA,
    MASTER_CARD,
    BITCOIN,
    // Accepted by order-service checkout — must stay name-compatible
    // with com.citymall.orderService.entity.PaymentMethod.
    UPI,
    WALLET
}
