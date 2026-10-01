package com.pvp.travelmatch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String name;
    private String username;

    private Integer age;
    private String gender;
    private String city;
    private String state;
    private String country;

    private Boolean verified;

    // Separate trust signals: email verification is not government identity verification.
    private boolean phoneVerified;
    private boolean identityVerified;
    private boolean selfieVerified;
    private boolean profilePhotoReviewed;
    private boolean selfieReviewed;
    private boolean trustedTraveler;

    private String bio;

    // Data URI (e.g. "data:image/png;base64,...") or null if not set — the
    // frontend falls back to an initials avatar when this is null.
    private String profilePhotoUrl;

    private List<String> travelStyle;
    private List<String> travelInterests;
    private List<String> preferredDestinations;
    private String budgetPreference;
    private String travelFrequency;
    private List<String> languages;
    private String idealTravelPartner;

    private Integer dnaAdventureRelaxation;
    private Integer dnaBudgetLuxury;
    private Integer dnaSunriseNightlife;
    private Integer dnaTrekkingSightseeing;
    private Integer dnaFoodCulture;
    private Integer dnaPlannedSpontaneous;
    private Integer dnaSoloGroup;
    private Integer dnaNatureCity;
    private Integer dnaPhotographyActivities;
    private Integer dnaFastSlow;

    private String instagramUrl;
    private String linkedinUrl;
    private String websiteUrl;

    // True when the profile belongs to the currently authenticated user.
    @JsonProperty("isOwnProfile")
    private boolean isOwnProfile;

    // True when the authenticated viewer is already connected to this traveler.
    // This is derived server-side from TravelPartner, never from client input.
    private boolean connectedToViewer;

    // Relationship from the authenticated viewer to this profile:
    // NONE / PENDING / FRIENDS.
    private String relationshipStatus;

    private boolean premiumUser;

    private List<ProfileTripResponse> upcomingTrips;
    private List<ProfileTripResponse> posts;
    private List<TravelMemoryResponse> travelMemories;
    private double averageRating;

    private long reviewCount;

    private List<TravelerReviewResponse> reviews;

    private List<FriendResponse> friends;
    private long friendCount;

    // Pagination metadata for profile lists. Additive fields keep the existing
    // profile UI compatible while preventing unbounded profile payloads.
    private boolean postsHasMore;
    private boolean memoriesHasMore;
    private boolean friendsHasMore;
    private boolean reviewsHasMore;
}