package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.coupon.AdminCouponFilterRequest;
import com.ecommerce.dto.coupon.BulkGenerateCouponRequest;
import com.ecommerce.dto.coupon.CouponRequest;
import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.dto.coupon.CouponUsageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminCouponService {

    CouponResponse createCoupon(CouponRequest request);

    List<CouponResponse> bulkGenerateCoupons(BulkGenerateCouponRequest request);

    PagedResponse<CouponResponse> getAllCoupons(AdminCouponFilterRequest filter, Pageable pageable);

    CouponResponse getCouponById(Long id);

    CouponResponse updateCoupon(Long id, CouponRequest request);

    CouponResponse deactivateCoupon(Long id);

    /** Hard delete — blocked with a 409 if the coupon has ever been redeemed; use deactivate instead. */
    void deleteCoupon(Long id);

    List<CouponUsageResponse> getCouponUsages(Long id);
}
