package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.ReportRequest;
import com.pvp.travelmatch.entity.Report;
import com.pvp.travelmatch.entity.ReportStatus;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.ReportRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    private final UserRepository userRepository;

    private final TravelPlanRepository travelPlanRepository;

    private final AdminEmailService adminEmailService;

//    @Value("${TRAVELMATCH_ADMIN_EMAILS:}")
//    private String adminEmails;

    private User getCurrentUser() {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    @Transactional
    public Report reportUser(
            Long userId,
            ReportRequest request) {

        User reporter = getCurrentUser();

        User reportedUser =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Traveler not found"
                                ));

        validate(request);

        if (reporter.getId()
                .equals(reportedUser.getId())) {

            throw new RuntimeException(
                    "You cannot report yourself"
            );
        }

        if (reportRepository
                .existsByReporterIdAndReportedUserId(
                        reporter.getId(),
                        reportedUser.getId()
                )) {

            throw new RuntimeException(
                    "You have already reported this user"
            );
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(request.getReason())
                .description(
                        clean(request.getDescription())
                )
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return reportRepository.save(report);
    }

    @Transactional
    public Report reportPost(
            Long travelPlanId,
            ReportRequest request) {

        User reporter = getCurrentUser();

        var travelPlan =
                travelPlanRepository.findById(travelPlanId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Travel post not found"
                                ));

        validate(request);

        if (reportRepository
                .existsByReporterIdAndReportedTravelPlanId(
                        reporter.getId(),
                        travelPlan.getId()
                )) {

            throw new RuntimeException(
                    "You have already reported this post"
            );
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedTravelPlan(travelPlan)
                .reason(request.getReason())
                .description(
                        clean(request.getDescription())
                )
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return reportRepository.save(report);
    }

    public List<Report> getAdminReports() {

        User currentUser = getCurrentUser();

        if (!adminEmailService.isAdminEmail(currentUser.getEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin access required"
            );
        }

        return reportRepository
                .findAllByOrderByCreatedAtDesc();
    }

    private void validate(
            ReportRequest request) {

        if (request == null ||
                request.getReason() == null) {

            throw new RuntimeException(
                    "Please select a report reason"
            );
        }
    }

    private String clean(String value) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}