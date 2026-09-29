package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TravelSquadInviteRequest {

    @NotNull(message = "User id is required")
    private Long userId;
}
