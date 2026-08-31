package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminMatchRequestResponse(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        Long travelPlanId,
        String destination,
        String status,
        LocalDateTime createdAt
) {}
