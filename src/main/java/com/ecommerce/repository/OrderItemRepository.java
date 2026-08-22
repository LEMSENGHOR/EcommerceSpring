package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Used by ProductServiceImpl.deleteProduct to block deletion cleanly (409)
     * if the product has ever been ordered — order history must never
     * silently lose a line item to a product hard-delete.
     */
    boolean existsByProductId(Long productId);

    /**
     * "Verified purchase" check for Review (Phase 13) — true only if the user
     * has a DELIVERED order containing this product. Deliberately stricter
     * than "ever ordered it" (a cancelled or still-in-transit order doesn't
     * count) — see README for why DELIVERED specifically was chosen.
     */
    @Query("""
            SELECT COUNT(oi) > 0 FROM OrderItem oi
            WHERE oi.order.user.id = :userId
              AND oi.product.id = :productId
              AND oi.order.status = com.ecommerce.entity.enums.OrderStatus.DELIVERED
            """)
    boolean existsDeliveredPurchase(@Param("userId") Long userId, @Param("productId") Long productId);
}
