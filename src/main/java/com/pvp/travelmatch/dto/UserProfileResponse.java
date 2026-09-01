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

    private String instagramUrl;
    private String linkedinUrl;
    private String websiteUrl;

    // True when the profile belongs to the currently authenticated user.
    @JsonProperty("isOwnProfile")
    private boolean isOwnProfile;
    private boolean premiumUser;

    private List<ProfileTripResponse> upcomingTrips;
    private List<ProfileTripResponse> posts;
    private List<TravelMemoryResponse> travelMemories;
    private double averageRating;

    private long reviewCount;

    private List<TravelerReviewResponse> reviews;
}