package com.citymall.coupon;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /api/v1/admin/coupons/{id}/status}. */
public record CouponStatusRequest(

        @NotNull(message = "active flag is required (true=enable, false=disable)")
        Boolean active
) {
}
