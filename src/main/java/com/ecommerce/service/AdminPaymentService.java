package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.payment.AdminPaymentFilterRequest;
import com.ecommerce.dto.payment.PaymentResponse;
import org.springframework.data.domain.Pageable;

/**
 * Deliberately read-only — there is no admin "refund" action here. Refunds
 * happen exclusively as a side effect of an Order status transition to
 * CANCELLED/REFUNDED (see AdminOrderService + PaymentRefundCoordinator), so
 * Payment state can never drift out of sync with Order state by being
 * editable independently. This service exists purely for admin visibility
 * and reporting (e.g. "show me all FAILED payments this week").
 */
public interface AdminPaymentService {

    PagedResponse<PaymentResponse> getAllPayments(AdminPaymentFilterRequest filter, Pageable pageable);

    PaymentResponse getPaymentById(Long id);
}
