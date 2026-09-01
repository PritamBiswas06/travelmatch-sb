package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.entity.ReportStatus;
import com.pvp.travelmatch.service.AdminService;
import com.pvp.travelmatch.service.MonetizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MonetizationService monetizationService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> users(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.users(search, page, size);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminActionRequest request) {
        return adminService.updateUserStatus(id, request);
    }

    @PatchMapping("/users/{id}/role")
    public AdminUserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminActionRequest request) {
        return adminService.updateUserRole(id, request);
    }

    @GetMapping("/trips")
    public Page<AdminTripResponse> trips(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.trips(search, page, size);
    }

    @PatchMapping("/trips/{id}/status")
    public AdminTripResponse updateTripStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminActionRequest request) {
        return adminService.updateTripStatus(id, request);
    }

    @GetMapping("/reports")
    public Page<AdminReportResponse> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.reports(status, page, size);
    }

    @PatchMapping("/reports/{id}")
    public AdminReportResponse updateReport(
            @PathVariable Long id,
            @Valid @RequestBody AdminActionRequest request) {
        return adminService.updateReport(id, request);
    }

    @PatchMapping("/reports/{id}/note")
    public AdminReportResponse addReportNote(
            @PathVariable Long id,
            @Valid @RequestBody AdminActionRequest request) {
        return adminService.addReportNote(id, request);
    }

    @GetMapping("/reviews")
    public Page<AdminReviewResponse> reviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.reviews(page, size);
    }

    @DeleteMapping("/reviews/{id}")
    public void removeReview(
            @PathVariable Long id,
            @RequestBody(required = false) AdminActionRequest request) {
        AdminActionRequest safe = request == null
                ? new AdminActionRequest("REMOVE", "")
                : request;
        adminService.removeReview(id, safe);
    }

    @GetMapping("/match-requests")
    public Page<AdminMatchRequestResponse> matchRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.matchRequests(page, size);
    }

    @GetMapping("/partners")
    public Page<AdminPartnerResponse> partners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.partners(page, size);
    }

    @GetMapping("/monetization/metrics") public java.util.Map<String,Object> monetizationMetrics(){ return monetizationService.adminMetrics(); }
    @GetMapping("/monetization/payments") public Page<AdminPaymentResponse> monetizationPayments(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){ return monetizationService.adminPayments(page,size); }

    @GetMapping("/audit-logs")
    public Page<AdminAuditLogResponse> auditLogs(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.auditLogs(search, page, size);
    }
}
