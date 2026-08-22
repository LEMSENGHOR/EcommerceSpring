package com.ecommerce.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code must be at most 50 characters")
    private String code;

    @NotNull(message = "discountType is required")
    private String discountType;

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "discountValue must be greater than 0")
    private BigDecimal discountValue;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "minimumAmount cannot be negative")
    private BigDecimal minimumAmount = BigDecimal.ZERO;

    /** Null = unlimited total uses across all users. */
    @Min(value = 1, message = "maxUsage must be at least 1 if provided")
    private Integer maxUsage;

    @NotNull(message = "startDate is required")
    private LocalDateTime startDate;

    @NotNull(message = "endDate is required")
    private LocalDateTime endDate;
}
