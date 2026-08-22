package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.payment.AdminPaymentFilterRequest;
import com.ecommerce.dto.payment.PaymentResponse;
import com.ecommerce.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only by design — already restricted to ROLE_ADMIN by SecurityConfig.
 * No refund endpoint here on purpose; see AdminPaymentService for why.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments")
@Tag(name = "Payment (Admin)", description = "Read-only payment visibility/reporting. Refunds happen via Order status transitions, not here.")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    /**
     * Examples:
     *   GET /api/admin/payments?status=FAILED
     *   GET /api/admin/payments?paymentMethod=CARD
     *   GET /api/admin/payments?orderId=42
     */
    @GetMapping
    public ResponseEntity<PagedResponse<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long orderId,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminPaymentFilterRequest filter = AdminPaymentFilterRequest.builder()
                .status(status)
                .paymentMethod(paymentMethod)
                .orderId(orderId)
                .build();

        return ResponseEntity.ok(adminPaymentService.getAllPayments(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(adminPaymentService.getPaymentById(id));
    }
}
