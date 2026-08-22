package com.ecommerce.service;

import com.ecommerce.dto.coupon.CouponValidationResponse;

public interface CouponService {

    /**
     * Checks whether `code` would be accepted at checkout for the caller's
     * *current cart* — same rules OrderService.placeOrder enforces, but this
     * does not redeem the coupon (no CouponUsage row, no usedCount increment).
     * Lets the frontend show "Apply" feedback on the cart page before checkout.
     */
    CouponValidationResponse validateCoupon(String code);
}
