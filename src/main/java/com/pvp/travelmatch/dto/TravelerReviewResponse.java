package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelerReview;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TravelerReviewResponse {

    private Long id;

    private Long reviewerId;

    private String reviewerName;

    private Long travelPlanId;

    private String destination;

    private Integer rating;

    private List<String> tags;

    private String comment;

    private LocalDateTime createdAt;

    public static TravelerReviewResponse
    fromEntity(TravelerReview review) {

        return TravelerReviewResponse.builder()
                .id(review.getId())
                .reviewerId(
                        review.getReviewer().getId()
                )
                .reviewerName(
                        review.getReviewer().getName()
                )
                .travelPlanId(
                        review.getTravelPlan().getId()
                )
                .destination(
                        review.getTravelPlan()
                                .getDestination()
                )
                .rating(review.getRating())
                .tags(
                        review.getTags() == null
                                ? List.of()
                                : List.copyOf(
                                review.getTags()
                        )
                )
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}