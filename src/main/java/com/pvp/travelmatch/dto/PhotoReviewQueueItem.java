package com.pvp.travelmatch.dto;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;

@Value @Builder
public class PhotoReviewQueueItem {
    Long userId;
    String name;
    String email;
    String photoDataUrl;
    String status;
    String reviewType;
    LocalDateTime submittedAt;
}
