package com.ecommerce.mapper;

import com.ecommerce.dto.cart.CartItemResponse;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class CartMapper {

    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() != null
                ? cart.getItems().stream()
                        .map(CartMapper::toResponse)
                        .sorted(Comparator.comparing(CartItemResponse::getId))
                        .toList()
                : List.of();

        int totalItems = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public static CartItemResponse toResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal unitPrice = product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(primaryImageUrl(product))
                .productStatus(product.getStatus() != null ? product.getStatus().name() : null)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .insufficientStock(item.getQuantity() > product.getStock())
                .build();
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
