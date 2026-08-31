package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TravelCommentRequest;
import com.pvp.travelmatch.dto.TravelCommentResponse;
import com.pvp.travelmatch.service.TravelCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel-plans")
@RequiredArgsConstructor
public class TravelCommentController {
    private final TravelCommentService service;

    @GetMapping("/{travelPlanId}/comments")
    public List<TravelCommentResponse> getComments(@PathVariable Long travelPlanId) {
        return service.getForPlan(travelPlanId);
    }

    @PostMapping("/{travelPlanId}/comments")
    public TravelCommentResponse addComment(
            @PathVariable Long travelPlanId,
            @RequestBody TravelCommentRequest request) {
        return service.create(travelPlanId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    public Map<String, String> deleteComment(@PathVariable Long commentId) {
        service.delete(commentId);
        return Map.of("message", "Comment deleted");
    }
}
