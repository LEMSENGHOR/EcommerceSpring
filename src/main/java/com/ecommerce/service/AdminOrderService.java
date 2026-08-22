package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.AdminOrderFilterRequest;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.UpdateOrderStatusRequest;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    PagedResponse<OrderResponse> getAllOrders(AdminOrderFilterRequest filter, Pageable pageable);

    OrderResponse getOrderById(Long orderId);

    /**
     * Validates the transition against the fixed order-status state machine
     * (see README) before applying it. Transitioning to CANCELLED restores stock.
     */
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
}
