package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PhotoReviewDecisionRequest {
    @NotBlank private String status; // APPROVED or REJECTED
    @Size(max = 250) private String note;
}
