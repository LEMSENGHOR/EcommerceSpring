package com.ecommerce.mapper;

import com.ecommerce.dto.review.ProductRatingSummaryResponse;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.entity.Review;
import com.ecommerce.repository.ReviewRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                // Always true: ReviewServiceImpl.createReview only ever persists a
                // Review after a verified-delivered-purchase check — see README.
                .verifiedPurchase(true)
                .createdAt(review.getCreatedAt())
                .build();
    }

    public static List<ReviewResponse> toResponseList(List<Review> reviews) {
        return reviews.stream().map(ReviewMapper::toResponse).toList();
    }

    public static ProductRatingSummaryResponse toSummary(
            Long productId, Double average, long count, List<ReviewRepository.RatingCountProjection> breakdown) {

        Map<Integer, Long> ratingBreakdown = new HashMap<>();
        for (int star = 1; star <= 5; star++) {
            ratingBreakdown.put(star, 0L);
        }
        for (ReviewRepository.RatingCountProjection row : breakdown) {
            ratingBreakdown.put(row.getRating(), row.getCount());
        }

        double rounded = Math.round((average != null ? average : 0.0) * 10) / 10.0;

        return ProductRatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(rounded)
                .reviewCount(count)
                .ratingBreakdown(ratingBreakdown)
                .build();
    }
}
