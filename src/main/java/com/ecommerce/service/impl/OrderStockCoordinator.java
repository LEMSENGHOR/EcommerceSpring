package com.ecommerce.service.impl;

import com.ecommerce.entity.OrderItem;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Centralizes the atomic stock decrement/restore logic so OrderServiceImpl
 * (checkout, self-service cancel) and AdminOrderServiceImpl (admin-initiated
 * cancel/refund) don't each reimplement it.
 */
@Service
@RequiredArgsConstructor
class OrderStockCoordinator {

    private final ProductRepository productRepository;

    /**
     * Decrements stock for every item, failing fast on the first item that
     * doesn't have enough. Does NOT roll back earlier decrements itself —
     * callers must invoke this within a @Transactional method (both
     * OrderServiceImpl.placeOrder and any future caller already are), so a
     * thrown exception here rolls back the whole transaction, including any
     * decrements already applied in this same loop.
     */
    void decrementForItems(Iterable<OrderItem> items) {
        for (OrderItem item : items) {
            int updated = productRepository.decrementStock(item.getProduct().getId(), item.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + item.getProduct().getName() + "'");
            }
        }
    }

    /** Restores stock for every item — used on cancellation/refund. */
    void restoreForItems(Set<OrderItem> items) {
        for (OrderItem item : items) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
        }
    }
}
