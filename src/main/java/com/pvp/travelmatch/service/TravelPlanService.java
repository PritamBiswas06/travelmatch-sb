package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.FeedFilterRequest;
import com.pvp.travelmatch.dto.FeedPostResponse;
import com.pvp.travelmatch.dto.FeedPageResponse;
import com.pvp.travelmatch.dto.MatchResponse;
import com.pvp.travelmatch.dto.TravelPlanRequest;
import com.pvp.travelmatch.entity.*;
import com.pvp.travelmatch.repository.*;
import com.pvp.travelmatch.specification.TravelPlanSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final MonetizationService monetizationService;
    private final BoostedTravelPlanRepository boostedTravelPlanRepository;
    private final SubscriptionRepository subscriptionRepository;

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

    /**
     * Load one exact travel post for the currently authenticated viewer.
     * Unlike getFeed(), this does not exclude the viewer's own posts.
     */
    public FeedPostResponse getTravelPost(Long planId) {
        User currentUser = getCurrentUser();

        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Travel post not found"
                ));

        TravelPlan myLatestPlan = travelPlanRepository
                .findTopByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .orElse(null);

        return toFeedPostResponse(plan, currentUser, myLatestPlan);
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

    public FeedPageResponse getFeed(String sortBy, FeedFilterRequest filter, int page, int size) {

        User currentUser = getCurrentUser();
        boolean premium = monetizationService.isPremium(currentUser);

        if (filter != null && (filter.getMinAge()!=null || filter.getMaxAge()!=null
                || filter.getTravelStyle()!=null || filter.getTravelInterest()!=null
                || filter.getLanguage()!=null || filter.getCountry()!=null || filter.getCity()!=null)
                && !premium) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Premium subscription required for advanced filters");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);

        Specification<TravelPlan> spec =
                TravelPlanSpecifications.feedFilters(
                        currentUser.getId(), LocalDate.now(), filter);

        Page<TravelPlan> feedPage = travelPlanRepository.findAll(
                spec,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        TravelPlan myLatestPlan = travelPlanRepository
                .findTopByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .orElse(null);

        List<FeedPostResponse> posts =
                toFeedPostResponses(
                        feedPage.getContent(),
                        currentUser,
                        myLatestPlan
                );

        if (filter != null && filter.getMinMatchScore() != null) {
            int minScore = filter.getMinMatchScore();
            posts = posts.stream()
                    .filter(p -> p.getMatchScore() != null
                            && p.getMatchScore() >= minScore)
                    .toList();
        }

        if ("popular".equalsIgnoreCase(sortBy)) {
            posts = posts.stream()
                    .sorted(Comparator.comparingLong(FeedPostResponse::getLikeCount)
                            .reversed()
                            .thenComparing(FeedPostResponse::getCreatedAt,
                                    Comparator.reverseOrder()))
                    .toList();
        } else if ("match".equalsIgnoreCase(sortBy)) {
            posts = posts.stream()
                    .sorted(Comparator.comparing(
                                    (FeedPostResponse p) ->
                                            p.getMatchScore() == null ? -1 : p.getMatchScore()
                            ).reversed()
                            .thenComparing(FeedPostResponse::getCreatedAt,
                                    Comparator.reverseOrder()))
                    .toList();
        }

        return new FeedPageResponse(
                posts,
                safePage,
                safeSize,
                feedPage.hasNext()
        );
    }

    /**
     * Builds a feed page without issuing per-post count/existence queries.
     * All viewer-specific state for the page is fetched in batches.
     */
    private List<FeedPostResponse> toFeedPostResponses(
            List<TravelPlan> plans,
            User currentUser,
            TravelPlan myLatestPlan) {

        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream()
                .map(TravelPlan::getId)
                .toList();

        Map<Long, Long> likes = new HashMap<>();
        Map<Long, Long> dislikes = new HashMap<>();
        for (Object[] row : postReactionRepository.countByPlanIdsGrouped(planIds)) {
            Long planId = ((Number) row[0]).longValue();
            String type = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();
            if ("LIKE".equals(type)) {
                likes.put(planId, count);
            } else if ("DISLIKE".equals(type)) {
                dislikes.put(planId, count);
            }
        }

        Map<Long, String> reactions = new HashMap<>();
        for (PostReaction reaction :
                postReactionRepository.findByUserAndTravelPlanIn(
                        currentUser, plans)) {
            reactions.put(
                    reaction.getTravelPlan().getId(),
                    reaction.getReactionType()
            );
        }

        Set<Long> savedIds = new HashSet<>(
                savedTravelPlanRepository.findSavedPlanIds(
                        currentUser.getId(), planIds));

        Map<Long, Long> comments = new HashMap<>();
        for (Object[] row : travelCommentRepository.countByPlanIds(planIds)) {
            comments.put(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue()
            );
        }

        Map<Long, String> requestStatuses = new HashMap<>();
        for (MatchRequest request :
                matchRequestRepository.findBySenderIdAndTravelPlanIds(
                        currentUser.getId(), planIds)) {
            requestStatuses.put(
                    request.getTravelPlan().getId(),
                    request.getStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, BoostedTravelPlan> boosts = boostedTravelPlanRepository
                .findActiveForPlans(planIds, now)
                .stream()
                .collect(Collectors.toMap(
                        b -> b.getTravelPlan().getId(),
                        b -> b,
                        (a, b) -> a
                ));

        Set<Long> premiumUserIds = new HashSet<>();
        List<Long> ownerIds = plans.stream()
                .map(plan -> plan.getUser().getId())
                .distinct()
                .toList();

        // A match is a relationship between users. Fetch all matched owners
        // for this page in one query so both users see their corresponding
        // feed post as matched without introducing an N+1 partner lookup.
        Set<Long> partnerOwnerIds = new HashSet<>();
        if (!ownerIds.isEmpty()) {
            partnerOwnerIds.addAll(
                    travelPartnerRepository.findPartnerUserIds(
                            currentUser.getId(), ownerIds));
        }

        // Subscription status is fetched once for the whole page instead of
        // one database call per post owner.
        subscriptionsForFeed(ownerIds).forEach(premiumUserIds::add);

        return plans.stream()
                .map(plan -> {
                    User owner = plan.getUser();
                    CompatibilityService.CompatibilityResult compatibility =
                            compatibilityService.calculate(
                                    currentUser, myLatestPlan, owner, plan);

                    BoostedTravelPlan boost = boosts.get(plan.getId());

                    return FeedPostResponse.builder()
                            .id(plan.getId())
                            .userId(owner.getId())
                            .userName(owner.getName())
                            .userCity(owner.getCity())
                            .userGender(owner.getGender())
                            .profilePhotoUrl(toPhotoDataUri(owner))
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
                            .likeCount(likes.getOrDefault(plan.getId(), 0L))
                            .dislikeCount(dislikes.getOrDefault(plan.getId(), 0L))
                            .shareCount(plan.getShareCount())
                            .currentUserReaction(reactions.get(plan.getId()))
                            .currentUserSaved(savedIds.contains(plan.getId()))
                            .commentCount(comments.getOrDefault(plan.getId(), 0L))
                            .matchRequestStatus(
                                    partnerOwnerIds.contains(owner.getId())
                                            ? "FRIENDS"
                                            : requestStatuses.getOrDefault(
                                            plan.getId(), "NONE"))
                            .premiumUser(premiumUserIds.contains(owner.getId()))
                            .boosted(boost != null)
                            .boostMultiplier(
                                    boost == null ? null : boost.getMultiplier())
                            .build();
                })
                .toList();
    }

    private Set<Long> subscriptionsForFeed(List<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Set.of();
        }

        LocalDateTime now = LocalDateTime.now();

        return subscriptionsRepository()
                .findByUserIdIn(ownerIds)
                .stream()
                .filter(subscription ->
                        subscription.getPlan() == SubscriptionPlan.PREMIUM
                                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                                && subscription.getEndDate() != null
                                && subscription.getEndDate().isAfter(now))
                .map(subscription -> subscription.getUser().getId())
                .collect(Collectors.toSet());
    }

    private SubscriptionRepository subscriptionsRepository() {
        return subscriptionRepository;
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

        // A successful match is a relationship between the two travelers,
        // not only a status on the original sender's request. Therefore the
        // matched state must be reflected when either traveler views the
        // other's feed post. This mirrors the profile-trip behaviour.
        String matchRequestStatus;
        if (travelPartnerRepository.arePartners(currentUser, plan.getUser())) {
            matchRequestStatus = "FRIENDS";
        } else {
            matchRequestStatus =
                    matchRequestRepository
                            .findBySenderIdAndTravelPlanId(
                                    currentUser.getId(),
                                    plan.getId()
                            )
                            .map(MatchRequest::getStatus)
                            .orElse("NONE");
        }

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
                .premiumUser(monetizationService.isPremium(postOwner))
                .boosted(boostedTravelPlanRepository.findActive(plan.getId(), LocalDateTime.now()).isPresent())
                .boostMultiplier(boostedTravelPlanRepository.findActive(plan.getId(), LocalDateTime.now()).map(BoostedTravelPlan::getMultiplier).orElse(null))
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

        TravelPlan myLatestPlan = travelPlanRepository
                .findTopByUserIdOrderByCreatedAtDesc(currentUser.getId())
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

        TravelPlan myLatestPlan = travelPlanRepository
                .findTopByUserIdOrderByCreatedAtDesc(currentUser.getId())
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