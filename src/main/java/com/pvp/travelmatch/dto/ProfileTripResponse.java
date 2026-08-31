package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class ProfileTripResponse {

    private Long id; // travelPlanId

    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String status;
    private java.time.LocalDateTime createdAt;
    private long likeCount;
    private long shareCount;
    private long commentCount;
    private String currentUserReaction;
    private boolean currentUserSaved;

    // NONE / PENDING / ACCEPTED / REJECTED — null when viewing your own profile,
    // since you can't send yourself a match request.
    private String matchRequestStatus;
}