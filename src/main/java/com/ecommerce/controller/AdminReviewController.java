package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.AdminReviewFilterRequest;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.service.AdminReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Already restricted to ROLE_ADMIN by SecurityConfig. Moderation only — see AdminReviewService. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
@Tag(name = "Review (Admin)", description = "Moderation: list any review, delete any review. No editing.")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    /**
     * Examples:
     *   GET /api/admin/reviews?minRating=1&maxRating=2   (surface likely complaints)
     *   GET /api/admin/reviews?productId=42
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminReviewFilterRequest filter = AdminReviewFilterRequest.builder()
                .productId(productId)
                .userId(userId)
                .minRating(minRating)
                .maxRating(maxRating)
                .build();

        return ResponseEntity.ok(adminReviewService.getAllReviews(filter, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        adminReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
