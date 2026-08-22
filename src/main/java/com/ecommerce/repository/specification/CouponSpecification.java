package com.ecommerce.repository.specification;

import com.ecommerce.dto.coupon.AdminCouponFilterRequest;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.enums.CouponStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CouponSpecification {

    private CouponSpecification() {
    }

    public static Specification<Coupon> withFilters(AdminCouponFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), CouponStatus.valueOf(filter.getStatus().toUpperCase())));
            }

            if (filter.getCode() != null && !filter.getCode().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + filter.getCode().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
