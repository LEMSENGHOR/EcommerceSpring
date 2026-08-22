package com.ecommerce.controller;

import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.wishlist.WishlistItemResponse;
import com.ecommerce.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Every endpoint acts on the authenticated caller's own wishlist (resolved
 * from the JWT via SecurityUtils) — no userId path variable. Keyed by
 * productId rather than a wishlist-entry id, since from the frontend's
 * perspective this is a toggle on a product ("heart" icon), not a
 * separately-managed resource.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "The authenticated caller's own wishlist, keyed by productId.")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getMyWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistItemResponse> addToWishlist(@PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.addToWishlist(productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/exists")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(Map.of("inWishlist", wishlistService.isInWishlist(productId)));
    }

    @Operation(summary = "Move a wishlist item into the cart",
            description = "Adds the product to the cart (merging quantity if already present, same as "
                    + "POST /api/cart/items) and removes it from the wishlist — atomically: if the "
                    + "product turns out to be inactive, the add fails and the wishlist entry is left "
                    + "untouched rather than being silently removed.")
    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<CartResponse> moveToCart(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(wishlistService.moveToCart(productId, quantity));
    }
}
