package com.ecommerce.service;

import com.ecommerce.dto.payment.PaymentResponse;
import com.ecommerce.dto.payment.ProcessPaymentRequest;

/**
 * Self-service payment operations — every method acts on an order owned by
 * the currently authenticated user (resolved via SecurityUtils in the impl).
 * Admin-on-any-payment operations (list all, refund) live in AdminPaymentService.
 */
public interface PaymentService {

    /**
     * Charges the order's total via the given method. Only allowed while the
     * order is PENDING and has no existing SUCCESS payment. On success, the
     * order transitions PENDING -> CONFIRMED. A gateway decline is NOT an
     * exception — it's a normal response with status FAILED; the order stays
     * PENDING so the customer can retry with a different method.
     */
    PaymentResponse processPayment(Long orderId, ProcessPaymentRequest request);

    PaymentResponse getMyPaymentForOrder(Long orderId);
}
