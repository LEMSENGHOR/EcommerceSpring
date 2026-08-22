package com.ecommerce.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;

    private Long orderId;
    private String orderNumber;

    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;

    /** Present only when status is FAILED — the simulated/gateway decline reason. */
    private String failureReason;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
