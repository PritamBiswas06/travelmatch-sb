package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelMemory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Base64;

@Data
@Builder
@AllArgsConstructor
public class TravelMemoryResponse {
    private Long id;
    private Long userId;
    private Long travelPlanId;
    private String destination;
    private String fromLocation;
    private String caption;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TravelMemoryResponse fromEntity(TravelMemory memory) {
        String photoUrl = null;
        if (memory.getPhoto() != null && memory.getPhoto().length > 0 && memory.getPhotoContentType() != null) {
            photoUrl = "data:" + memory.getPhotoContentType() + ";base64," +
                    Base64.getEncoder().encodeToString(memory.getPhoto());
        }
        return TravelMemoryResponse.builder()
                .id(memory.getId())
                .userId(memory.getUser().getId())
                .travelPlanId(memory.getTravelPlan().getId())
                .destination(memory.getTravelPlan().getDestination())
                .fromLocation(memory.getTravelPlan().getFromLocation())
                .caption(memory.getCaption())
                .photoUrl(photoUrl)
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
