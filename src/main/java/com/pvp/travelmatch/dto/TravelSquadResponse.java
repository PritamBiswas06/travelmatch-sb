package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelSquad;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TravelSquadResponse {
    private Long id;
    private String name;
    private String description;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxMembers;
    private long memberCount;
    private Long creatorId;
    private String creatorName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long travelPlanId;
    private String viewerRole;
    private String viewerMembershipStatus;
    private List<TravelSquadMemberResponse> members;
}
