package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminPartnerResponse(
        Long id,
        Long userOneId,
        String userOneName,
        Long userTwoId,
        String userTwoName,
        Long travelPlanId,
        String destination,
        LocalDateTime createdAt
) {}
