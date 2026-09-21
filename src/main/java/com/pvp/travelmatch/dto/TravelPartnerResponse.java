package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TravelPartnerResponse {
    private Long id;
    private UserSummary userOne;
    private UserSummary userTwo;
    private TravelPlanSummary travelPlan;
    private LocalDateTime createdAt;

    @Data @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String name;
        private String city;
        private String country;
    }

    @Data @AllArgsConstructor
    public static class TravelPlanSummary {
        private Long id;
        private String destination;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
