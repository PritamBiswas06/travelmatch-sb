package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.ReportRequest;
import com.pvp.travelmatch.entity.Report;
import com.pvp.travelmatch.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/user/{userId}")
    public Map<String, String> reportUser(
            @PathVariable Long userId,
            @Valid @RequestBody ReportRequest request) {

        reportService.reportUser(
                userId,
                request
        );

        return Map.of(
                "message",
                "Report submitted successfully"
        );
    }

    @PostMapping("/post/{travelPlanId}")
    public Map<String, String> reportPost(
            @PathVariable Long travelPlanId,
            @Valid @RequestBody ReportRequest request) {

        reportService.reportPost(
                travelPlanId,
                request
        );

        return Map.of(
                "message",
                "Report submitted successfully"
        );
    }

    @GetMapping("/admin")
    public List<Report> adminReports() {

        return reportService.getAdminReports();
    }
}