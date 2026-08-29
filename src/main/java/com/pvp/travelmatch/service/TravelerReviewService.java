package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TravelerReviewRequest;
import com.pvp.travelmatch.dto.TravelerReviewResponse;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.TravelerReview;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.TravelPartnerRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.TravelerReviewRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TravelerReviewService {

    private static final Set<String> ALLOWED_TAGS =
            Set.of(
                    "FRIENDLY",
                    "RELIABLE",
                    "GOOD_PLANNER",
                    "PUNCTUAL",
                    "FUN_TRAVELER",
                    "RESPECTFUL",
                    "EASY_TO_TRAVEL_WITH"
            );

    private final TravelerReviewRepository reviewRepository;

    private final UserRepository userRepository;

    private final TravelPlanRepository travelPlanRepository;

    private final TravelPartnerRepository travelPartnerRepository;

    private final NotificationService notificationService;

    private User getCurrentUser() {

        String email =
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));
    }

    @Transactional
    public TravelerReviewResponse create(
            TravelerReviewRequest request) {

        User reviewer = getCurrentUser();

        if (request == null ||
                request.getReviewedUserId() == null ||
                request.getTravelPlanId() == null ||
                request.getRating() == null) {

            throw new RuntimeException(
                    "Reviewed user, trip and rating are required"
            );
        }

        User reviewedUser =
                userRepository.findById(
                                request.getReviewedUserId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Traveler not found"
                                ));

        TravelPlan travelPlan =
                travelPlanRepository.findById(
                                request.getTravelPlanId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Travel plan not found"
                                ));

        if (reviewer.getId()
                .equals(reviewedUser.getId())) {

            throw new RuntimeException(
                    "You cannot review yourself"
            );
        }

        if (!"COMPLETED".equalsIgnoreCase(
                travelPlan.getStatus())) {

            throw new RuntimeException(
                    "Reviews are available only after a completed trip"
            );
        }

        if (travelPlan.getEndDate() == null ||
                travelPlan.getEndDate()
                        .isAfter(LocalDate.now())) {

            throw new RuntimeException(
                    "This trip has not finished yet"
            );
        }

        if (!travelPartnerRepository.arePartners(
                reviewer,
                reviewedUser
        )) {

            throw new RuntimeException(
                    "You can only review a confirmed travel partner"
            );
        }

        if (!travelPlan.getUser()
                .getId()
                .equals(reviewer.getId())
                &&
                !travelPlan.getUser()
                        .getId()
                        .equals(reviewedUser.getId())) {

            throw new RuntimeException(
                    "This trip is not associated with this match"
            );
        }

        if (reviewRepository
                .existsByReviewerIdAndReviewedUserIdAndTravelPlanId(
                        reviewer.getId(),
                        reviewedUser.getId(),
                        travelPlan.getId()
                )) {

            throw new RuntimeException(
                    "You have already reviewed this traveler for this trip"
            );
        }

        List<String> tags =
                request.getTags() == null
                        ? List.of()
                        : request.getTags()
                        .stream()
                        .filter(t ->
                                t != null &&
                                        !t.isBlank()
                        )
                        .map(t ->
                                t.trim()
                                        .toUpperCase()
                                        .replace(' ', '_')
                        )
                        .distinct()
                        .toList();

        if (tags.stream()
                .anyMatch(
                        tag -> !ALLOWED_TAGS.contains(tag)
                )) {

            throw new RuntimeException(
                    "Invalid review tag"
            );
        }

        TravelerReview review =
                TravelerReview.builder()
                        .reviewer(reviewer)
                        .reviewedUser(reviewedUser)
                        .travelPlan(travelPlan)
                        .rating(request.getRating())
                        .tags(tags)
                        .comment(
                                request.getComment() == null ||
                                        request.getComment().isBlank()
                                        ? null
                                        : request.getComment().trim()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        TravelerReview saved =
                reviewRepository.save(review);

        notificationService.createNotification(
                reviewedUser,
                reviewer,
                "⭐ " + reviewer.getName()
                        + " left you a travel review.",
                com.pvp.travelmatch.entity.NotificationType.REVIEW_RECEIVED,
                saved.getId()
        );

        return TravelerReviewResponse
                .fromEntity(saved);
    }

    /**
     * Returns completed trips on which the authenticated user and the
     * reviewed traveler are confirmed partners and for which a review has
     * not already been submitted by the authenticated user.
     */
    public List<Long> getEligibleTravelPlanIds(Long reviewedUserId) {
        User reviewer = getCurrentUser();

        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));

        if (reviewer.getId().equals(reviewedUser.getId())) {
            return List.of();
        }

        return travelPartnerRepository
                .findByUserOneOrUserTwo(reviewer, reviewer)
                .stream()
                .filter(partner -> {
                    User otherUser = partner.getUserOne().getId().equals(reviewer.getId())
                            ? partner.getUserTwo()
                            : partner.getUserOne();

                    return otherUser.getId().equals(reviewedUser.getId());
                })
                .map(com.pvp.travelmatch.entity.TravelPartner::getTravelPlan)
                .filter(java.util.Objects::nonNull)
                .filter(plan -> "COMPLETED".equalsIgnoreCase(plan.getStatus()))
                .filter(plan -> plan.getEndDate() != null && !plan.getEndDate().isAfter(LocalDate.now()))
                .map(TravelPlan::getId)
                .filter(planId -> !reviewRepository
                        .existsByReviewerIdAndReviewedUserIdAndTravelPlanId(
                                reviewer.getId(),
                                reviewedUser.getId(),
                                planId
                        ))
                .distinct()
                .toList();
    }

    public List<TravelerReviewResponse>
    getForUser(Long userId) {

        return reviewRepository
                .findByReviewedUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(
                        TravelerReviewResponse::fromEntity
                )
                .toList();
    }

    public double getAverage(Long userId) {

        Double average =
                reviewRepository.averageRating(userId);

        if (average == null) {
            return 0.0;
        }

        return Math.round(
                average * 10.0
        ) / 10.0;
    }

    public long getCount(Long userId) {

        return reviewRepository
                .countByReviewedUserId(userId);
    }
}