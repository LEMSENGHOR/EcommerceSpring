package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.dto.order.PlaceOrderRequest;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.CouponUsage;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.NotificationType;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CouponUsageRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.NotificationService;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CouponValidator couponValidator;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderStockCoordinator stockCoordinator;
    private final PaymentRefundCoordinator paymentRefundCoordinator;
    private final NotificationService notificationService;

    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new InvalidRequestException("Cannot place an order from an empty cart"));

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + request.getAddressId()));

        // Snapshot cart items into order items BEFORE touching stock, so price
        // is fixed at the moment of checkout regardless of later catalog changes
        // (unlike the cart, which always shows live pricing — see Phase 8 README).
        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            BigDecimal price = cartItem.getProduct().getPrice();
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            orderItems.add(OrderItem.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(price)
                    .subtotal(itemSubtotal)
                    .build());
        }

        Coupon coupon = null;
        BigDecimal totalAmount = subtotal;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponValidator.validate(request.getCouponCode(), userId, subtotal);
            BigDecimal discount = couponValidator.calculateDiscount(subtotal, coupon);
            totalAmount = subtotal.subtract(discount);
        }

        Order order = Order.builder()
                .user(user)
                .orderNumber(generateUniqueOrderNumber())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .shippingAddress(address)
                .coupon(coupon)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        // Atomic per-item decrement — throws InsufficientStockException (409) on
        // the first item that doesn't have enough, rolling back this whole
        // transaction (no order is persisted, no stock is left partially decremented).
        stockCoordinator.decrementForItems(orderItems);

        Order saved = orderRepository.save(order);

        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponUsageRepository.save(CouponUsage.builder()
                    .coupon(coupon)
                    .user(user)
                    .order(saved)
                    .build());
        }

        // Clear the cart only after everything above succeeded.
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        // REQUIRES_NEW + internally caught (see NotificationServiceImpl) — a
        // notification failure here can never roll back or block this order.
        notificationService.notify(userId, "Order placed",
                "Your order " + saved.getOrderNumber() + " has been placed successfully.",
                NotificationType.ORDER);

        return OrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getMyOrders(Pageable pageable) {
        // Phase 10 keeps this a simple findByUserId + in-memory page rather than
        // adding a paged repository method — fine at current scale; if per-user
        // order history grows large, switch to a Pageable-aware query instead.
        var orders = orderRepository.findByUserId(SecurityUtils.getCurrentUserId());
        var summaries = OrderMapper.toSummaryList(orders);

        int start = Math.min((int) pageable.getOffset(), summaries.size());
        int end = Math.min(start + pageable.getPageSize(), summaries.size());
        var pageContent = summaries.subList(start, end);

        return PagedResponse.<OrderSummaryResponse>builder()
                .content(pageContent)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(summaries.size())
                .totalPages((int) Math.ceil((double) summaries.size() / pageable.getPageSize()))
                .last(end >= summaries.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long orderId) {
        return OrderMapper.toResponse(findOwnedOrderOrThrow(orderId));
    }

    @Override
    public OrderResponse cancelMyOrder(Long orderId) {
        Order order = findOwnedOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidRequestException(
                    "Order cannot be cancelled once it has been " + order.getStatus().name().toLowerCase());
        }

        stockCoordinator.restoreForItems(order.getItems());
        paymentRefundCoordinator.refundIfPaid(order);
        order.setStatus(OrderStatus.CANCELLED);

        notificationService.notify(order.getUser().getId(), "Order cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled.", NotificationType.ORDER);

        return OrderMapper.toResponse(order);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Order findOwnedOrderOrThrow(Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private String generateUniqueOrderNumber() {
        String candidate;
        do {
            String datePart = LocalDateTime.now().format(ORDER_NUMBER_DATE_FORMAT);
            String randomPart = String.format("%06d", RANDOM.nextInt(1_000_000));
            candidate = "ORD-" + datePart + "-" + randomPart;
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }
}
