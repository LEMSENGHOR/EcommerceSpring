package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.AdminOrderFilterRequest;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.enums.NotificationType;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.specification.OrderSpecification;
import com.ecommerce.service.AdminOrderService;
import com.ecommerce.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderServiceImpl implements AdminOrderService {

    /**
     * Fixed order-status state machine. Any transition not listed here is
     * rejected with a 400, regardless of current status — including "no-op"
     * transitions (PENDING -> PENDING) and skipping stages (PENDING -> SHIPPED).
     * CANCELLED and REFUNDED are terminal; DELIVERED can only move to REFUNDED.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUNDED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final OrderStockCoordinator stockCoordinator;
    private final PaymentRefundCoordinator paymentRefundCoordinator;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(AdminOrderFilterRequest filter, Pageable pageable) {
        Page<Order> page = orderRepository.findAll(OrderSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(OrderMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return OrderMapper.toResponse(findOrderOrThrow(orderId));
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(orderId);
        OrderStatus newStatus = parseStatusOrThrow(request.getStatus());
        OrderStatus currentStatus = order.getStatus();

        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidRequestException(
                    "Cannot transition order from " + currentStatus + " to " + newStatus);
        }

        // Stock is restored on any transition INTO CANCELLED or REFUNDED, whichever
        // path got there — mirrors the restore already done by the user-initiated
        // cancelMyOrder() in OrderServiceImpl, but this covers admin-initiated
        // cancellation and post-delivery refunds too. Payment (Phase 11) is kept
        // in sync the same way — a SUCCESS payment is refunded via the gateway
        // and marked REFUNDED; a no-op if the order was never actually paid.
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED) {
            stockCoordinator.restoreForItems(order.getItems());
            paymentRefundCoordinator.refundIfPaid(order);
        }

        order.setStatus(newStatus);

        notificationService.notify(order.getUser().getId(),
                "Order " + newStatus.name().toLowerCase(),
                "Your order " + order.getOrderNumber() + " is now " + newStatus.name().toLowerCase() + ".",
                NotificationType.ORDER);

        return OrderMapper.toResponse(order);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderStatus parseStatusOrThrow(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown order status: " + status);
        }
    }
}
