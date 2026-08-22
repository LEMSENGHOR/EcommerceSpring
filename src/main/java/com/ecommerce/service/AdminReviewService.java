package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.AdminReviewFilterRequest;
import com.ecommerce.dto.review.ReviewResponse;
import org.springframework.data.domain.Pageable;

/** Moderation only — admins can view/remove any review, but not author one as themselves. */
public interface AdminReviewService {

    PagedResponse<ReviewResponse> getAllReviews(AdminReviewFilterRequest filter, Pageable pageable);

    void deleteReview(Long id);
}
