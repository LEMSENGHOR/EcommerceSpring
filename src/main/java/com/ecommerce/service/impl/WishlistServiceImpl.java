package com.ecommerce.service.impl;

import com.ecommerce.dto.cart.CartItemRequest;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.wishlist.WishlistItemResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Wishlist;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.WishlistMapper;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.WishlistRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.CartService;
import com.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist() {
        return WishlistMapper.toResponseList(
                wishlistRepository.findByUserId(SecurityUtils.getCurrentUserId()));
    }

    @Override
    public WishlistItemResponse addToWishlist(Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("Product " + productId + " is already in your wishlist");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        return WishlistMapper.toResponse(wishlistRepository.save(wishlist));
    }

    @Override
    public void removeFromWishlist(Long productId) {
        Wishlist wishlist = findOwnedEntryOrThrow(productId);
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(SecurityUtils.getCurrentUserId(), productId);
    }

    @Override
    public CartResponse moveToCart(Long productId, Integer quantity) {
        Wishlist wishlist = findOwnedEntryOrThrow(productId);

        // Delegate to CartService for the actual add — reuses its active-product
        // check and quantity-merge behavior rather than duplicating that logic here.
        CartResponse cartResponse = cartService.addItem(
                CartItemRequest.builder()
                        .productId(productId)
                        .quantity(quantity != null ? quantity : 1)
                        .build());

        wishlistRepository.delete(wishlist);
        return cartResponse;
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Wishlist findOwnedEntryOrThrow(Long productId) {
        return wishlistRepository.findByUserIdAndProductId(SecurityUtils.getCurrentUserId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not in your wishlist"));
    }
}
