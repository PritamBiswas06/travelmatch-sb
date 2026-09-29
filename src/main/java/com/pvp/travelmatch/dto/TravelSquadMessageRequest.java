package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TravelSquadMessageRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1500, message = "Message cannot exceed 1500 characters")
    private String content;
}
