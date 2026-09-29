package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TravelSquadCreateRequest {

    @NotBlank(message = "Squad name is required")
    @Size(max = 80, message = "Squad name cannot exceed 80 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Long travelPlanId;

    @Size(max = 120, message = "Destination cannot exceed 120 characters")
    private String destination;

    private LocalDate startDate;
    private LocalDate endDate;

    @Min(value = 2, message = "A squad needs at least 2 members")
    @Max(value = 20, message = "A squad cannot have more than 20 members")
    private Integer maxMembers = 6;
}
