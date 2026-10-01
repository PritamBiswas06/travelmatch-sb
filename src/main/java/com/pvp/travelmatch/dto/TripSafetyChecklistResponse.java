package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TripSafetyChecklist;
import java.time.LocalDateTime;

public record TripSafetyChecklistResponse(
        Long travelPlanId, boolean publicMeetingPoint, boolean itineraryShared,
        boolean emergencyContactInformed, boolean transportConfirmed,
        boolean accommodationConfirmed, boolean offlineMapsReady,
        boolean checkInPlanAgreed, LocalDateTime updatedAt
) {
    public static TripSafetyChecklistResponse from(TripSafetyChecklist c) {
        return new TripSafetyChecklistResponse(c.getTravelPlan().getId(), c.isPublicMeetingPoint(),
                c.isItineraryShared(), c.isEmergencyContactInformed(), c.isTransportConfirmed(),
                c.isAccommodationConfirmed(), c.isOfflineMapsReady(), c.isCheckInPlanAgreed(), c.getUpdatedAt());
    }
}
