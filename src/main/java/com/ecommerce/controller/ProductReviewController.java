package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.ProductRatingSummaryResponse;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public — no authentication required. Nested under /api/products/** so it
 * falls under the same permitAll-for-GET rule already covering the product
 * catalog in SecurityConfig; no security changes needed for this controller.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/reviews")
@Tag(name = "Review", description = "Public browsing of a product's reviews and rating summary.")
@SecurityRequirements
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<PagedResponse<ReviewResponse>> getProductReviews(
            @PathVariable Long productId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<ProductRatingSummaryResponse> getProductRatingSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductRatingSummary(productId));
    }
}
