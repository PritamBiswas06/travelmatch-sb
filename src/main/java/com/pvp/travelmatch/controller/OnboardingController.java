package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final UserRepository userRepository;

    @Value("${travelmatch.terms.version:1.0}")
    private String termsVersion;

    @GetMapping("/status")
    public Map<String, Object> status() {
        User user = currentUser();
        return Map.of(
                "requiresOnboarding", Boolean.TRUE.equals(user.getOnboardingRequired()),
                "termsAccepted", Boolean.TRUE.equals(user.getTermsAccepted()),
                "termsVersion", termsVersion
        );
    }

    @PostMapping("/accept-terms")
    public Map<String, Object> acceptTerms() {
        User user = currentUser();

        if (!Boolean.TRUE.equals(user.getOnboardingRequired())) {
            return Map.of("message", "Onboarding already completed", "termsAccepted", Boolean.TRUE.equals(user.getTermsAccepted()));
        }

        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setTermsVersion(termsVersion);
        userRepository.save(user);

        return Map.of(
                "message", "Terms and Conditions accepted",
                "termsAccepted", true,
                "termsVersion", termsVersion
        );
    }

    @PostMapping("/complete")
    public Map<String, String> complete() {
        User user = currentUser();

        if (!Boolean.TRUE.equals(user.getOnboardingRequired())) {
            return Map.of("message", "Onboarding already completed");
        }

        if (!Boolean.TRUE.equals(user.getTermsAccepted())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You must accept the Terms and Conditions before completing onboarding"
            );
        }

        user.setOnboardingRequired(false);
        userRepository.save(user);

        return Map.of("message", "Onboarding completed");
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
