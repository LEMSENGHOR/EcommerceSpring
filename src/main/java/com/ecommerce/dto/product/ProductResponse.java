package com.ecommerce.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;

    private Long categoryId;
    private String categoryName;

    private Long subCategoryId;
    private String subCategoryName;

    private Long brandId;
    private String brandName;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private String status;

    private List<ProductImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
