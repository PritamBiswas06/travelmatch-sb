package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long userId;
    private String name;
    private String role;

    /* True only when this account still needs first-login onboarding. */
    private boolean requiresOnboarding;

    /* Included so the frontend knows the server-side acceptance state. */
    private boolean termsAccepted;
}
