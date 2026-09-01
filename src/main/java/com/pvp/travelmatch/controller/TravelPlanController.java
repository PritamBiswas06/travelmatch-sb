package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.FeedFilterRequest;
import com.pvp.travelmatch.dto.FeedPostResponse;
import com.pvp.travelmatch.dto.MatchResponse;
import com.pvp.travelmatch.dto.TravelPlanRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanService travelPlanService;

    @PostMapping
    public TravelPlan createPlan(@RequestBody TravelPlanRequest request) {
        return travelPlanService.createPlan(request);
    }

    @GetMapping("/{planId}/matches")
    public List<MatchResponse> getMatches(@PathVariable Long planId) {
        return travelPlanService.findMatches(planId);
    }

    @GetMapping("/my")
    public List<TravelPlan> getMyPlans() {
        return travelPlanService.getMyPlans();
    }

    // ==================== FEED ====================

    @GetMapping("/feed")
    public List<FeedPostResponse> getFeed(
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String fromLocation,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String travelType,
            @RequestParam(required = false) Integer minMatchScore,
            @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String travelStyle, @RequestParam(required = false) String travelInterest,
            @RequestParam(required = false) String language, @RequestParam(required = false) String country, @RequestParam(required = false) String city
    ) {
        FeedFilterRequest filter = FeedFilterRequest.builder()
                .destination(destination)
                .fromLocation(fromLocation)
                .minBudget(minBudget)
                .maxBudget(maxBudget)
                .startDate(startDate)
                .endDate(endDate)
                .travelType(travelType)
                .minMatchScore(minMatchScore).minAge(minAge).maxAge(maxAge).travelStyle(travelStyle).travelInterest(travelInterest).language(language).country(country).city(city)
                .build();

        return travelPlanService.getFeed(sort, filter);
    }

    @PostMapping("/{planId}/like")
    public FeedPostResponse likePlan(@PathVariable Long planId) {
        return travelPlanService.likePost(planId);
    }

    @PostMapping("/{planId}/dislike")
    public FeedPostResponse dislikePlan(@PathVariable Long planId) {
        return travelPlanService.dislikePost(planId);
    }

    @PostMapping("/{planId}/share")
    public FeedPostResponse sharePlan(@PathVariable Long planId) {
        return travelPlanService.sharePost(planId);
    }

    @DeleteMapping("/{planId}")
    public void deletePlan(@PathVariable Long planId) {
        travelPlanService.deletePlan(planId);
    }
}