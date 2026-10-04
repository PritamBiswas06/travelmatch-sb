package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.SmartTripMatchResponse;
import com.pvp.travelmatch.service.SmartMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart-matches")
@RequiredArgsConstructor
public class SmartMatchController {

    private final SmartMatchService smartMatchService;

    @GetMapping
    public List<SmartTripMatchResponse> getMyMatches() {
        return smartMatchService.getMyMatches();
    }

    @PutMapping("/{id}/viewed")
    public SmartTripMatchResponse markViewed(@PathVariable Long id) {
        return smartMatchService.markViewed(id);
    }

    @PutMapping("/{id}/dismiss")
    public Map<String, String> dismiss(@PathVariable Long id) {
        smartMatchService.dismiss(id);
        return Map.of("message", "Smart match dismissed");
    }
}
