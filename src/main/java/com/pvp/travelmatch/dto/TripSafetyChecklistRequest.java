package com.pvp.travelmatch.dto;

import jakarta.validation.constraints.NotNull;

public record TripSafetyChecklistRequest(
        @NotNull Boolean publicMeetingPoint,
        @NotNull Boolean itineraryShared,
        @NotNull Boolean emergencyContactInformed,
        @NotNull Boolean transportConfirmed,
        @NotNull Boolean accommodationConfirmed,
        @NotNull Boolean offlineMapsReady,
        @NotNull Boolean checkInPlanAgreed
) {}
