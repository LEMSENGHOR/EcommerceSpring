package com.ecommerce.service;

import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.wishlist.WishlistItemResponse;

import java.util.List;

/**
 * All methods act on the currently authenticated user's wishlist (resolved
 * via SecurityUtils in the impl) — no userId parameter anywhere here, same
 * pattern as CartService.
 */
public interface WishlistService {

    List<WishlistItemResponse> getMyWishlist();

    WishlistItemResponse addToWishlist(Long productId);

    void removeFromWishlist(Long productId);

    boolean isInWishlist(Long productId);

    /** Removes the product from the wishlist and adds it to the cart, in one call. */
    CartResponse moveToCart(Long productId, Integer quantity);
}
