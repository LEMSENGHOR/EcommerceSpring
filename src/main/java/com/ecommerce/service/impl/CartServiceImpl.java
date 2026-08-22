package com.ecommerce.service.impl;

import com.ecommerce.dto.cart.CartItemRequest;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.cart.UpdateCartItemRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.EntityStatus;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        // Read-only path: don't create a row just for viewing an empty cart.
        // Return an empty, unsaved Cart shell so the response shape is always
        // consistent (id will be null until the cart is actually persisted).
        Cart cart = cartRepository.findByUserId(userId).orElseGet(Cart::new);
        return CartMapper.toResponse(cart);
    }

    @Override
    public CartResponse addItem(CartItemRequest request) {
        Product product = findActiveProductOrThrow(request.getProductId());
        Cart cart = findOrCreateCart();

        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> {
                            CartItem item = CartItem.builder()
                                    .cart(cart)
                                    .product(product)
                                    .quantity(request.getQuantity())
                                    .build();
                            cartItemRepository.save(item);
                            cart.getItems().add(item);
                        }
                );

        return CartMapper.toResponse(cart);
    }

    @Override
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        CartItem item = findOwnedItemOrThrow(itemId);
        item.setQuantity(request.getQuantity());
        return CartMapper.toResponse(item.getCart());
    }

    @Override
    public CartResponse removeItem(Long itemId) {
        CartItem item = findOwnedItemOrThrow(itemId);
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return CartMapper.toResponse(cart);
    }

    @Override
    public CartResponse clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId).orElseGet(Cart::new);

        if (cart.getId() != null) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
        return CartMapper.toResponse(cart);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    /**
     * The cart is created lazily on first write (addItem), not at registration —
     * see the Phase 7 README note this decision was deferred from. getMyCart()
     * and clearCart() deliberately do NOT call this, so simply viewing or
     * clearing an empty cart never creates a row.
     */
    private Cart findOrCreateCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            return cartRepository.save(Cart.builder().user(user).build());
        });
    }

    private Product findActiveProductOrThrow(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getStatus() != EntityStatus.ACTIVE) {
            throw new InvalidRequestException("Product '" + product.getName() + "' is not currently available");
        }
        // Deliberately NOT blocking on stock here — see README: the cart allows
        // over-committing and surfaces it via CartItemResponse.insufficientStock,
        // with the hard stock check happening at checkout (Phase 10/11) instead.
        return product;
    }

    private CartItem findOwnedItemOrThrow(Long itemId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
        return cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
    }
}
