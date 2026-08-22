package com.ecommerce.mapper;

import com.ecommerce.dto.order.OrderItemResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream()
                        .map(OrderMapper::toResponse)
                        .sorted(Comparator.comparing(OrderItemResponse::getId))
                        .toList()
                : List.of();

        BigDecimal subtotal = itemResponses.stream()
                .map(OrderItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = subtotal.subtract(order.getTotalAmount()).max(BigDecimal.ZERO);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .items(itemResponses)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .totalAmount(order.getTotalAmount())
                .shippingAddressId(order.getShippingAddress() != null ? order.getShippingAddress().getId() : null)
                .shippingAddressSummary(summarize(order.getShippingAddress()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public static OrderItemResponse toResponse(OrderItem item) {
        Product product = item.getProduct();
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? primaryImageUrl(product) : null)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    public static OrderSummaryResponse toSummary(Order order) {
        int itemCount = order.getItems() != null
                ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum()
                : 0;

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .itemCount(itemCount)
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public static List<OrderResponse> toResponseList(List<Order> orders) {
        return orders.stream().map(OrderMapper::toResponse).toList();
    }

    public static List<OrderSummaryResponse> toSummaryList(List<Order> orders) {
        return orders.stream().map(OrderMapper::toSummary).toList();
    }

    private static String summarize(Address address) {
        if (address == null) {
            return null;
        }
        return String.join(", ",
                nullSafe(address.getStreet()),
                nullSafe(address.getCity()),
                nullSafe(address.getState()),
                nullSafe(address.getCountry())
        ).replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
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
