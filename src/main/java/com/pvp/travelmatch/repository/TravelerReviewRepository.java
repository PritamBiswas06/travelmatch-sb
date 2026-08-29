package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TravelerReviewRepository
        extends JpaRepository<TravelerReview, Long> {

    boolean existsByReviewerIdAndReviewedUserIdAndTravelPlanId(
            Long reviewerId,
            Long reviewedUserId,
            Long travelPlanId
    );

    List<TravelerReview>
    findByReviewedUserIdOrderByCreatedAtDesc(
            Long reviewedUserId
    );

    long countByReviewedUserId(
            Long reviewedUserId
    );

    @Query("""
        SELECT COALESCE(AVG(r.rating), 0)
        FROM TravelerReview r
        WHERE r.reviewedUser.id = :userId
    """)
    Double averageRating(Long userId);
}