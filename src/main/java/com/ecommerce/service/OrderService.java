package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.dto.order.PlaceOrderRequest;
import org.springframework.data.domain.Pageable;

/**
 * Self-service order operations — every method acts on the currently
 * authenticated user (resolved via SecurityUtils in the impl), never on an
 * arbitrary id. Admin-on-any-order operations live in AdminOrderService.
 */
public interface OrderService {

    /**
     * Snapshots the caller's current cart into a new Order, atomically
     * decrementing stock per item, and clears the cart on success.
     * Throws if the cart is empty, the address doesn't belong to the caller,
     * any cart item is out of stock, or the coupon (if provided) is invalid.
     */
    OrderResponse placeOrder(PlaceOrderRequest request);

    PagedResponse<OrderSummaryResponse> getMyOrders(Pageable pageable);

    OrderResponse getMyOrderById(Long orderId);

    /** Only allowed while the order is PENDING or CONFIRMED; restores stock. */
    OrderResponse cancelMyOrder(Long orderId);
}
