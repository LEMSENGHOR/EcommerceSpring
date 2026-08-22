package com.ecommerce.controller;

import com.ecommerce.dto.payment.PaymentResponse;
import com.ecommerce.dto.payment.ProcessPaymentRequest;
import com.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Every endpoint acts on an order owned by the authenticated caller
 * (ownership enforced in PaymentServiceImpl via SecurityUtils + findByIdAndUserId).
 * Nested under /api/orders/{orderId}/payment since a payment doesn't exist
 * independently of its order — there's no "list all my payments" endpoint
 * here on purpose; order history (Phase 10) already covers that view.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/payment")
@Tag(name = "Payment", description = "Charge and view payment for one of the caller's own orders.")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Charge the order's total",
            description = "On success, drives the order PENDING -> CONFIRMED automatically. A gateway "
                    + "decline is a normal FAILED response, not an error — the order stays PENDING so the "
                    + "customer can retry with a different method. The simulateFailure flag on the request "
                    + "is a dev-only escape hatch for the simulated gateway and must be dropped once a real "
                    + "payment provider replaces it.")
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long orderId, @Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(orderId, request));
    }

    @GetMapping
    public ResponseEntity<PaymentResponse> getMyPaymentForOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getMyPaymentForOrder(orderId));
    }
}
