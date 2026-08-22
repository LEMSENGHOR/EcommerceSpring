package com.ecommerce.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GatewayChargeResult {
    private final boolean success;
    private final String transactionId;
    private final String failureReason;

    public static GatewayChargeResult success(String transactionId) {
        return GatewayChargeResult.builder().success(true).transactionId(transactionId).build();
    }

    public static GatewayChargeResult failure(String reason) {
        return GatewayChargeResult.builder().success(false).failureReason(reason).build();
    }
}
