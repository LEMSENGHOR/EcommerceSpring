package com.ecommerce.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;

    private Long productId;
    private String productName;
    private String productImageUrl;
    private String productStatus;

    /** Current unit price from the product catalog (not a snapshot — see README). */
    private BigDecimal unitPrice;

    private Integer quantity;
    private BigDecimal subtotal;

    /** True if requested quantity exceeds current available stock. */
    private Boolean insufficientStock;
}
