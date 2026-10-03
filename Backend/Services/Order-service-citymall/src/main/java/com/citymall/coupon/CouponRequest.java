package com.citymall.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Body for admin coupon create/update. */
public record CouponRequest(

        @NotBlank(message = "Coupon code is required")
        @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
                message = "Code must be 3-50 chars: uppercase letters, digits, - or _")
        String code,

        String description,

        @NotNull(message = "Discount type is required (PERCENTAGE or FLAT)")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be positive")
        BigDecimal discountValue,

        BigDecimal minOrderAmount,

        @NotNull(message = "validFrom is required")
        LocalDateTime validFrom,

        @NotNull(message = "validTo is required")
        LocalDateTime validTo
) {
}
