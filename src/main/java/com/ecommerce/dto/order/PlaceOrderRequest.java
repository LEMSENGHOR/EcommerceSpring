package com.ecommerce.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    @NotNull(message = "addressId is required")
    private Long addressId;

    /**
     * Optional. Coupon *management* (create/list/deactivate codes) is Phase 12 —
     * this field only wires up *redeeming* an existing, already-active code at
     * checkout, since the coupon_id FK on orders and the coupon_usages table
     * already exist from Phase 2. See README for what's deliberately deferred.
     */
    @Size(max = 50, message = "Coupon code must be at most 50 characters")
    private String couponCode;
}
