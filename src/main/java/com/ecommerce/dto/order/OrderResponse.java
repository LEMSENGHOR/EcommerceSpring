package com.ecommerce.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String status;

    private List<OrderItemResponse> items;

    /** Sum of item subtotals, before any coupon discount. */
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String couponCode;

    /** subtotal - discountAmount. What the customer is actually charged. */
    private BigDecimal totalAmount;

    private Long shippingAddressId;
    private String shippingAddressSummary;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
