package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.CompatibilityFactorResponse;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Deterministic, explainable Travel Compatibility Score (0-100).
//
// Each factor below has a fixed maximum weight. A factor is only scored
// when BOTH sides have the relevant data (e.g. destination is only scored
// if the viewer has an active trip AND the candidate trip has one). The
// final score is the percentage of *evaluated* weight actually earned, so
// the score stays meaningful even when the viewer has no active trip of
// their own (in that case it's based purely on shared profile info)
// instead of just being null.
//
// This is intentionally not an external/ML "AI" call: it's a rules-based
// engine built from the profile/trip fields that already exist, structured
// so a real model could later replace individual factor calculations
// without touching the rest of the pipeline.
@Service
public class CompatibilityService {

    private record Factor(String label, int weight, Integer earned) {
        boolean wasEvaluated() {
            return earned != null;
        }
    }

    public CompatibilityResult calculate(User viewer, TravelPlan viewerPlan, User candidateUser, TravelPlan candidatePlan) {

        List<Factor> factors = new ArrayList<>();

        factors.add(destinationFactor(viewerPlan, candidatePlan));
        factors.add(dateOverlapFactor(viewerPlan, candidatePlan));
        factors.add(budgetFactor(viewerPlan, candidatePlan));
        factors.add(travelTypeFactor(viewerPlan, candidatePlan));
        factors.add(overlapFactor("Travel Interests", 10, viewer.getTravelInterests(), candidateUser.getTravelInterests()));
        factors.add(overlapFactor("Travel Style", 10, viewer.getTravelStyle(), candidateUser.getTravelStyle()));
        factors.add(overlapFactor("Preferred Destinations", 5, viewer.getPreferredDestinations(), candidateUser.getPreferredDestinations()));
        factors.add(overlapFactor("Languages", 5, viewer.getLanguages(), candidateUser.getLanguages()));
        factors.add(exactMatchFactor("Travel Frequency", 5, viewer.getTravelFrequency(), candidateUser.getTravelFrequency()));

        int possibleWeight = 0;
        int earnedWeight = 0;
        List<CompatibilityFactorResponse> breakdown = new ArrayList<>();

        for (Factor factor : factors) {
            if (!factor.wasEvaluated()) {
                continue;
            }
            possibleWeight += factor.weight();
            earnedWeight += factor.earned();
            breakdown.add(new CompatibilityFactorResponse(factor.label(), ratingFor(factor.earned(), factor.weight())));
        }

        if (possibleWeight == 0) {
            // Nothing comparable at all (no trip on either side, and no
            // overlapping profile info) - same "not computable" behaviour
            // as before, rather than a misleading 0%.
            return new CompatibilityResult(null, List.of());
        }

        int score = (int) Math.round(100.0 * earnedWeight / possibleWeight);
        score = Math.max(0, Math.min(100, score));

        return new CompatibilityResult(score, breakdown);
    }

    // ==================== TRIP-LEVEL FACTORS ====================

    private Factor destinationFactor(TravelPlan viewerPlan, TravelPlan candidatePlan) {
        if (viewerPlan == null || isBlank(viewerPlan.getDestination()) || isBlank(candidatePlan.getDestination())) {
            return new Factor("Destination", 20, null);
        }
        int earned = viewerPlan.getDestination().equalsIgnoreCase(candidatePlan.getDestination()) ? 20 : 0;
        return new Factor("Destination", 20, earned);
    }

    private Factor dateOverlapFactor(TravelPlan viewerPlan, TravelPlan candidatePlan) {
        if (viewerPlan == null
                || viewerPlan.getStartDate() == null || viewerPlan.getEndDate() == null
                || candidatePlan.getStartDate() == null || candidatePlan.getEndDate() == null) {
            return new Factor("Travel Dates", 15, null);
        }

        long totalDays = viewerPlan.getStartDate().until(viewerPlan.getEndDate()).getDays();

        long overlapStart = candidatePlan.getStartDate().isAfter(viewerPlan.getStartDate())
                ? candidatePlan.getStartDate().toEpochDay()
                : viewerPlan.getStartDate().toEpochDay();

        long overlapEnd = candidatePlan.getEndDate().isBefore(viewerPlan.getEndDate())
                ? candidatePlan.getEndDate().toEpochDay()
                : viewerPlan.getEndDate().toEpochDay();

        long overlapDays = overlapEnd - overlapStart;

        int earned = 0;
        if (overlapDays > 0 && totalDays > 0) {
            double overlapPercent = Math.min(1.0, (double) overlapDays / totalDays);
            earned = (int) Math.round(overlapPercent * 15);
        }
        return new Factor("Travel Dates", 15, earned);
    }

    private Factor budgetFactor(TravelPlan viewerPlan, TravelPlan candidatePlan) {
        if (viewerPlan == null
                || viewerPlan.getBudget() == null || candidatePlan.getBudget() == null
                || viewerPlan.getBudget() <= 0) {
            return new Factor("Budget", 15, null);
        }

        double budgetDiff = Math.abs(viewerPlan.getBudget() - candidatePlan.getBudget());
        double budgetPercent = 1 - (budgetDiff / viewerPlan.getBudget());
        int earned = (int) Math.round(Math.max(0, budgetPercent) * 15);
        return new Factor("Budget", 15, earned);
    }

    private Factor travelTypeFactor(TravelPlan viewerPlan, TravelPlan candidatePlan) {
        if (viewerPlan == null || isBlank(viewerPlan.getTravelType()) || isBlank(candidatePlan.getTravelType())) {
            return new Factor("Travel Type", 10, null);
        }
        int earned = viewerPlan.getTravelType().equalsIgnoreCase(candidatePlan.getTravelType()) ? 10 : 0;
        return new Factor("Travel Type", 10, earned);
    }

    // ==================== PROFILE-LEVEL FACTORS ====================

    // Comma-separated multi-select fields (travel style, interests,
    // preferred destinations, languages) are compared by overlap: how much
    // of the combined set of choices the two users share.
    private Factor overlapFactor(String label, int weight, String csvA, String csvB) {
        Set<String> a = toSet(csvA);
        Set<String> b = toSet(csvB);

        if (a.isEmpty() || b.isEmpty()) {
            return new Factor(label, weight, null);
        }

        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        double ratio = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
        int earned = (int) Math.round(ratio * weight);
        return new Factor(label, weight, earned);
    }

    private Factor exactMatchFactor(String label, int weight, String valueA, String valueB) {
        if (isBlank(valueA) || isBlank(valueB)) {
            return new Factor(label, weight, null);
        }
        int earned = valueA.equalsIgnoreCase(valueB) ? weight : 0;
        return new Factor(label, weight, earned);
    }

    // ==================== HELPERS ====================

    private Set<String> toSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String ratingFor(int earned, int weight) {
        if (weight <= 0) {
            return "Fair";
        }
        double pct = (double) earned / weight;
        if (pct >= 0.9) return "Excellent";
        if (pct >= 0.7) return "Very Good";
        if (pct >= 0.5) return "Good";
        if (pct >= 0.25) return "Fair";
        return "Poor";
    }

    // Result holder returned to the caller.
    public record CompatibilityResult(Integer score, List<CompatibilityFactorResponse> factors) {
    }
}