package com.ecommerce.repository.specification;

import com.ecommerce.dto.order.AdminOrderFilterRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<Order> withFilters(AdminOrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), OrderStatus.valueOf(filter.getStatus().toUpperCase())));
            }

            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }

            if (filter.getOrderNumber() != null && !filter.getOrderNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("orderNumber")),
                        "%" + filter.getOrderNumber().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
