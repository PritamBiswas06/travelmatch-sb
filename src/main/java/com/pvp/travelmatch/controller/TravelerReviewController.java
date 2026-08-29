package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TravelerReviewRequest;
import com.pvp.travelmatch.dto.TravelerReviewResponse;
import com.pvp.travelmatch.service.TravelerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class TravelerReviewController {

    private final TravelerReviewService reviewService;

    @PostMapping
    public TravelerReviewResponse create(
            @Valid @RequestBody
            TravelerReviewRequest request) {

        return reviewService.create(request);
    }

    @GetMapping("/user/{userId}")
    public List<TravelerReviewResponse>
    getReviews(
            @PathVariable Long userId) {

        return reviewService
                .getForUser(userId);
    }

    @GetMapping("/user/{userId}/summary")
    public Map<String, Object> summary(
            @PathVariable Long userId) {

        return Map.of(
                "averageRating",
                reviewService.getAverage(userId),

                "reviewCount",
                reviewService.getCount(userId)
        );
    }
}