package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.FeedFilterRequest;
import com.pvp.travelmatch.dto.FeedPostResponse;
import com.pvp.travelmatch.dto.MatchResponse;
import com.pvp.travelmatch.dto.TravelPlanRequest;
import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.PostReaction;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.*;
import com.pvp.travelmatch.specification.TravelPlanSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PostReactionRepository postReactionRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final TravelPartnerRepository travelPartnerRepository;
    private final NotificationService notificationService;
    private final CompatibilityService compatibilityService;
    private final SavedTravelPlanRepository savedTravelPlanRepository;
    private final TravelCommentRepository travelCommentRepository;
    private final TravelMemoryRepository travelMemoryRepository;

    @Value("${app.frontend-url:https://tripmatch.fun}")
    private String frontendUrl;


    public TravelPlan createPlan(TravelPlanRequest request) {

        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TravelPlan plan = TravelPlan.builder()
                .fromLocation(request.getFromLocation())
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .budget(request.getBudget())
                .travelType(request.getTravelType())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        TravelPlan savedPlan = travelPlanRepository.save(plan);

        String dashboardLink = frontendUrl + "/dashboard";

        String htmlEmail = """
<html>
<body style="font-family:Arial;background:#f4f6fb;padding:30px;">

<div style="max-width:600px;margin:auto;background:white;border-radius:12px;
box-shadow:0 10px 40px rgba(0,0,0,0.1);overflow:hidden;">

<div style="background:#0d78e3;color:white;padding:20px;text-align:center;font-size:22px;">
✈ TravelMatch
</div>

<div style="padding:30px;text-align:center;">

<h2>Your Trip is Live 🌍</h2>

<p>Hello <b>%s</b>,</p>

<p>Your travel plan has been successfully created!</p>

<div style="margin:25px 0;padding:20px;background:#f7f9ff;border-radius:8px;text-align:left;">

<p><b>From:</b> %s</p>
<p><b>Destination:</b> %s</p>
<p><b>Start Date:</b> %s</p>
<p><b>End Date:</b> %s</p>
<p><b>Budget:</b> ₹ %s</p>
<p><b>Travel Style:</b> %s</p>

</div>

<p>We are now showing your trip to compatible travelers.</p>

<a href="%s"
style="display:inline-block;margin-top:20px;padding:14px 28px;
background:#ff5a3d;color:white;text-decoration:none;
border-radius:6px;font-weight:bold;">
View Dashboard
</a>

<p style="margin-top:30px;font-size:13px;color:#888;">
Keep an eye on your inbox for match requests 👀
</p>

</div>

</div>

</body>
</html>
""".formatted(
                user.getName(),
                plan.getFromLocation(),
                plan.getDestination(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getBudget(),
                plan.getTravelType(),
                dashboardLink
        );

        emailService.sendHtmlEmail(
                user.getEmail(),
                "Trip Confirmed: " + plan.getDestination() + " ✈",
                htmlEmail
        );

        return savedPlan;
    }

    public List<MatchResponse> findMatches(Long planId) {

        TravelPlan myPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        List<TravelPlan> candidates = travelPlanRepository.findMatchingPlans(
                myPlan.getDestination(),
                myPlan.getStartDate(),
                myPlan.getEndDate(),
                myPlan.getUser().getId()
        );

        return candidates.stream()
                .map(plan -> {

                    int score = 0;

                    // 1️⃣ Destination match
                    score += 40;

                    // 2️⃣ Date overlap %
                    long totalDays = myPlan.getStartDate().until(myPlan.getEndDate()).getDays();
                    long overlapStart =
                            plan.getStartDate().isAfter(myPlan.getStartDate())
                                    ? plan.getStartDate().toEpochDay()
                                    : myPlan.getStartDate().toEpochDay();

                    long overlapEnd =
                            plan.getEndDate().isBefore(myPlan.getEndDate())
                                    ? plan.getEndDate().toEpochDay()
                                    : myPlan.getEndDate().toEpochDay();

                    long overlapDays = overlapEnd - overlapStart;

                    if (overlapDays > 0 && totalDays > 0) {
                        double overlapPercent = (double) overlapDays / totalDays;
                        score += (int) (overlapPercent * 30);
                    }

                    // 3️⃣ Budget similarity
                    double budgetDiff = Math.abs(myPlan.getBudget() - plan.getBudget());
                    double budgetPercent = 1 - (budgetDiff / myPlan.getBudget());
                    score += (int) (budgetPercent * 20);

                    // 4️⃣ Travel type
                    if (myPlan.getTravelType().equalsIgnoreCase(plan.getTravelType())) {
                        score += 10;
                    }

                    return new MatchResponse(plan, score);
                })
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .toList();
    }


    public List<TravelPlan> getMyPlans() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return travelPlanRepository.findByUser(user);
    }

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ==================== FEED ====================

    public List<FeedPostResponse> getFeed(String sortBy, FeedFilterRequest filter) {

        User currentUser = getCurrentUser();

        // All destination/location/budget/date/travelType filters are applied
        // at the database level via a Specification, so no unfiltered
        // over-fetching happens regardless of how many filters are active.
        Specification<TravelPlan> spec =
                TravelPlanSpecifications.feedFilters(currentUser.getId(), LocalDate.now(), filter);

        List<TravelPlan> feedPlans =
                travelPlanRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Used to compute an optional match score against the viewer's own trips
        List<TravelPlan> myPlans = travelPlanRepository.findByUser(currentUser);
        TravelPlan myLatestPlan = myPlans.stream()
                .max(Comparator.comparing(TravelPlan::getCreatedAt))
                .orElse(null);

        List<FeedPostResponse> posts = feedPlans.stream()
                .map(plan -> toFeedPostResponse(plan, currentUser, myLatestPlan))
                .collect(java.util.stream.Collectors.toList());

        // Match score isn't a DB column (it's computed per-viewer), so the
        // "minimum match %" filter is applied here, after scoring.
        if (filter != null && filter.getMinMatchScore() != null) {
            int minScore = filter.getMinMatchScore();
            posts = posts.stream()
                    .filter(p -> p.getMatchScore() != null && p.getMatchScore() >= minScore)
                    .collect(java.util.stream.Collectors.toList());
        }

        if ("popular".equalsIgnoreCase(sortBy)) {
            posts.sort(Comparator.comparingLong(FeedPostResponse::getLikeCount).reversed());
        } else if ("match".equalsIgnoreCase(sortBy)) {
            posts.sort(Comparator.comparing(
                    (FeedPostResponse p) -> p.getMatchScore() == null ? -1 : p.getMatchScore()
            ).reversed());
        }
        // "latest" (default) is already the natural order from the query (createdAt DESC)

        return posts;
    }

    private String toPhotoDataUri(User user) {

        if (user == null ||
                user.getProfilePhoto() == null ||
                user.getProfilePhoto().length == 0 ||
                user.getProfilePhotoContentType() == null) {

            return null;
        }

        String base64 =
                java.util.Base64
                        .getEncoder()
                        .encodeToString(user.getProfilePhoto());

        return "data:" +
                user.getProfilePhotoContentType() +
                ";base64," +
                base64;
    }
    private FeedPostResponse toFeedPostResponse(
            TravelPlan plan,
            User currentUser,
            TravelPlan myLatestPlan) {

        long likeCount =
                postReactionRepository.countByTravelPlanAndReactionType(
                        plan,
                        "LIKE"
                );

        long dislikeCount =
                postReactionRepository.countByTravelPlanAndReactionType(
                        plan,
                        "DISLIKE"
                );

        String myReaction =
                postReactionRepository
                        .findByTravelPlanAndUser(plan, currentUser)
                        .map(PostReaction::getReactionType)
                        .orElse(null);

        String matchRequestStatus =
                matchRequestRepository
                        .findBySenderIdAndTravelPlanId(
                                currentUser.getId(),
                                plan.getId()
                        )
                        .map(MatchRequest::getStatus)
                        .orElse("NONE");

        CompatibilityService.CompatibilityResult compatibility =
                compatibilityService.calculate(
                        currentUser,
                        myLatestPlan,
                        plan.getUser(),
                        plan
                );

        User postOwner = plan.getUser();

        return FeedPostResponse.builder()
                .id(plan.getId())

                .userId(postOwner.getId())
                .userName(postOwner.getName())
                .userCity(postOwner.getCity())

                // NEW PROFILE INFORMATION
                .userGender(postOwner.getGender())
                .profilePhotoUrl(toPhotoDataUri(postOwner))

                .fromLocation(plan.getFromLocation())
                .destination(plan.getDestination())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .budget(plan.getBudget())
                .travelType(plan.getTravelType())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())

                .matchScore(compatibility.score())
                .matchFactors(compatibility.factors())

                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .shareCount(plan.getShareCount())

                .currentUserReaction(myReaction)
                .currentUserSaved(savedTravelPlanRepository.existsByUserIdAndTravelPlanId(currentUser.getId(), plan.getId()))
                .commentCount(travelCommentRepository.countByTravelPlanId(plan.getId()))
                .matchRequestStatus(matchRequestStatus)

                .build();
    }
