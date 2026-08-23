package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class FeedPostResponse {

    private Long id; // travelPlanId, used as postId too

    private Long userId;
    private String userName;
    private String userCity;

    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String status;
    private LocalDateTime createdAt;

    private Integer matchScore; // null if not computable (e.g. viewer has no active plan of their own)

    private long likeCount;
    private long dislikeCount;
    private Integer shareCount;

    private String currentUserReaction; // LIKE / DISLIKE / null

    private String matchRequestStatus; // NONE / PENDING / ACCEPTED / REJECTED
}
