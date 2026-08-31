package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminActionRequest(
        @NotBlank(message = "Action value is required") String value,
        String reason
) {}
