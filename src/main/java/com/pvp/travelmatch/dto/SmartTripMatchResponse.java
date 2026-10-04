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
public class SmartTripMatchResponse {

    private Long id;
    private Integer score;
    private String matchLevel;
    private List<String> reasons;

    private Long matchedPlanId;
    private Long matchedUserId;
    private String matchedUserName;
    private String matchedUserCity;
    private String profilePhotoUrl;

    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String groupType;
    private Boolean openForJoining;

    private Boolean viewed;
    private LocalDateTime createdAt;
}
