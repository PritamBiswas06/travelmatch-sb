package com.pvp.travelmatch.dto;

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
    private String city;
    private Boolean verified;

    private String bio;
    private String travelStyle;
    private List<String> travelInterests;
    private List<String> preferredDestinations;
    private String budgetPreference;

    // True when the profile belongs to the currently authenticated user.
    private boolean isOwnProfile;

    private List<ProfileTripResponse> upcomingTrips;
}