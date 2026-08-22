package com.ecommerce.repository;

import com.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /** Used to enforce that a cart item can only be modified via its owning cart. */
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    /**
     * Used by ProductServiceImpl.deleteProduct to clean up any dangling
     * references in active carts before a hard delete — a cart isn't a
     * historical record the way an Order is, so silently removing a
     * discontinued product from someone's cart is fine (unlike order_items,
     * which block deletion entirely; see existsByProductId in OrderItemRepository).
     */
    long deleteByProductId(Long productId);
}
