package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.ReportReason;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRequest {

    private ReportReason reason;

    @Size(
            max = 500,
            message = "Description must be 500 characters or less"
    )
    private String description;
}