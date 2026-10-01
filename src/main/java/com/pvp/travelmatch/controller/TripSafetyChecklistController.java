package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TripSafetyChecklistRequest;
import com.pvp.travelmatch.dto.TripSafetyChecklistResponse;
import com.pvp.travelmatch.service.TripSafetyChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/travel/{planId}/safety-checklist") @RequiredArgsConstructor
public class TripSafetyChecklistController {
    private final TripSafetyChecklistService service;

    @GetMapping
    public TripSafetyChecklistResponse get(@PathVariable Long planId) { return service.get(planId); }

    @PutMapping
    public TripSafetyChecklistResponse update(@PathVariable Long planId, @Valid @RequestBody TripSafetyChecklistRequest request) {
        return service.update(planId, request);
    }
}
