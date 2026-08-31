package com.pvp.travelmatch.dto;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long adminId,
        String adminName,
        String action,
        String targetType,
        Long targetId,
        String description,
        LocalDateTime createdAt
) {}
