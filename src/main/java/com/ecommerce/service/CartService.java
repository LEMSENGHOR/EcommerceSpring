package com.ecommerce.service;

import com.ecommerce.dto.cart.CartItemRequest;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.cart.UpdateCartItemRequest;

/**
 * All methods act on the currently authenticated user's cart (resolved via
 * SecurityUtils in the impl) — there is no userId parameter anywhere here.
 * The cart is created lazily: it does not exist until the user's first
 * addItem() call (see README "lazy vs eager" decision for Phase 8).
 */
public interface CartService {

    CartResponse getMyCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItem(Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long itemId);

    CartResponse clearCart();
}
