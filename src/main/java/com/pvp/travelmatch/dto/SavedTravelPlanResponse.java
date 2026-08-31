package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.SavedTravelPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class SavedTravelPlanResponse {
    private Long id;
    private Long travelPlanId;
    private Long ownerId;
    private String ownerName;
    private String ownerCity;
    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String status;
    private LocalDateTime createdAt;

    public static SavedTravelPlanResponse fromEntity(SavedTravelPlan saved) {
        var plan = saved.getTravelPlan();
        var owner = plan.getUser();
        return SavedTravelPlanResponse.builder()
                .id(saved.getId())
                .travelPlanId(plan.getId())
                .ownerId(owner == null ? null : owner.getId())
                .ownerName(owner == null ? null : owner.getName())
                .ownerCity(owner == null ? null : owner.getCity())
                .fromLocation(plan.getFromLocation())
                .destination(plan.getDestination())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .budget(plan.getBudget())
                .travelType(plan.getTravelType())
                .status(plan.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
