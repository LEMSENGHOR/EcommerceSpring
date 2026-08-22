package com.ecommerce.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Generates `count` distinct, randomly-suffixed coupon codes that all share
 * the same discount rules — e.g. a "SUMMER-XXXXXX" batch for an email campaign,
 * where each recipient gets a unique single-use code rather than everyone
 * sharing one code (which maxUsage alone can't express per-recipient limits for).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkGenerateCouponRequest {

    @NotNull(message = "count is required")
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 500, message = "count cannot exceed 500 per request")
    private Integer count;

    /** Prepended to each generated code, e.g. "SUMMER" -> "SUMMER-A1B2C3D4". Optional. */
    @Size(max = 20, message = "codePrefix must be at most 20 characters")
    private String codePrefix;

    @NotNull(message = "discountType is required")
    private String discountType;

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "discountValue must be greater than 0")
    private BigDecimal discountValue;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "minimumAmount cannot be negative")
    private BigDecimal minimumAmount = BigDecimal.ZERO;

    /** Applies per generated code (e.g. 1 = single-use), not across the whole batch. */
    @Min(value = 1, message = "maxUsage must be at least 1 if provided")
    private Integer maxUsage;

    @NotNull(message = "startDate is required")
    private LocalDateTime startDate;

    @NotNull(message = "endDate is required")
    private LocalDateTime endDate;
}
