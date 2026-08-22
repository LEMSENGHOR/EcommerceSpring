package com.ecommerce.repository;

import com.ecommerce.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    List<CouponUsage> findByCouponIdAndUserId(Long couponId, Long userId);
    long countByCouponId(Long couponId);
    List<CouponUsage> findByCouponIdOrderByUsedAtDesc(Long couponId);
}
