package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class FeedPostResponse {

    private Long id;

    private Long userId;
    private String userName;
    private String userCity;

    // Profile information used by the frontend for the profile avatar.
    private String userGender;
    private String profilePhotoUrl;

    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String status;
    private LocalDateTime createdAt;

    private Integer matchScore;

    private List<CompatibilityFactorResponse> matchFactors;

    private long likeCount;
    private long dislikeCount;
    private Integer shareCount;

    private String currentUserReaction;

    private boolean currentUserSaved;
    private long commentCount;

    private String matchRequestStatus;
}