package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchRequestController {

    private final MatchRequestService matchRequestService;

    // Send request
    @PostMapping("/send/{travelPlanId}")
    public MatchRequest sendRequest(@PathVariable Long travelPlanId) {
        return matchRequestService.sendRequest(travelPlanId);
    }

    // Accept request
    @PutMapping("/{requestId}/accept")
    public MatchRequest accept(@PathVariable Long requestId) {
        return matchRequestService.updateStatus(requestId, "ACCEPTED");
    }

    // Reject request
    @PutMapping("/{requestId}/reject")
    public MatchRequest reject(@PathVariable Long requestId) {
        return matchRequestService.updateStatus(requestId, "REJECTED");
    }

    // View my incoming requests
    @GetMapping("/my")
    public List<MatchRequest> myRequests() {
        return matchRequestService.getMyRequests();
    }

    @GetMapping("/api/match/{planId}")
    public List<?> getMatches(@PathVariable Long planId) {
        return matchRequestService.findMatches(planId);
    }

}