package com.citymall.dto.internal;

import com.citymall.entity.User;

/**
 * Feign-friendly customer shape consumed by order-service
 * ({@code id/firstname/lastname/email}). Field names must stay exactly as-is.
 */
public record CustomerSummary(
        String id,
        String firstname,
        String lastname,
        String email
) {
    public static CustomerSummary from(User user) {
        return new CustomerSummary(
                String.valueOf(user.getUserId()),
                // Never null — payment-service renders these into emails.
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : "",
                user.getEmailId());
    }
}
