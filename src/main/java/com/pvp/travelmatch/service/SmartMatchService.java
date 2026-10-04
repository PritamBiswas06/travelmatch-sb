package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.SmartTripMatchResponse;
import com.pvp.travelmatch.entity.SmartTripMatch;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.SmartTripMatchRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.TrustVerificationRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartMatchService {

    private static final int MIN_MATCH_SCORE = 55;
    private static final int MAX_MATCHES_PER_PLAN = 25;

    private final SmartTripMatchRepository smartTripMatchRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;
    private final TrustVerificationRepository trustVerificationRepository;
    private final CompatibilityService compatibilityService;
    private final NotificationService notificationService;
    private final BlockedUserService blockedUserService;

    /**
     * Rebuilds the smart-match records affected by one trip.
     * Called after a trip is created or edited and also when the user opens
     * Smart Matches so newly-created compatible trips are discovered.
     */
    @Transactional
    public void processForPlan(TravelPlan sourcePlan) {
        if (!isEligible(sourcePlan)) {
            return;
        }

        List<TravelPlan> candidates = travelPlanRepository.findSmartMatchCandidates(
                sourcePlan.getDestination(),
                sourcePlan.getStartDate(),
                sourcePlan.getEndDate(),
                sourcePlan.getUser().getId(),
                LocalDate.now()
        );

        int processed = 0;
        for (TravelPlan candidate : candidates) {
            if (processed >= MAX_MATCHES_PER_PLAN) {
                break;
            }
            if (!isEligible(candidate)
                    || blockedUserService.isBlockedEitherWay(
                    sourcePlan.getUser().getId(), candidate.getUser().getId())) {
                continue;
            }

            upsertPair(sourcePlan, candidate);
            processed++;
        }
    }

    @Transactional
    public List<SmartTripMatchResponse> getMyMatches() {
        User currentUser = getCurrentUser();

        // Refresh each active upcoming plan. This makes the feature resilient
        // even when the other traveler created their trip before this user.
        travelPlanRepository.findUpcomingByUserId(
                currentUser.getId(),
                LocalDate.now(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent().forEach(this::processForPlan);

        return smartTripMatchRepository.findVisibleMatchesForUser(currentUser.getId())
                .stream()
                .filter(match -> hasUpcomingPlansForBothUsers(match))
                .map(match -> toResponse(match, currentUser))
                .toList();
    }

    @Transactional
    public SmartTripMatchResponse markViewed(Long matchId) {
        User currentUser = getCurrentUser();
        SmartTripMatch match = getOwnedMatch(matchId, currentUser.getId());
        setViewed(match, currentUser.getId(), true);
        smartTripMatchRepository.save(match);
        return toResponse(match, currentUser);
    }

    @Transactional
    public void dismiss(Long matchId) {
        User currentUser = getCurrentUser();
        SmartTripMatch match = getOwnedMatch(matchId, currentUser.getId());
        setDismissed(match, currentUser.getId(), true);
        smartTripMatchRepository.save(match);
    }

    private void upsertPair(TravelPlan first, TravelPlan second) {
        TravelPlan planA = first.getId() < second.getId() ? first : second;
        TravelPlan planB = first.getId() < second.getId() ? second : first;

        CompatibilityService.CompatibilityResult aToB = compatibilityService.calculate(
                planA.getUser(), planA, planB.getUser(), planB);
        CompatibilityService.CompatibilityResult bToA = compatibilityService.calculate(
                planB.getUser(), planB, planA.getUser(), planA);

        if (aToB.score() == null && bToA.score() == null) {
            return;
        }

        int score = averageScore(aToB.score(), bToA.score());
        if (score < MIN_MATCH_SCORE) {
            return;
        }

        SmartTripMatch match = smartTripMatchRepository
                .findByPlanAIdAndPlanBId(planA.getId(), planB.getId())
                .orElseGet(() -> SmartTripMatch.builder()
                        .planA(planA)
                        .planB(planB)
                        .createdAt(LocalDateTime.now())
                        .build());

        match.setScore(score);
        match.setUpdatedAt(LocalDateTime.now());

        boolean firstNotification = match.getNotifiedAt() == null;
        SmartTripMatch saved = smartTripMatchRepository.save(match);

        if (firstNotification) {
            notifyBothUsers(saved);
            saved.setNotifiedAt(LocalDateTime.now());
            smartTripMatchRepository.save(saved);
        }
    }

    private void notifyBothUsers(SmartTripMatch match) {
        User userA = match.getPlanA().getUser();
        User userB = match.getPlanB().getUser();
        String destination = safe(match.getPlanA().getDestination(), "your destination");
        String level = matchLevel(match.getScore());

        notificationService.createNotification(
                userA,
                userB,
                "✨ New Smart Match: " + userB.getName() + " is planning " + destination
                        + " around your travel dates (" + match.getScore() + "% " + level + ").",
                com.pvp.travelmatch.entity.NotificationType.SMART_TRIP_MATCH,
                match.getId()
        );

        notificationService.createNotification(
                userB,
                userA,
                "✨ New Smart Match: " + userA.getName() + " is planning " + destination
                        + " around your travel dates (" + match.getScore() + "% " + level + ").",
                com.pvp.travelmatch.entity.NotificationType.SMART_TRIP_MATCH,
                match.getId()
        );
    }

    private SmartTripMatchResponse toResponse(SmartTripMatch match, User viewer) {
        boolean viewerIsA = match.getPlanA().getUser().getId().equals(viewer.getId());
        TravelPlan candidatePlan = viewerIsA ? match.getPlanB() : match.getPlanA();
        User candidateUser = candidatePlan.getUser();
        TravelPlan viewerPlan = viewerIsA ? match.getPlanA() : match.getPlanB();

        return SmartTripMatchResponse.builder()
                .id(match.getId())
                .score(match.getScore())
                .matchLevel(matchLevel(match.getScore()))
                .reasons(buildReasons(viewerPlan, candidatePlan, viewer, candidateUser))
                .matchedPlanId(candidatePlan.getId())
                .matchedUserId(candidateUser.getId())
                .matchedUserName(candidateUser.getName())
                .matchedUserCity(candidateUser.getCity())
                .profilePhotoUrl("/api/users/" + candidateUser.getId() + "/photo")
                .fromLocation(candidatePlan.getFromLocation())
                .destination(candidatePlan.getDestination())
                .startDate(candidatePlan.getStartDate())
                .endDate(candidatePlan.getEndDate())
                .budget(candidatePlan.getBudget())
                .travelType(candidatePlan.getTravelType())
                .groupType(candidatePlan.getGroupType())
                .openForJoining(Boolean.TRUE.equals(candidatePlan.getOpenForJoining()))
                .viewed(viewerIsA ? match.getViewedByA() : match.getViewedByB())
                .createdAt(match.getCreatedAt())
                .build();
    }

    private List<String> buildReasons(
            TravelPlan viewerPlan,
            TravelPlan candidatePlan,
            User viewer,
            User candidateUser) {

        List<String> reasons = new ArrayList<>();

        if (same(viewerPlan.getDestination(), candidatePlan.getDestination())) {
            reasons.add("Same destination");
        }

        long overlapDays = overlapDays(viewerPlan, candidatePlan);
        if (overlapDays > 0) {
            reasons.add("Dates overlap by " + overlapDays + (overlapDays == 1 ? " day" : " days"));
        }

        if (same(viewerPlan.getFromLocation(), candidatePlan.getFromLocation())) {
            reasons.add("Same starting location");
        }

        if (viewerPlan.getBudget() != null && candidatePlan.getBudget() != null
                && viewerPlan.getBudget() > 0 && candidatePlan.getBudget() > 0) {
            double ratio = Math.abs(viewerPlan.getBudget() - candidatePlan.getBudget())
                    / Math.max(viewerPlan.getBudget(), candidatePlan.getBudget());
            if (ratio <= 0.25) {
                reasons.add("Similar budget");
            }
        }

        if (same(viewerPlan.getTravelType(), candidatePlan.getTravelType())) {
            reasons.add("Same travel type");
        }

        Set<String> sharedInterests = intersection(viewer.getTravelInterests(), candidateUser.getTravelInterests());
        if (!sharedInterests.isEmpty()) {
            reasons.add("Shared interests: " + sharedInterests.stream().limit(2).collect(Collectors.joining(", ")));
        }

        Set<String> sharedStyle = intersection(viewer.getTravelStyle(), candidateUser.getTravelStyle());
        if (!sharedStyle.isEmpty()) {
            reasons.add("Similar travel style");
        }

        if (reasons.isEmpty()) {
            reasons.add("Compatible travel preferences");
        }

        return reasons.stream().limit(5).toList();
    }

    private int averageScore(Integer a, Integer b) {
        if (a == null) return Math.max(0, Math.min(100, b));
        if (b == null) return Math.max(0, Math.min(100, a));
        return Math.max(0, Math.min(100, (int) Math.round((a + b) / 2.0)));
    }

    private String matchLevel(Integer score) {
        if (score == null) return "Potential Match";
        if (score >= 90) return "Excellent Match";
        if (score >= 75) return "Strong Match";
        if (score >= 60) return "Good Match";
        return "Potential Match";
    }

    private long overlapDays(TravelPlan a, TravelPlan b) {
        if (a.getStartDate() == null || a.getEndDate() == null
                || b.getStartDate() == null || b.getEndDate() == null) return 0;
        LocalDate start = a.getStartDate().isAfter(b.getStartDate()) ? a.getStartDate() : b.getStartDate();
        LocalDate end = a.getEndDate().isBefore(b.getEndDate()) ? a.getEndDate() : b.getEndDate();
        return Math.max(0, end.toEpochDay() - start.toEpochDay());
    }

    private Set<String> intersection(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return Set.of();
        Set<String> left = Arrays.stream(a.split(","))
                .map(String::trim).map(String::toLowerCase).filter(s -> !s.isBlank()).collect(Collectors.toSet());
        Set<String> right = Arrays.stream(b.split(","))
                .map(String::trim).map(String::toLowerCase).filter(s -> !s.isBlank()).collect(Collectors.toSet());
        left.retainAll(right);
        return left;
    }

    private boolean same(String a, String b) {
        return a != null && b != null && !a.isBlank() && !b.isBlank() && a.trim().equalsIgnoreCase(b.trim());
    }

    private boolean isEligible(TravelPlan plan) {
        return plan != null
                && plan.getId() != null
                && plan.getUser() != null
                && "ACTIVE".equalsIgnoreCase(plan.getStatus())
                && plan.getStartDate() != null
                && plan.getEndDate() != null
                && !plan.getEndDate().isBefore(LocalDate.now())
                && !Boolean.FALSE.equals(
                trustVerificationRepository.findByUserId(plan.getUser().getId())
                        .map(t -> t.isProfileDiscoverable())
                        .orElse(true));
    }

    private boolean hasUpcomingPlansForBothUsers(SmartTripMatch match) {
        return isEligible(match.getPlanA()) && isEligible(match.getPlanB());
    }

    private SmartTripMatch getOwnedMatch(Long matchId, Long userId) {
        SmartTripMatch match = smartTripMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Smart match not found"));
        if (!match.getPlanA().getUser().getId().equals(userId)
                && !match.getPlanB().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this smart match");
        }
        return match;
    }

    private void setViewed(SmartTripMatch match, Long userId, boolean value) {
        if (match.getPlanA().getUser().getId().equals(userId)) match.setViewedByA(value);
        else match.setViewedByB(value);
    }

    private void setDismissed(SmartTripMatch match, Long userId, boolean value) {
        if (match.getPlanA().getUser().getId().equals(userId)) match.setDismissedByA(value);
        else match.setDismissedByB(value);
    }

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
