package com.pvp.travelmatch.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    private Integer age;
    private String gender;
    private String city;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TravelPlan> travelPlans;


    @Column(nullable = false)
    private Boolean verified = false;

    private String verificationCode;

    private LocalDateTime codeExpiry;

    @Column(name = "reset_code")
    private String resetCode;

    @Column(name = "reset_code_expiry")
    private LocalDateTime resetCodeExpiry;

    // ==================== PROFILE FIELDS ====================
    // Kept on User rather than a separate Profile entity: it's a handful of
    // simple, always-present fields, so a 1:1 join table would add complexity
    // without benefit. All fields below are nullable, so existing users are
    // unaffected and simply show sensible "not set" defaults until they fill
    // their profile in.

    // Optional unique handle, separate from the display name.
    @Column(unique = true, length = 30)
    private String username;

    private String state;
    private String country;

    @Column(length = 500)
    private String bio;

    // Comma-separated multi-select, e.g. "Adventure,Backpacking,Solo"
    @Column(length = 500)
    private String travelStyle;

    // Comma-separated multi-select, e.g. "Hiking,Photography,Food"
    @Column(length = 500)
    private String travelInterests;

    // Comma-separated list, e.g. "Goa,Manali,Kerala"
    @Column(length = 500)
    private String preferredDestinations;

    // Free-text budget preference, e.g. "₹10,000 - ₹20,000"
    private String budgetPreference;

    // e.g. "Occasionally" / "Monthly" / "Frequently"
    private String travelFrequency;

    // Comma-separated multi-select, e.g. "English,Hindi,Bengali"
    @Column(length = 300)
    private String languages;

    @Column(length = 500)
    private String idealTravelPartner;

    // Optional public social links — only shown if the user fills them in.
    private String instagramUrl;
    private String linkedinUrl;
    private String websiteUrl;

    // Profile photo stored directly in the database (no existing file/cloud
    // storage mechanism to reuse, and this avoids relying on ephemeral local
    // disk storage on the deployment platform).
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] profilePhoto;

    private String profilePhotoContentType;
}