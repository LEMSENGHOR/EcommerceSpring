package com.ecommerce.repository;

import com.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    Page<Review> findByProductId(Long productId, Pageable pageable);
    List<Review> findByUserId(Long userId);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    /** Used to enforce that a review can only be edited/deleted via its owner. */
    Optional<Review> findByIdAndUserId(Long id, Long userId);

    long countByProductId(Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT r.rating AS rating, COUNT(r) AS count
            FROM Review r
            WHERE r.product.id = :productId
            GROUP BY r.rating
            """)
    List<RatingCountProjection> findRatingBreakdownByProductId(@Param("productId") Long productId);

    interface RatingCountProjection {
        Integer getRating();
        Long getCount();
    }
}
