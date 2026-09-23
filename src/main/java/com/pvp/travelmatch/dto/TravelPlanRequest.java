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
    private String groupType;
    private Integer currentGroupSize;
    private Integer minGroupSize;
    private Integer maxGroupSize;
    private String lookingFor;
    private Boolean openForJoining;
    private Boolean familyFriendly;
    private Boolean childrenAllowed;
}