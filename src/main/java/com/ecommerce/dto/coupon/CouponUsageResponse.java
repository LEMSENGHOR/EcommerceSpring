package com.ecommerce.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private Long orderId;
    private String orderNumber;
    private LocalDateTime usedAt;
}
