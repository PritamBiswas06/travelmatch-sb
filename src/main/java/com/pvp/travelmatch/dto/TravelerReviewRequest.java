package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TravelerReviewRequest {

    private Long reviewedUserId;

    private Long travelPlanId;

    @Min(
            value = 1,
            message = "Rating must be between 1 and 5"
    )
    @Max(
            value = 5,
            message = "Rating must be between 1 and 5"
    )
    private Integer rating;

    @Size(
            max = 7,
            message = "Maximum 7 tags allowed"
    )
    private List<String> tags;

    @Size(
            max = 500,
            message = "Review must be 500 characters or less"
    )
    private String comment;
}