package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelSquadMember;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TravelSquadMemberResponse {
    private Long userId;
    private String name;
    private String city;
    private String country;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
