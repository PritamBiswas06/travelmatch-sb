package com.pvp.travelmatch.dto;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value @Builder
public class TrustCenterResponse {
    boolean emailVerified;
    boolean phoneVerified;
    String phoneNumber;
    String identityStatus;
    String selfieStatus;
    String profilePhotoReviewStatus;
    String profilePhotoReviewNote;
    String selfieReviewStatus;
    String selfieReviewNote;
    int profileCompletion;
    boolean travelDnaComplete;
    long completedTrips;
    double averageRating;
    long reviewCount;
    boolean emergencySharingEnabled;
    boolean profileDiscoverable;
    List<String> earnedBadges;
    List<SafetyContactResponse> safetyCircle;
    String identityProviderStatus;
    String selfieProviderStatus;

    @Value @Builder
    public static class SafetyContactResponse {
        Long id; String name; String phone; String email; String relationship;
    }
}
