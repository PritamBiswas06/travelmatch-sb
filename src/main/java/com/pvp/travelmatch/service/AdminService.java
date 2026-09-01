package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.entity.*;
import com.pvp.travelmatch.entity.AuditLog;
import com.pvp.travelmatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final ReportRepository reportRepository;
    private final TravelerReviewRepository reviewRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final TravelPartnerRepository partnerRepository;
    private final AuditLogRepository auditLogRepository;
    private final MonetizationService monetizationService;


    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    public AdminDashboardResponse dashboard() {

        requireAdmin();

        return new AdminDashboardResponse(

                // 1. Total Users
                userRepository.count(),

                // 2. Active Users
                userRepository.countByAccountStatus(
                        AccountStatus.ACTIVE
                ),

                // 3. Suspended Users
                userRepository.countByAccountStatus(
                        AccountStatus.SUSPENDED
                ),

                // 4. Total Trips
                travelPlanRepository.count(),

                // 5. Active Trips
                travelPlanRepository.countByStatus("ACTIVE"),

                // 6. Total Reports
                reportRepository.count(),

                // 7. Pending Reports
                reportRepository.countByStatus(
                        ReportStatus.PENDING
                ),

                // 8. Total Reviews
                reviewRepository.count(),

                // 9. Total Match Requests
                matchRequestRepository.count(),

                // 10. Total Travel Partners
                partnerRepository.count(),
                ((Number)monetizationService.adminMetrics().get("premiumUsers")).longValue(),
                ((Number)monetizationService.adminMetrics().get("activeSubscriptions")).longValue(),
                ((Number)monetizationService.adminMetrics().get("totalPayments")).longValue(),
                ((Number)monetizationService.adminMetrics().get("successfulPayments")).longValue(),
                ((Number)monetizationService.adminMetrics().get("failedPayments")).longValue(),
                ((Number)monetizationService.adminMetrics().get("totalBoosts")).longValue(),
                ((Number)monetizationService.adminMetrics().get("activeBoosts")).longValue(),
                ((Number)monetizationService.adminMetrics().get("revenuePaise")).longValue()
        );
    }


    // ============================================================
    // USERS
    // ============================================================

    public Page<AdminUserResponse> users(
            String search,
            int page,
            int size
    ) {

        requireAdmin();

        Pageable pageable = pageable(page, size);

        String q = clean(search);

        Page<User> result =
                q == null
                        ? userRepository.findAll(pageable)
                        : userRepository
                        .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                                q,
                                q,
                                pageable
                        );

        return result.map(this::toUser);
    }


    // ============================================================
    // UPDATE USER STATUS
    // ============================================================

    @Transactional
    public AdminUserResponse updateUserStatus(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        AccountStatus status =
                parseEnum(
                        AccountStatus.class,
                        request.value(),
                        "Invalid account status"
                );

        User user =
                userRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("User not found")
                        );

        if (
                admin.getId().equals(user.getId())
                        && status != AccountStatus.ACTIVE
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot suspend or deactivate your own admin account"
            );
        }

        user.setAccountStatus(status);

        User saved =
                userRepository.save(user);

        audit(
                admin,
                "USER_STATUS_CHANGED",
                "USER",
                id,
                "Changed account status to "
                        + status
                        + reasonSuffix(request.reason())
        );

        return toUser(saved);
    }


    // ============================================================
    // UPDATE USER ROLE
    // ============================================================

    @Transactional
    public AdminUserResponse updateUserRole(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        Role role =
                parseEnum(
                        Role.class,
                        request.value(),
                        "Invalid role"
                );

        User user =
                userRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("User not found")
                        );

        if (
                admin.getId().equals(user.getId())
                        && role != Role.ADMIN
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot remove your own admin access"
            );
        }

        if (
                user.getRole() == Role.ADMIN
                        && role != Role.ADMIN
                        && userRepository.countByRole(Role.ADMIN) <= 1
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one administrator must remain"
            );
        }

        user.setRole(role);

        User saved =
                userRepository.save(user);

        audit(
                admin,
                "USER_ROLE_CHANGED",
                "USER",
                id,
                "Changed role to "
                        + role
                        + reasonSuffix(request.reason())
        );

        return toUser(saved);
    }


    // ============================================================
    // TRIPS
    // ============================================================

    public Page<AdminTripResponse> trips(
            String search,
            int page,
            int size
    ) {

        requireAdmin();

        Pageable pageable =
                pageable(page, size);

        String q = clean(search);

        Page<TravelPlan> result =
                q == null
                        ? travelPlanRepository.findAll(pageable)
                        : travelPlanRepository
                        .findByDestinationContainingIgnoreCaseOrFromLocationContainingIgnoreCase(
                                q,
                                q,
                                pageable
                        );

        return result.map(this::toTrip);
    }


    // ============================================================
    // UPDATE TRIP STATUS
    // ============================================================

    @Transactional
    public AdminTripResponse updateTripStatus(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        String status =
                request.value()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (
                !status.matches(
                        "ACTIVE|COMPLETED|CANCELLED|REMOVED"
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid trip status"
            );
        }

        TravelPlan trip =
                travelPlanRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("Trip not found")
                        );

        trip.setStatus(status);

        TravelPlan saved =
                travelPlanRepository.save(trip);

        audit(
                admin,
                "TRIP_STATUS_CHANGED",
                "TRIP",
                id,
                "Changed trip status to "
                        + status
                        + reasonSuffix(request.reason())
        );

        return toTrip(saved);
    }


    // ============================================================
    // REPORTS
    // ============================================================

    public Page<AdminReportResponse> reports(
            ReportStatus status,
            int page,
            int size
    ) {

        requireAdmin();

        Pageable pageable =
                pageable(page, size);

        Page<Report> result =
                status == null
                        ? reportRepository
                        .findAllByOrderByCreatedAtDesc(
                                pageable
                        )
                        : reportRepository
                        .findByStatusOrderByCreatedAtDesc(
                                status,
                                pageable
                        );

        return result.map(this::toReport);
    }


    // ============================================================
    // UPDATE REPORT
    // ============================================================

    @Transactional
    public AdminReportResponse updateReport(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        ReportStatus status =
                parseEnum(
                        ReportStatus.class,
                        request.value(),
                        "Invalid report status"
                );

        Report report =
                reportRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("Report not found")
                        );

        report.setStatus(status);

        if (
                request.reason() != null
                        && !request.reason().isBlank()
        ) {

            report.setResolution(
                    request.reason().trim()
            );
        }

        Report saved =
                reportRepository.save(report);

        audit(
                admin,
                "REPORT_" + status.name(),
                "REPORT",
                id,
                "Changed report status to "
                        + status
                        + reasonSuffix(request.reason())
        );

        return toReport(saved);
    }


    // ============================================================
    // REPORT NOTE
    // ============================================================

    @Transactional
    public AdminReportResponse addReportNote(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        if (
                request.reason() == null
                        || request.reason().isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Note is required"
            );
        }

        Report report =
                reportRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("Report not found")
                        );

        report.setAdminNote(
                request.reason().trim()
        );

        Report saved =
                reportRepository.save(report);

        audit(
                admin,
                "REPORT_NOTE_ADDED",
                "REPORT",
                id,
                "Updated internal moderation note"
        );

        return toReport(saved);
    }


    // ============================================================
    // REVIEWS
    // ============================================================

    public Page<AdminReviewResponse> reviews(
            int page,
            int size
    ) {

        requireAdmin();

        return reviewRepository
                .findAllByOrderByCreatedAtDesc(
                        pageable(page, size)
                )
                .map(this::toReview);
    }


    // ============================================================
    // REMOVE REVIEW
    // ============================================================

    @Transactional
    public void removeReview(
            Long id,
            AdminActionRequest request
    ) {

        User admin = requireAdmin();

        TravelerReview review =
                reviewRepository.findById(id)
                        .orElseThrow(
                                () -> notFound("Review not found")
                        );

        String description =
                "Removed review by "
                        + review.getReviewer().getName()
                        + reasonSuffix(request.reason());

        reviewRepository.delete(review);

        audit(
                admin,
                "REVIEW_REMOVED",
                "REVIEW",
                id,
                description
        );
    }


    // ============================================================
    // MATCH REQUESTS
    // ============================================================

    public Page<AdminMatchRequestResponse> matchRequests(
            int page,
            int size
    ) {

        requireAdmin();

        return matchRequestRepository
                .findAllByOrderByCreatedAtDesc(
                        pageable(page, size)
                )
                .map(this::toMatchRequest);
    }


    // ============================================================
    // PARTNERS
    // ============================================================

    public Page<AdminPartnerResponse> partners(
            int page,
            int size
    ) {

        requireAdmin();

        return partnerRepository
                .findAllByOrderByCreatedAtDesc(
                        pageable(page, size)
                )
                .map(this::toPartner);
    }


    // ============================================================
    // AUDIT LOGS
    // ============================================================

    public Page<AdminAuditLogResponse> auditLogs(
            String search,
            int page,
            int size
    ) {

        requireAdmin();

        return auditLogRepository
                .search(
                        clean(search),
                        pageable(page, size)
                )
                .map(this::toAudit);
    }


    // ============================================================
    // REQUIRE ADMIN
    // ============================================================

    public User requireAdmin() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication.getPrincipal() == null
                        || "anonymousUser".equals(
                        authentication.getPrincipal()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        String email =
                authentication.getName();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "User not found"
                                )
                        );

        if (user.getRole() != Role.ADMIN) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin access required"
            );
        }

        if (
                user.getAccountStatus()
                        != AccountStatus.ACTIVE
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin account is not active"
            );
        }

        return user;
    }


    // ============================================================
    // AUDIT
    // ============================================================

    private void audit(
            User admin,
            String action,
            String targetType,
            Long targetId,
            String description
    ) {

        auditLogRepository.save(
                AuditLog.builder()
                        .admin(admin)
                        .action(action)
                        .targetType(targetType)
                        .targetId(targetId)
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }


    // ============================================================
    // PAGINATION
    // ============================================================

    private Pageable pageable(
            int page,
            int size
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }


    // ============================================================
    // CLEAN SEARCH
    // ============================================================

    private String clean(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }


    // ============================================================
    // REASON
    // ============================================================

    private String reasonSuffix(
            String reason
    ) {

        return reason == null
                || reason.isBlank()
                ? ""
                : " | Reason: " + reason.trim();
    }


    // ============================================================
    // ENUM PARSER
    // ============================================================

    private <E extends Enum<E>> E parseEnum(
            Class<E> type,
            String value,
            String message
    ) {

        if (value == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }


    // ============================================================
    // NOT FOUND
    // ============================================================

    private ResponseStatusException notFound(
            String message
    ) {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }


    // ============================================================
    // USER MAPPER
    // ============================================================

    private AdminUserResponse toUser(
            User u
    ) {

        return new AdminUserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getUsername(),
                u.getCity(),
                u.getVerified(),
                u.getRole().name(),
                u.getAccountStatus().name(),
                u.getCreatedAt()
        );
    }


    // ============================================================
    // TRIP MAPPER
    // ============================================================

    private AdminTripResponse toTrip(
            TravelPlan t
    ) {

        User u = t.getUser();

        return new AdminTripResponse(
                t.getId(),
                u == null ? null : u.getId(),
                u == null ? null : u.getName(),
                u == null ? null : u.getEmail(),
                t.getFromLocation(),
                t.getDestination(),
                t.getStartDate(),
                t.getEndDate(),
                t.getBudget(),
                t.getTravelType(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }


    // ============================================================
    // REPORT MAPPER
    // ============================================================

    private AdminReportResponse toReport(
            Report r
    ) {

        User reporter = r.getReporter();
        User reported = r.getReportedUser();
        TravelPlan trip = r.getReportedTravelPlan();

        return new AdminReportResponse(
                r.getId(),

                reporter == null
                        ? null
                        : reporter.getId(),

                reporter == null
                        ? null
                        : reporter.getName(),

                reported == null
                        ? null
                        : reported.getId(),

                reported == null
                        ? null
                        : reported.getName(),

                trip == null
                        ? null
                        : trip.getId(),

                trip == null
                        ? null
                        : trip.getDestination(),

                r.getReason().name(),
                r.getDescription(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }


    // ============================================================
    // REVIEW MAPPER
    // ============================================================

    private AdminReviewResponse toReview(
            TravelerReview r
    ) {

        return new AdminReviewResponse(
                r.getId(),
                r.getReviewer().getId(),
                r.getReviewer().getName(),
                r.getReviewedUser().getId(),
                r.getReviewedUser().getName(),
                r.getTravelPlan().getId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }


    // ============================================================
    // MATCH REQUEST MAPPER
    // ============================================================

    private AdminMatchRequestResponse toMatchRequest(
            MatchRequest r
    ) {

        return new AdminMatchRequestResponse(
                r.getId(),
                r.getSender().getId(),
                r.getSender().getName(),
                r.getReceiver().getId(),
                r.getReceiver().getName(),
                r.getTravelPlan().getId(),
                r.getTravelPlan().getDestination(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }


    // ============================================================
    // PARTNER MAPPER
    // ============================================================

    private AdminPartnerResponse toPartner(
            TravelPartner p
    ) {

        return new AdminPartnerResponse(
                p.getId(),
                p.getUserOne().getId(),
                p.getUserOne().getName(),
                p.getUserTwo().getId(),
                p.getUserTwo().getName(),
                p.getTravelPlan().getId(),
                p.getTravelPlan().getDestination(),
                p.getCreatedAt()
        );
    }


    // ============================================================
    // AUDIT MAPPER
    // ============================================================

    private AdminAuditLogResponse toAudit(
            AuditLog a
    ) {

        return new AdminAuditLogResponse(
                a.getId(),
                a.getAdmin().getId(),
                a.getAdmin().getName(),
                a.getAction(),
                a.getTargetType(),
                a.getTargetId(),
                a.getDescription(),
                a.getCreatedAt()
        );
    }
}