package com.citymall.coupon;

import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        if (coupon == null) return null;
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getValidFrom(),
                coupon.getValidTo(),
                coupon.getActive(),
                coupon.getCreatedDate(),
                coupon.getLastModifiedDate());
    }
}
