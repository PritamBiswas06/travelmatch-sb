package com.pvp.travelmatch.dto;
import com.pvp.travelmatch.entity.TripChecklistItem;
import java.time.LocalDateTime;
public record TripChecklistResponse(Long id, Long createdById, String title, Boolean completed, LocalDateTime createdAt) {
 public static TripChecklistResponse from(TripChecklistItem x) { return new TripChecklistResponse(x.getId(),x.getCreatedBy().getId(),x.getTitle(),x.getCompleted(),x.getCreatedAt()); }
}
