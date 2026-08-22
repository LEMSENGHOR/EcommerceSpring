package com.ecommerce.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lightweight row for order list views — avoids serializing every item's detail. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponse {
    private Long id;
    private String orderNumber;
    private String status;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
