package com.ecommerce.service.impl;

import com.ecommerce.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.dto.payment.PaymentResponse;
import com.ecommerce.dto.payment.ProcessPaymentRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.enums.NotificationType;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.entity.enums.PaymentMethod;
import com.ecommerce.entity.enums.PaymentStatus;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.gateway.GatewayChargeResult;
import com.ecommerce.gateway.PaymentGatewayService;
import com.ecommerce.mapper.PaymentMapper;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.AdminOrderService;
import com.ecommerce.service.NotificationService;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final AdminOrderService adminOrderService;
    private final NotificationService notificationService;

    @Override
    public PaymentResponse processPayment(Long orderId, ProcessPaymentRequest request) {
        Order order = findOwnedOrderOrThrow(orderId);
        PaymentMethod method = parseMethodOrThrow(request.getPaymentMethod());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidRequestException(
                    "Order is " + order.getStatus() + " — payment can only be taken for a PENDING order");
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new DuplicateResourceException("Order " + orderId + " has already been paid");
        }

        GatewayChargeResult result = paymentGatewayService.charge(
                order.getTotalAmount(), method, Boolean.TRUE.equals(request.getSimulateFailure()));

        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .paymentMethod(method)
                    .build();
        } else {
            // Retrying after a prior FAILED attempt — same row, since order_id is
            // unique on payments (one payment record per order, not per attempt).
            payment.setPaymentMethod(method);
        }

        if (result.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(result.getTransactionId());
            payment.setFailureReason(null);
            payment.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.getFailureReason());
            payment.setPaidAt(null);
        }

        Payment saved = paymentRepository.save(payment);

        if (result.isSuccess()) {
            // Reuses AdminOrderService's transition validation rather than setting
            // status directly here, so PENDING -> CONFIRMED is governed by the same
            // single state-machine definition regardless of whether an admin or a
            // successful payment triggered it. See AdminOrderServiceImpl.ALLOWED_TRANSITIONS.
            adminOrderService.updateOrderStatus(orderId,
                    UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED.name()).build());
            // Note: that call already sends an "Order confirmed" (ORDER-type)
            // notification via AdminOrderServiceImpl — the one below is
            // deliberately a separate, PAYMENT-type notification about the
            // charge itself, not a duplicate of the order-status one.
            notificationService.notify(order.getUser().getId(), "Payment successful",
                    "Your payment of " + saved.getAmount() + " for order " + order.getOrderNumber()
                            + " was successful.",
                    NotificationType.PAYMENT);
        } else {
            // On failure, the order deliberately stays PENDING — no transition —
            // so the customer can retry processPayment with a different method.
            notificationService.notify(order.getUser().getId(), "Payment failed",
                    "Your payment for order " + order.getOrderNumber() + " could not be processed"
                            + (saved.getFailureReason() != null ? " (" + saved.getFailureReason() + ")" : "") + ".",
                    NotificationType.PAYMENT);
        }

        return PaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getMyPaymentForOrder(Long orderId) {
        findOwnedOrderOrThrow(orderId); // ownership check
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order " + orderId));
        return PaymentMapper.toResponse(payment);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Order findOwnedOrderOrThrow(Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private PaymentMethod parseMethodOrThrow(String method) {
        try {
            return PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown payment method: " + method);
        }
    }
}
