package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.AdminOrderFilterRequest;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Everything here is already restricted to ROLE_ADMIN by SecurityConfig. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
@Tag(name = "Order (Admin)", description = "Order oversight and status transitions. ADMIN only.")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * Examples:
     *   GET /api/admin/orders?status=PENDING
     *   GET /api/admin/orders?userId=42
     *   GET /api/admin/orders?orderNumber=ORD-20260820
     */
    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminOrderFilterRequest filter = AdminOrderFilterRequest.builder()
                .status(status)
                .userId(userId)
                .orderNumber(orderNumber)
                .build();

        return ResponseEntity.ok(adminOrderService.getAllOrders(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getOrderById(id));
    }

    @Operation(summary = "Transition an order's status",
            description = "Only these transitions are allowed, validated against a fixed state machine "
                    + "(400 for anything else, including skipping a stage or a no-op): "
                    + "PENDING -> CONFIRMED or CANCELLED; CONFIRMED -> SHIPPED or CANCELLED; "
                    + "SHIPPED -> DELIVERED; DELIVERED -> REFUNDED. CANCELLED and REFUNDED are terminal. "
                    + "Transitioning to CANCELLED or REFUNDED restores stock for every item and, if the "
                    + "order was paid, refunds the payment via the gateway.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(id, request));
    }
}