//    private FeedPostResponse toFeedPostResponse(TravelPlan plan, User currentUser, TravelPlan myLatestPlan) {
//
//        long likeCount = postReactionRepository.countByTravelPlanAndReactionType(plan, "LIKE");
//        long dislikeCount = postReactionRepository.countByTravelPlanAndReactionType(plan, "DISLIKE");
//
//        String myReaction = postReactionRepository.findByTravelPlanAndUser(plan, currentUser)
//                .map(PostReaction::getReactionType)
//                .orElse(null);
//
//        String matchRequestStatus = matchRequestRepository
//                .findBySenderIdAndTravelPlanId(currentUser.getId(), plan.getId())
//                .map(MatchRequest::getStatus)
//                .orElse("NONE");
//
//        CompatibilityService.CompatibilityResult compatibility = compatibilityService.calculate(
//                currentUser, myLatestPlan, plan.getUser(), plan
//        );
//
//        return FeedPostResponse.builder()
//                .id(plan.getId())
//                .userId(plan.getUser().getId())
//                .userName(plan.getUser().getName())
//                .userCity(plan.getUser().getCity())
//                .fromLocation(plan.getFromLocation())
//                .destination(plan.getDestination())
//                .startDate(plan.getStartDate())
//                .endDate(plan.getEndDate())
//                .budget(plan.getBudget())
//                .travelType(plan.getTravelType())
//                .status(plan.getStatus())
//                .createdAt(plan.getCreatedAt())
//                .matchScore(compatibility.score())
//                .matchFactors(compatibility.factors())
//                .likeCount(likeCount)
//                .dislikeCount(dislikeCount)
//                .shareCount(plan.getShareCount())
//                .currentUserReaction(myReaction)
//                .matchRequestStatus(matchRequestStatus)
//                .build();
//    }

    // ==================== REACTIONS ====================

    public FeedPostResponse likePost(Long planId) {
        return setReaction(planId, "LIKE");
    }

    public FeedPostResponse dislikePost(Long planId) {
        return setReaction(planId, "DISLIKE");
    }

    private FeedPostResponse setReaction(Long planId, String reactionType) {

        User currentUser = getCurrentUser();

        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Travel plan not found"));

        Optional<PostReaction> existing =
                postReactionRepository.findByTravelPlanAndUser(plan, currentUser);

        // Captured BEFORE mutation: was the user's reaction already LIKE?
        // Used below to notify only when the final reaction actually
        // BECOMES LIKE (fresh like, or switching DISLIKE -> LIKE) - never
        // on unlike (toggle-off), dislike, or switching LIKE -> DISLIKE.
        boolean wasAlreadyLiked = existing.isPresent()
                && "LIKE".equals(existing.get().getReactionType());

        if (existing.isPresent()) {

            PostReaction reaction = existing.get();

            if (reaction.getReactionType().equals(reactionType)) {
                // Same reaction tapped again -> remove it (toggle off)
                postReactionRepository.delete(reaction);
            } else {
                // Switching between LIKE and DISLIKE
                reaction.setReactionType(reactionType);
                reaction.setCreatedAt(LocalDateTime.now());
                postReactionRepository.save(reaction);
            }

        } else {

            PostReaction reaction = PostReaction.builder()
                    .travelPlan(plan)
                    .user(currentUser)
                    .reactionType(reactionType)
                    .createdAt(LocalDateTime.now())
                    .build();

            postReactionRepository.save(reaction);
        }

        // 🔔 Notify the post owner only when this action's final result is
        // a LIKE - not on dislike, unlike, or LIKE -> DISLIKE switches.
        // Wrapped so a notification failure can never break liking/disliking.
        boolean becameLike = "LIKE".equals(reactionType) && !wasAlreadyLiked;

        if (becameLike) {
            try {
                notificationService.createPostLikeNotification(plan.getUser(), currentUser, plan.getId());
            } catch (Exception e) {
                // Deliberately swallow: reacting to a post must always
                // succeed even if the notification side-effect fails.
            }
        }

        List<TravelPlan> myPlans = travelPlanRepository.findByUser(currentUser);
        TravelPlan myLatestPlan = myPlans.stream()
                .max(Comparator.comparing(TravelPlan::getCreatedAt))
                .orElse(null);

        return toFeedPostResponse(plan, currentUser, myLatestPlan);
    }

    // ==================== SHARE ====================

    public FeedPostResponse sharePost(Long planId) {

        User currentUser = getCurrentUser();

        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Travel plan not found"));

        plan.setShareCount((plan.getShareCount() == null ? 0 : plan.getShareCount()) + 1);
        travelPlanRepository.save(plan);

        List<TravelPlan> myPlans = travelPlanRepository.findByUser(currentUser);
        TravelPlan myLatestPlan = myPlans.stream()
                .max(Comparator.comparing(TravelPlan::getCreatedAt))
                .orElse(null);

        return toFeedPostResponse(plan, currentUser, myLatestPlan);
    }

    @Transactional
    public void deletePlan(Long planId) {

        User currentUser = getCurrentUser();

        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Travel plan not found"));

        if (!plan.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete your own travel plans"
            );
        }

        postReactionRepository.deleteByTravelPlan(plan);
        matchRequestRepository.deleteByTravelPlan(plan);
        travelPartnerRepository.deleteByTravelPlan(plan);
        travelCommentRepository.deleteByTravelPlan(plan);
        travelMemoryRepository.deleteByTravelPlan(plan);
        savedTravelPlanRepository.deleteByTravelPlan(plan);

        travelPlanRepository.delete(plan);
    }
}