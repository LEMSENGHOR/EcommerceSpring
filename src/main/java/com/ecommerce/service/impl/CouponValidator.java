package com.ecommerce.service.impl;

import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.enums.CouponStatus;
import com.ecommerce.entity.enums.DiscountType;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.repository.CouponRepository;
import com.ecommerce.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Coupon redemption rules — active status, date window, usage limit, minimum
 * order amount, one-use-per-user — plus discount math. Originally written
 * inline in OrderServiceImpl (Phase 10, before Coupon *management* existed);
 * extracted here in Phase 12 so the self-service preview endpoint
 * (CouponServiceImpl.validateCoupon) enforces the *exact same* rules checkout
 * does, rather than a second hand-maintained copy that could drift.
 *
 * Package-private: only OrderServiceImpl and CouponServiceImpl (both in this
 * package) need it. Same "shared implementation detail, not a public service"
 * pattern as OrderStockCoordinator and PaymentRefundCoordinator.
 */
@Service
@RequiredArgsConstructor
class CouponValidator {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /** Throws InvalidRequestException with a specific reason if the coupon can't be applied. */
    Coupon validate(String code, Long userId, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new InvalidRequestException("Coupon code '" + code + "' does not exist"));

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new InvalidRequestException("Coupon '" + code + "' is not active");
        }
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new InvalidRequestException("Coupon '" + code + "' is not valid at this time");
        }
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            throw new InvalidRequestException("Coupon '" + code + "' has reached its usage limit");
        }
        if (subtotal.compareTo(coupon.getMinimumAmount()) < 0) {
            throw new InvalidRequestException(
                    "Order subtotal does not meet the minimum amount required for coupon '" + code + "'");
        }
        if (!couponUsageRepository.findByCouponIdAndUserId(coupon.getId(), userId).isEmpty()) {
            throw new InvalidRequestException("You have already used coupon '" + code + "'");
        }

        return coupon;
    }

    /** Same rules as validate(), but returns a boolean + reason instead of throwing — for previews. */
    ValidationOutcome tryValidate(String code, Long userId, BigDecimal subtotal) {
        try {
            Coupon coupon = validate(code, userId, subtotal);
            return ValidationOutcome.valid(coupon);
        } catch (InvalidRequestException e) {
            return ValidationOutcome.invalid(e.getMessage());
        }
    }

    BigDecimal calculateDiscount(BigDecimal subtotal, Coupon coupon) {
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();

        // Never let a fixed-amount coupon (or a rounding edge case) push the total below zero.
        return discount.min(subtotal);
    }

    record ValidationOutcome(boolean valid, Coupon coupon, String reason) {
        static ValidationOutcome valid(Coupon coupon) {
            return new ValidationOutcome(true, coupon, null);
        }

        static ValidationOutcome invalid(String reason) {
            return new ValidationOutcome(false, null, reason);
        }
    }
}
