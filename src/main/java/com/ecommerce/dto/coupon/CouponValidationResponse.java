package com.ecommerce.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A preview, not a redemption — validating a coupon does NOT create a
 * CouponUsage row or increment usedCount. Only actually placing an order
 * with the code (Phase 10's PlaceOrderRequest.couponCode) redeems it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResponse {
    private boolean valid;
    private String reason;

    /** Null when valid is false. */
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal estimatedTotal;
}
