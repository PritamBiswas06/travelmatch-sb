package com.pvp.travelmatch.dto;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;

@Value @Builder
public class TrustRiskSignalResponse {
    Long userId;
    String name;
    String email;
    LocalDateTime createdAt;
    List<String> reviewSignals;
    long reportCount;
}
