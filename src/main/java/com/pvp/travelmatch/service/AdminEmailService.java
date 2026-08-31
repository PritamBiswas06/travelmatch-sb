package com.pvp.travelmatch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AdminEmailService {

    @Value("${TRAVELMATCH_ADMIN_EMAILS:}")
    private String adminEmails;

    public boolean isAdminEmail(String email) {

        if (email == null || email.isBlank()) {
            return false;
        }

        return Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(e -> !e.isBlank())
                .anyMatch(e ->
                        e.equalsIgnoreCase(email.trim())
                );
    }
}