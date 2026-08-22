package com.ecommerce.mapper;

import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.dto.coupon.CouponUsageResponse;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.CouponUsage;

import java.util.List;

public class CouponMapper {

    private CouponMapper() {
    }

    public static CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null)
                .discountValue(coupon.getDiscountValue())
                .minimumAmount(coupon.getMinimumAmount())
                .maxUsage(coupon.getMaxUsage())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .status(coupon.getStatus() != null ? coupon.getStatus().name() : null)
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    public static List<CouponResponse> toResponseList(List<Coupon> coupons) {
        return coupons.stream().map(CouponMapper::toResponse).toList();
    }

    public static CouponUsageResponse toUsageResponse(CouponUsage usage) {
        return CouponUsageResponse.builder()
                .id(usage.getId())
                .userId(usage.getUser() != null ? usage.getUser().getId() : null)
                .userEmail(usage.getUser() != null ? usage.getUser().getEmail() : null)
                .orderId(usage.getOrder() != null ? usage.getOrder().getId() : null)
                .orderNumber(usage.getOrder() != null ? usage.getOrder().getOrderNumber() : null)
                .usedAt(usage.getUsedAt())
                .build();
    }

    public static List<CouponUsageResponse> toUsageResponseList(List<CouponUsage> usages) {
        return usages.stream().map(CouponMapper::toUsageResponse).toList();
    }
}
