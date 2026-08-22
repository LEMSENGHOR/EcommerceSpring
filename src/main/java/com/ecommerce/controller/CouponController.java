package com.ecommerce.controller;

import com.ecommerce.dto.coupon.CouponValidationResponse;
import com.ecommerce.dto.coupon.ValidateCouponRequest;
import com.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Any authenticated user — validates against their own current cart. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "Preview a coupon against the caller's own cart.")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Preview a coupon code",
            description = "Checks whether a code would be accepted for the caller's current cart — does "
                    + "NOT redeem it, create a usage record, or increment the coupon's used count. Only "
                    + "POST /api/orders (placing an order) actually redeems a coupon.")
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(request.getCode()));
    }
}
