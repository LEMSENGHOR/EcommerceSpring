package com.ecommerce.mapper;

import com.ecommerce.dto.product.ProductImageResponse;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;

import java.util.List;

public class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .subCategoryId(product.getSubCategory() != null ? product.getSubCategory().getId() : null)
                .subCategoryName(product.getSubCategory() != null ? product.getSubCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .sku(product.getSku())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .images(product.getImages() != null
                        ? toImageResponseList(product.getImages().stream().toList())
                        : List.of())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static ProductImageResponse toResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    public static List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    public static List<ProductImageResponse> toImageResponseList(List<ProductImage> images) {
        return images.stream().map(ProductMapper::toResponse).toList();
    }
}
