package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Internal parameter object bundling the optional advanced feed filters.
// Every field is optional — a null/blank field means "don't filter on this".
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedFilterRequest {

    private String destination;      // partial, case-insensitive
    private String fromLocation;     // partial, case-insensitive
    private Double minBudget;
    private Double maxBudget;
    private LocalDate startDate;     // trip must overlap this range
    private LocalDate endDate;
    private String travelType;       // exact, case-insensitive
    private Integer minMatchScore;
    private Integer minAge; private Integer maxAge; private String travelStyle; private String travelInterest; private String language; private String country; private String city;   // applied after scoring, not a DB column
}