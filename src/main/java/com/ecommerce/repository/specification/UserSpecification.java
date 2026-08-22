package com.ecommerce.repository.specification;

import com.ecommerce.dto.admin.AdminUserFilterRequest;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.EntityStatus;
import com.ecommerce.entity.enums.RoleName;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> withFilters(AdminUserFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), EntityStatus.valueOf(filter.getStatus().toUpperCase())));
            }

            if (filter.getRole() != null && !filter.getRole().isBlank()) {
                // distinct avoids duplicate rows from the roles join when a user has multiple roles
                query.distinct(true);
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin.get("name"), RoleName.valueOf(filter.getRole().toUpperCase())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
