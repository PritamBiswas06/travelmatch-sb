package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.SavedTravelPlanResponse;
import com.pvp.travelmatch.service.SavedTravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved-trips")
@RequiredArgsConstructor
public class SavedTravelPlanController {
    private final SavedTravelPlanService service;

    @PostMapping("/{travelPlanId}")
    public SavedTravelPlanResponse save(@PathVariable Long travelPlanId) {
        return service.save(travelPlanId);
    }

    @DeleteMapping("/{travelPlanId}")
    public Map<String, String> unsave(@PathVariable Long travelPlanId) {
        service.unsave(travelPlanId);
        return Map.of("message", "Trip removed from saved trips");
    }

    @GetMapping
    public List<SavedTravelPlanResponse> getMine() {
        return service.getMine();
    }

    @GetMapping("/check/{travelPlanId}")
    public Map<String, Boolean> check(@PathVariable Long travelPlanId) {
        return Map.of("saved", service.isSaved(travelPlanId));
    }
}
