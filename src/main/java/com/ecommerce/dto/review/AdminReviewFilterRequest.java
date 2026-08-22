package com.ecommerce.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bound from query params on GET /api/admin/reviews. All fields optional. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewFilterRequest {
    private Long productId;
    private Long userId;
    private Integer minRating;
    private Integer maxRating;
}
