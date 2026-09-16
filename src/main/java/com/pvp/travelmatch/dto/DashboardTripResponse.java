package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTripResponse {

    private Long id;
    private String fromLocation;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
    private String status;

    public static DashboardTripResponse fromEntity(
            TravelPlan plan
    ) {
        return new DashboardTripResponse(
                plan.getId(),
                plan.getFromLocation(),
                plan.getDestination(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getBudget(),
                plan.getTravelType(),
                plan.getStatus()
        );
    }
}