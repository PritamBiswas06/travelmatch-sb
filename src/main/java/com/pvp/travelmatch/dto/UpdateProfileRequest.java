package com.pvp.travelmatch.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    private String city;
    private String bio;
    private String travelStyle;
    private String budgetPreference;
    private List<String> travelInterests;
    private List<String> preferredDestinations;
}