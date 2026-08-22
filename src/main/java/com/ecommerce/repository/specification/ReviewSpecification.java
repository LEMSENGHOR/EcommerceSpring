package com.ecommerce.repository.specification;

import com.ecommerce.dto.review.AdminReviewFilterRequest;
import com.ecommerce.entity.Review;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReviewSpecification {

    private ReviewSpecification() {
    }

    public static Specification<Review> withFilters(AdminReviewFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProductId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), filter.getProductId()));
            }
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }
            if (filter.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.getMinRating()));
            }
            if (filter.getMaxRating() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), filter.getMaxRating()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
