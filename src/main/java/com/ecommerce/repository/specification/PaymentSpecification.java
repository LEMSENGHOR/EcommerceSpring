package com.ecommerce.repository.specification;

import com.ecommerce.dto.payment.AdminPaymentFilterRequest;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.enums.PaymentMethod;
import com.ecommerce.entity.enums.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    private PaymentSpecification() {
    }

    public static Specification<Payment> withFilters(AdminPaymentFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), PaymentStatus.valueOf(filter.getStatus().toUpperCase())));
            }

            if (filter.getPaymentMethod() != null && !filter.getPaymentMethod().isBlank()) {
                predicates.add(cb.equal(root.get("paymentMethod"),
                        PaymentMethod.valueOf(filter.getPaymentMethod().toUpperCase())));
            }

            if (filter.getOrderId() != null) {
                predicates.add(cb.equal(root.get("order").get("id"), filter.getOrderId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
