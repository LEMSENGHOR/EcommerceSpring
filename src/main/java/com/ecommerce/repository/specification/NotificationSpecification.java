package com.ecommerce.repository.specification;

import com.ecommerce.dto.notification.AdminNotificationFilterRequest;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.enums.NotificationType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class NotificationSpecification {

    private NotificationSpecification() {
    }

    public static Specification<Notification> withFilters(AdminNotificationFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }
            if (filter.getType() != null && !filter.getType().isBlank()) {
                predicates.add(cb.equal(root.get("type"), NotificationType.valueOf(filter.getType().toUpperCase())));
            }
            if (filter.getIsRead() != null) {
                predicates.add(cb.equal(root.get("isRead"), filter.getIsRead()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
