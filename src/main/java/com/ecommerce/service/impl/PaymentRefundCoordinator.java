package com.ecommerce.service.impl;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.enums.PaymentStatus;
import com.ecommerce.gateway.PaymentGatewayService;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Keeps Payment state in sync whenever an Order transitions to CANCELLED or
 * REFUNDED. Payment didn't exist yet when Phase 10 built order cancellation,
 * so that flow only restored stock — this closes the gap: cancelling or
 * refunding an order that was already paid now also refunds the payment via
 * the gateway and marks it REFUNDED. A no-op (not an error) if the order was
 * never successfully paid — e.g. cancelling a still-PENDING, unpaid order.
 *
 * Injected into both OrderServiceImpl (self-service cancel) and
 * AdminOrderServiceImpl (admin cancel/refund), same pattern as
 * OrderStockCoordinator.
 */
@Service
@RequiredArgsConstructor
class PaymentRefundCoordinator {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;

    void refundIfPaid(Order order) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                return; // nothing to refund — never paid, or already refunded/failed
            }

            // Simulated gateway always succeeds; a real adapter could return false
            // for a genuine refund decline. Either way we record REFUNDED here —
            // see README for why a failed *refund* attempt isn't modeled as its
            // own status in this phase.
            paymentGatewayService.refund(
                    payment.getTransactionId(), payment.getAmount(), payment.getPaymentMethod());
            markRefunded(payment);
        });
    }

    private void markRefunded(Payment payment) {
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason(null);
    }
}
