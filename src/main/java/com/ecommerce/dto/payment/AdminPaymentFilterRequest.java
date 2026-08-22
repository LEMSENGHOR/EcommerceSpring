package com.ecommerce.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bound from query params on GET /api/admin/payments. All fields optional. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPaymentFilterRequest {
    private String status;
    private String paymentMethod;
    private Long orderId;
}
