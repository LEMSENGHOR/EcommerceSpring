package com.ecommerce.dto.wishlist;

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
public class WishlistItemResponse {
    private Long id;

    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private String productStatus;

    /** True if stock is currently 0 — useful for the frontend to grey out "add to cart". */
    private Boolean outOfStock;

    private LocalDateTime createdAt;
}
