package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserId(Long userId);
    boolean existsByOrderNumber(String orderNumber);

    /** Used to enforce that a user can only view/cancel their own orders. */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** Used by AdminUserServiceImpl.deleteUser to block deletion cleanly (409) if the user has order history. */
    boolean existsByUserId(Long userId);
}
