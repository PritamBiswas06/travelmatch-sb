package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminReviewResponse(
        Long id,
        Long reviewerId,
        String reviewerName,
        Long reviewedUserId,
        String reviewedUserName,
        Long travelPlanId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}
