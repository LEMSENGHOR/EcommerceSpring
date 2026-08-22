package com.ecommerce.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingSummaryResponse {
    private Long productId;

    /** Rounded to 1 decimal place. 0 if the product has no reviews yet. */
    private Double averageRating;

    private Long reviewCount;

    /** Keys 1-5, always all present (0 for ratings with no reviews) — ready for a stars histogram. */
    private Map<Integer, Long> ratingBreakdown;
}
