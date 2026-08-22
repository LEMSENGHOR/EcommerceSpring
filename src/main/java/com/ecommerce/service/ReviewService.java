package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.CreateReviewRequest;
import com.ecommerce.dto.review.ProductRatingSummaryResponse;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.dto.review.UpdateReviewRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {

    /**
     * Throws InvalidRequestException unless the caller has a DELIVERED order
     * containing this product (see OrderItemRepository.existsDeliveredPurchase),
     * and DuplicateResourceException if they've already reviewed it — the
     * one-review-per-product-per-user constraint from Phase 2.
     */
    ReviewResponse createReview(CreateReviewRequest request);

    ReviewResponse updateMyReview(Long reviewId, UpdateReviewRequest request);

    void deleteMyReview(Long reviewId);

    List<ReviewResponse> getMyReviews();

    // ---------------------------------------------------------
    // Public (no ownership check — these back the public product page)
    // ---------------------------------------------------------

    PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    ProductRatingSummaryResponse getProductRatingSummary(Long productId);
}
