package com.ecommerce.mapper;

import com.ecommerce.dto.wishlist.WishlistItemResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.Wishlist;

import java.util.Comparator;
import java.util.List;

public class WishlistMapper {

    private WishlistMapper() {
    }

    public static WishlistItemResponse toResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();

        return WishlistItemResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(primaryImageUrl(product))
                .productPrice(product.getPrice())
                .productStatus(product.getStatus() != null ? product.getStatus().name() : null)
                .outOfStock(product.getStock() == null || product.getStock() <= 0)
                .createdAt(wishlist.getCreatedAt())
                .build();
    }

    public static List<WishlistItemResponse> toResponseList(List<Wishlist> wishlists) {
        return wishlists.stream()
                .map(WishlistMapper::toResponse)
                .sorted(Comparator.comparing(WishlistItemResponse::getCreatedAt).reversed())
                .toList();
    }

    private static String primaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}
