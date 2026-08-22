package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.dto.order.PlaceOrderRequest;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Every endpoint acts on the authenticated caller's own orders (resolved
 * from the JWT via SecurityUtils) — no userId path variable. Viewing or
 * managing another user's orders is exclusively AdminOrderController's job.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Checkout and order history for the authenticated caller.")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place an order from the current cart",
            description = "Snapshots the cart's items and prices into the order (the order is unaffected "
                    + "by later catalog price changes), atomically decrements stock per item (409 if any "
                    + "item is unavailable), and clears the cart on success. Requires an existing addressId "
                    + "from the caller's own address book. An optional couponCode is validated and redeemed "
                    + "in the same transaction. The order starts PENDING with no payment yet — see the "
                    + "Payment endpoints to actually charge it.")
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrderById(id));
    }

    @Operation(summary = "Cancel an order",
            description = "Only allowed while the order is PENDING or CONFIRMED. Restores stock for every "
                    + "item, and if the order was already paid, refunds the payment via the gateway too.")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelMyOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelMyOrder(id));
    }
}
