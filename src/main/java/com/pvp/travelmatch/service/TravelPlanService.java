package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.FeedPostResponse;
import com.pvp.travelmatch.dto.MatchResponse;
import com.pvp.travelmatch.dto.TravelPlanRequest;
import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.PostReaction;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.MatchRequestRepository;
import com.pvp.travelmatch.repository.PostReactionRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

        String dashboardLink = "https://travelmatch49.netlify.app/dashboard";

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

//    public List<TravelPlan> findMatches(Long planId) {
//
//        TravelPlan myPlan = travelPlanRepository.findById(planId)
//                .orElseThrow(() -> new RuntimeException("Plan not found"));
//
//        List<TravelPlan> candidates = travelPlanRepository.findMatchingPlans(
//                myPlan.getDestination(),
//                myPlan.getStartDate(),
//                myPlan.getEndDate(),
//                myPlan.getUser().getId()
//        );
//
//        // 🔥 Budget Filtering (within 30% difference)
//        return candidates.stream()
//                .filter(plan -> {
//                    double myBudget = myPlan.getBudget();
//                    double otherBudget = plan.getBudget();
//
//                    double difference = Math.abs(myBudget - otherBudget);
//                    return difference <= myBudget * 0.3;
//                })
//                .toList();
//    }




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

    public List<FeedPostResponse> getFeed(String sortBy) {

        User currentUser = getCurrentUser();

        List<TravelPlan> feedPlans =
                travelPlanRepository.findFeedPlans(currentUser.getId(), LocalDate.now());

        // Used to compute an optional match score against the viewer's own trips
        List<TravelPlan> myPlans = travelPlanRepository.findByUser(currentUser);
        TravelPlan myLatestPlan = myPlans.stream()
                .max(Comparator.comparing(TravelPlan::getCreatedAt))
                .orElse(null);

        List<FeedPostResponse> posts = feedPlans.stream()
                .map(plan -> toFeedPostResponse(plan, currentUser, myLatestPlan))
                .collect(java.util.stream.Collectors.toList());

        if ("popular".equalsIgnoreCase(sortBy)) {
            posts.sort(Comparator.comparingLong(FeedPostResponse::getLikeCount).reversed());
        } else if ("match".equalsIgnoreCase(sortBy)) {
            posts.sort(Comparator.comparing(
                    (FeedPostResponse p) -> p.getMatchScore() == null ? -1 : p.getMatchScore()
            ).reversed());
        }
        // "latest" (default) is already the natural order from findFeedPlans (createdAt DESC)

        return posts;
    }

    private FeedPostResponse toFeedPostResponse(TravelPlan plan, User currentUser, TravelPlan myLatestPlan) {

        long likeCount = postReactionRepository.countByTravelPlanAndReactionType(plan, "LIKE");
        long dislikeCount = postReactionRepository.countByTravelPlanAndReactionType(plan, "DISLIKE");

        String myReaction = postReactionRepository.findByTravelPlanAndUser(plan, currentUser)
                .map(PostReaction::getReactionType)
                .orElse(null);

        String matchRequestStatus = matchRequestRepository
                .findBySenderIdAndTravelPlanId(currentUser.getId(), plan.getId())
                .map(MatchRequest::getStatus)
                .orElse("NONE");

        Integer matchScore = myLatestPlan == null ? null : computeFeedMatchScore(myLatestPlan, plan);

        return FeedPostResponse.builder()
                .id(plan.getId())
                .userId(plan.getUser().getId())
                .userName(plan.getUser().getName())
                .userCity(plan.getUser().getCity())
                .fromLocation(plan.getFromLocation())
                .destination(plan.getDestination())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .budget(plan.getBudget())
                .travelType(plan.getTravelType())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .matchScore(matchScore)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .shareCount(plan.getShareCount())
                .currentUserReaction(myReaction)
                .matchRequestStatus(matchRequestStatus)
                .build();
    }

    // Generalized version of the scoring logic in findMatches(), safe to use on
    // candidates that may not share the exact same destination.
    private Integer computeFeedMatchScore(TravelPlan myPlan, TravelPlan candidate) {

        int score = 0;

        if (myPlan.getDestination() != null &&
                myPlan.getDestination().equalsIgnoreCase(candidate.getDestination())) {
            score += 40;
        }

        if (myPlan.getStartDate() != null && myPlan.getEndDate() != null &&
                candidate.getStartDate() != null && candidate.getEndDate() != null) {

            long totalDays = myPlan.getStartDate().until(myPlan.getEndDate()).getDays();

            long overlapStart = candidate.getStartDate().isAfter(myPlan.getStartDate())
                    ? candidate.getStartDate().toEpochDay()
                    : myPlan.getStartDate().toEpochDay();

            long overlapEnd = candidate.getEndDate().isBefore(myPlan.getEndDate())
                    ? candidate.getEndDate().toEpochDay()
                    : myPlan.getEndDate().toEpochDay();

            long overlapDays = overlapEnd - overlapStart;

            if (overlapDays > 0 && totalDays > 0) {
                double overlapPercent = (double) overlapDays / totalDays;
                score += (int) (overlapPercent * 30);
            }
        }

        if (myPlan.getBudget() != null && candidate.getBudget() != null && myPlan.getBudget() > 0) {
            double budgetDiff = Math.abs(myPlan.getBudget() - candidate.getBudget());
            double budgetPercent = 1 - (budgetDiff / myPlan.getBudget());
            score += (int) (Math.max(budgetPercent, 0) * 20);
        }

        if (myPlan.getTravelType() != null &&
                myPlan.getTravelType().equalsIgnoreCase(candidate.getTravelType())) {
            score += 10;
        }

        return Math.min(score, 100);
    }

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
}
