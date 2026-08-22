package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.coupon.AdminCouponFilterRequest;
import com.ecommerce.dto.coupon.BulkGenerateCouponRequest;
import com.ecommerce.dto.coupon.CouponRequest;
import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.dto.coupon.CouponUsageResponse;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.enums.CouponStatus;
import com.ecommerce.entity.enums.DiscountType;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceInUseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.CouponMapper;
import com.ecommerce.repository.CouponRepository;
import com.ecommerce.repository.CouponUsageRepository;
import com.ecommerce.repository.specification.CouponSpecification;
import com.ecommerce.service.AdminCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCouponServiceImpl implements AdminCouponService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 — avoids ambiguity
    private static final int RANDOM_SUFFIX_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("A coupon with code '" + request.getCode() + "' already exists");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountType(parseDiscountTypeOrThrow(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .minimumAmount(request.getMinimumAmount())
                .maxUsage(request.getMaxUsage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return CouponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    public List<CouponResponse> bulkGenerateCoupons(BulkGenerateCouponRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        DiscountType discountType = parseDiscountTypeOrThrow(request.getDiscountType());
        String prefix = request.getCodePrefix() != null && !request.getCodePrefix().isBlank()
                ? request.getCodePrefix().trim().toUpperCase() + "-"
                : "";

        List<Coupon> generated = new ArrayList<>(request.getCount());
        // Track codes generated in THIS batch too — existsByCode only sees
        // already-persisted coupons, so without this a collision between two
        // codes generated in the same loop (before saveAll flushes) would slip
        // through. Astronomically unlikely at 8 random chars from a 33-char
        // alphabet, but "unlikely" isn't "impossible", and a duplicate code
        // would fail loudly at saveAll on the unique constraint anyway — better
        // to prevent it than let a large batch occasionally 500.
        Set<String> codesInThisBatch = new HashSet<>();
        for (int i = 0; i < request.getCount(); i++) {
            Coupon coupon = Coupon.builder()
                    .code(generateUniqueCode(prefix, codesInThisBatch))
                    .discountType(discountType)
                    .discountValue(request.getDiscountValue())
                    .minimumAmount(request.getMinimumAmount())
                    .maxUsage(request.getMaxUsage())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .build();
            generated.add(coupon);
        }

        return CouponMapper.toResponseList(couponRepository.saveAll(generated));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CouponResponse> getAllCoupons(AdminCouponFilterRequest filter, Pageable pageable) {
        Page<Coupon> page = couponRepository.findAll(CouponSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(CouponMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long id) {
        return CouponMapper.toResponse(findCouponOrThrow(id));
    }

    @Override
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = findCouponOrThrow(id);

        if (!coupon.getCode().equalsIgnoreCase(request.getCode())
                && couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("A coupon with code '" + request.getCode() + "' already exists");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        coupon.setCode(request.getCode());
        coupon.setDiscountType(parseDiscountTypeOrThrow(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumAmount(request.getMinimumAmount());
        coupon.setMaxUsage(request.getMaxUsage());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        return CouponMapper.toResponse(coupon);
    }

    @Override
    public CouponResponse deactivateCoupon(Long id) {
        Coupon coupon = findCouponOrThrow(id);
        coupon.setStatus(CouponStatus.INACTIVE);
        return CouponMapper.toResponse(coupon);
    }

    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = findCouponOrThrow(id);

        // Unlike Category/Brand/Product/User's deletion gaps (flagged in earlier
        // phases), this pre-checks usage and returns a clean 409 instead of
        // letting a raw FK violation on coupon_usages surface as a 500 —
        // applying that lesson now rather than deferring it again. Uses
        // ResourceInUseException (409), not InvalidRequestException (400) —
        // this was originally InvalidRequestException here, a status-code
        // mismatch fixed as part of introducing ResourceInUseException in Phase 15.
        if (couponUsageRepository.countByCouponId(id) > 0) {
            throw new ResourceInUseException(
                    "Coupon '" + coupon.getCode() + "' has been used and cannot be deleted — deactivate it instead");
        }

        couponRepository.delete(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsages(Long id) {
        findCouponOrThrow(id); // existence check
        return CouponMapper.toUsageResponseList(couponUsageRepository.findByCouponIdOrderByUsedAtDesc(id));
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Coupon findCouponOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }

    private DiscountType parseDiscountTypeOrThrow(String value) {
        try {
            return DiscountType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown discount type: " + value);
        }
    }

    private void validateDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidRequestException("startDate must be before endDate");
        }
    }

    private String generateUniqueCode(String prefix, Set<String> codesInThisBatch) {
        String candidate;
        do {
            StringBuilder suffix = new StringBuilder(RANDOM_SUFFIX_LENGTH);
            for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
                suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            candidate = prefix + suffix;
        } while (codesInThisBatch.contains(candidate) || couponRepository.existsByCode(candidate));
        codesInThisBatch.add(candidate);
        return candidate;
    }
}
