package com.ecommerce.service.impl;

import com.ecommerce.dto.coupon.CouponValidationResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CartRepository cartRepository;
    private final CouponValidator couponValidator;

    @Override
    public CouponValidationResponse validateCoupon(String code) {
        Long userId = SecurityUtils.getCurrentUserId();

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new InvalidRequestException("Your cart is empty — nothing to apply a coupon to");
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CouponValidator.ValidationOutcome outcome = couponValidator.tryValidate(code, userId, subtotal);

        if (!outcome.valid()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .reason(outcome.reason())
                    .build();
        }

        BigDecimal discount = couponValidator.calculateDiscount(subtotal, outcome.coupon());
        return CouponValidationResponse.builder()
                .valid(true)
                .subtotal(subtotal)
                .discountAmount(discount)
                .estimatedTotal(subtotal.subtract(discount))
                .build();
    }
}
