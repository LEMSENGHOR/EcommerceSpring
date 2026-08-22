package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.CreateReviewRequest;
import com.ecommerce.dto.review.ProductRatingSummaryResponse;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.dto.review.UpdateReviewRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.User;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ReviewResponse createReview(CreateReviewRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (!orderItemRepository.existsDeliveredPurchase(userId, request.getProductId())) {
            throw new InvalidRequestException(
                    "You can only review products from a delivered order");
        }

        if (reviewRepository.findByUserIdAndProductId(userId, request.getProductId()).isPresent()) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return ReviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    public ReviewResponse updateMyReview(Long reviewId, UpdateReviewRequest request) {
        Review review = findOwnedReviewOrThrow(reviewId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return ReviewMapper.toResponse(review);
    }

    @Override
    public void deleteMyReview(Long reviewId) {
        Review review = findOwnedReviewOrThrow(reviewId);
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {
        return ReviewMapper.toResponseList(reviewRepository.findByUserId(SecurityUtils.getCurrentUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        ensureProductExists(productId);
        Page<Review> page = reviewRepository.findByProductId(productId, pageable);
        return PagedResponse.from(page.map(ReviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        ensureProductExists(productId);
        Double average = reviewRepository.findAverageRatingByProductId(productId);
        long count = reviewRepository.countByProductId(productId);
        var breakdown = reviewRepository.findRatingBreakdownByProductId(productId);
        return ReviewMapper.toSummary(productId, average, count, breakdown);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Review findOwnedReviewOrThrow(Long reviewId) {
        return reviewRepository.findByIdAndUserId(reviewId, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
    }
}
