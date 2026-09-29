package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelSquadMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TravelSquadMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}
