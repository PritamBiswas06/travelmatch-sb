package com.pvp.travelmatch.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminTripResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String fromLocation,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Double budget,
        String travelType,
        String status,
        LocalDateTime createdAt
) {}
