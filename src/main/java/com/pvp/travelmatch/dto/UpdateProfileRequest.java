package com.pvp.travelmatch.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    // Every field is optional — only fields that are present (non-null) are
    // applied, so editing one field never wipes out the others. To clear a
    // text field, send an empty string; to clear a list, send an empty list.

    private String name;
    private String username;

    private Integer age;
    private String gender;
    private String city;
    private String state;
    private String country;

    private String bio;

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
}