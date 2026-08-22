package com.ecommerce.controller;

import com.ecommerce.dto.review.CreateReviewRequest;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.dto.review.UpdateReviewRequest;
import com.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Authoring endpoints — every method acts on the authenticated caller's own
 * reviews. Public browsing of a product's reviews lives in
 * ProductReviewController instead, since that's unauthenticated and
 * naturally nested under /api/products/{id}/reviews.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
@Tag(name = "Review", description = "Authoring the caller's own reviews. Public browsing lives under /api/products/{id}/reviews.")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Create a review",
            description = "Requires a DELIVERED order containing this product — a cancelled or "
                    + "still-in-transit order doesn't qualify (400). One review per product per user (409 "
                    + "if you've already reviewed it).")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getMyReviews());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @PathVariable Long id, @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateMyReview(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyReview(@PathVariable Long id) {
        reviewService.deleteMyReview(id);
        return ResponseEntity.noContent().build();
    }
}
