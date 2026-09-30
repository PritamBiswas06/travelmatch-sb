package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.AnalyticsEventRequest;
import com.pvp.travelmatch.service.AnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analytics;

    @PostMapping("/api/analytics/events")
    public ResponseEntity<Void> record(@Valid @RequestBody AnalyticsEventRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return ResponseEntity.status(401).build();
        analytics.record(authentication.getName(), request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/api/admin/analytics/summary")
    public Map<String, Object> summary(@RequestParam(defaultValue = "30") int days) { return analytics.summary(days); }

    @GetMapping("/api/admin/analytics/activity")
    public Map<String, Object> activity(@RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { return analytics.activity(days, userId, page, size); }

    @GetMapping(value = "/api/admin/analytics/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "30") int days) {
        byte[] body = analytics.csv(days).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=travelmatch-analytics.csv")
                .contentType(new MediaType("text", "csv")).body(body);
    }
}
