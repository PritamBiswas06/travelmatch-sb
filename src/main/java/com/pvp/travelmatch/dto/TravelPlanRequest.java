package com.pvp.travelmatch.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TravelPlanRequest {

    private String destination;
    private String fromLocation;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double budget;
    private String travelType;
}