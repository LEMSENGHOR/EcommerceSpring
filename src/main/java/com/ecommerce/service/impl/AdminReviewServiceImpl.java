package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.AdminReviewFilterRequest;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.entity.Review;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.specification.ReviewSpecification;
import com.ecommerce.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getAllReviews(AdminReviewFilterRequest filter, Pageable pageable) {
        Page<Review> page = reviewRepository.findAll(ReviewSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(ReviewMapper::toResponse));
    }

    @Override
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        // No ownership check — this is moderation, admins can remove any review
        // (e.g. abusive content), unlike ReviewServiceImpl.deleteMyReview which
        // is strictly scoped to the caller's own reviews.
        reviewRepository.delete(review);
    }
}
