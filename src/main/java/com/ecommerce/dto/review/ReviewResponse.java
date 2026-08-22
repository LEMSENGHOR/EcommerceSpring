package com.ecommerce.dto.review;

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
public class ReviewResponse {
    private Long id;

    private Long productId;
    private String productName;

    private Long userId;
    private String userName;

    private Integer rating;
    private String comment;

    /** Always true under the current create-time gate — see README. Exposed for
     *  frontend badge display and to future-proof against that gate loosening. */
    private Boolean verifiedPurchase;

    private LocalDateTime createdAt;
}
