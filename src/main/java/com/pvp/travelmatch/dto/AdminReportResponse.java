package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long id,
        Long reporterId,
        String reporterName,
        Long reportedUserId,
        String reportedUserName,
        Long reportedTravelPlanId,
        String destination,
        String reason,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
