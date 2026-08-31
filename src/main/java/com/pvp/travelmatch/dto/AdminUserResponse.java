package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String username,
        String city,
        Boolean verified,
        String role,
        String accountStatus,
        LocalDateTime createdAt
) {}
