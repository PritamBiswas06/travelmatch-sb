package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.ProfileTripResponse;
import com.pvp.travelmatch.dto.TravelerReviewResponse;
import com.pvp.travelmatch.dto.UpdateProfileRequest;
import com.pvp.travelmatch.dto.UserProfileResponse;
import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.PostReaction;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_BIO_LENGTH = 500;
    private static final int MAX_IDEAL_PARTNER_LENGTH = 500;
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPartnerRepository travelPartnerRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final NotificationService notificationService;
    private final TravelerReviewService travelerReviewService;
    private final PostReactionRepository postReactionRepository;
    private final SavedTravelPlanRepository savedTravelPlanRepository;
    private final TravelCommentRepository travelCommentRepository;
    private final TravelMemoryRepository travelMemoryRepository;
    private final MonetizationService monetizationService;

    // ==================== VIEW PROFILE ====================

    public UserProfileResponse getProfile(Long userId) {
        return getProfile(userId, 0, 10);
    }

    public UserProfileResponse getProfile(Long userId, int page, int size) {

        User currentUser = getCurrentUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());

        if (!isOwnProfile) {
            try {
                monetizationService.recordProfileView(targetUser.getId());
                notificationService.createProfileViewNotification(targetUser, currentUser);
            } catch (Exception e) {
                // Profile reads must not fail because of notification side effects.
            }
        }

        final int safePage = Math.max(page, 0);
        final int pageSize = Math.min(Math.max(size, 1), 20);
        PageRequest profilePage = PageRequest.of(
                safePage, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var postsPage = travelPlanRepository
                .findByUserIdOrderByCreatedAtDesc(targetUser.getId(), profilePage);
        List<TravelPlan> userPlans = postsPage.getContent();

        List<ProfileTripResponse> posts =
                toProfileTripResponses(userPlans, currentUser, isOwnProfile);

        List<TravelPlan> upcomingPlanList = travelPlanRepository
                .findUpcomingByUserId(
                        targetUser.getId(),
                        LocalDate.now(),
                        PageRequest.of(safePage, pageSize)
                )
                .getContent();

        List<ProfileTripResponse> upcomingTrips =
                toProfileTripResponses(
                        upcomingPlanList, currentUser, isOwnProfile);

        var memoriesPage = travelMemoryRepository
                .findByUserIdOrderByCreatedAtDesc(
                        targetUser.getId(), profilePage);
        List<com.pvp.travelmatch.dto.TravelMemoryResponse> travelMemories =
                memoriesPage.getContent()
                        .stream()
                        .map(com.pvp.travelmatch.dto.TravelMemoryResponse::fromEntity)
                        .toList();

        var friendsPage = travelPartnerRepository
                .findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(
                        targetUser.getId(),
                        targetUser.getId(),
                        profilePage);
        List<com.pvp.travelmatch.dto.FriendResponse> friends =
                friendsPage.getContent().stream()
                        .map(partner -> {
                            User friend = partner.getUserOne().getId().equals(targetUser.getId())
                                    ? partner.getUserTwo()
                                    : partner.getUserOne();
                            return com.pvp.travelmatch.dto.FriendResponse.builder()
                                    .userId(friend.getId())
                                    .name(friend.getName())
                                    .username(friend.getUsername())
                                    .city(friend.getCity())
                                    .country(friend.getCountry())
                                    .gender(friend.getGender())
                                    .profilePhotoUrl(toPhotoDataUri(friend))
                                    .build();
                        })
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toMap(
                                        com.pvp.travelmatch.dto.FriendResponse::getUserId,
                                        f -> f,
                                        (first, second) -> first,
                                        java.util.LinkedHashMap::new),
                                map -> new java.util.ArrayList<>(map.values())));

        var reviewsPage = travelerReviewService.getForUserPageResult(
                targetUser.getId(), profilePage);
        List<TravelerReviewResponse> reviews =
                reviewsPage.getContent();

        return buildProfileResponse(
                targetUser,
                isOwnProfile,
                upcomingTrips,
                posts,
                travelMemories,
                friends,
                reviews,
                travelPlanRepository.countByUserId(targetUser.getId()),
                travelPartnerRepository.countByUserOneIdOrUserTwoId(targetUser.getId(), targetUser.getId()),
                postsPage.hasNext(),
                memoriesPage.hasNext(),
                friendsPage.hasNext(),
                reviewsPage.hasNext()
        );
    }


    private UserProfileResponse buildProfileResponse(
            User user,
            boolean isOwnProfile,
            List<ProfileTripResponse> upcomingTrips,
            List<ProfileTripResponse> posts,
            List<com.pvp.travelmatch.dto.TravelMemoryResponse> travelMemories,
            List<com.pvp.travelmatch.dto.FriendResponse> friends,
            List<TravelerReviewResponse> reviews,
            long tripCount,
            long friendCount,
            boolean postsHasMore,
            boolean memoriesHasMore,
            boolean friendsHasMore,
            boolean reviewsHasMore) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .age(user.getAge())
                .gender(user.getGender())
                .city(user.getCity())
                .state(user.getState())
                .country(user.getCountry())
                .verified(Boolean.TRUE.equals(user.getVerified()))
                .bio(user.getBio())
                .profilePhotoUrl(toPhotoDataUri(user))
                .travelStyle(splitToList(user.getTravelStyle()))
                .travelInterests(splitToList(user.getTravelInterests()))
                .preferredDestinations(splitToList(user.getPreferredDestinations()))
                .budgetPreference(user.getBudgetPreference())
                .travelFrequency(user.getTravelFrequency())
                .languages(splitToList(user.getLanguages()))
                .idealTravelPartner(user.getIdealTravelPartner())
                .instagramUrl(user.getInstagramUrl())
                .linkedinUrl(user.getLinkedinUrl())
                .websiteUrl(user.getWebsiteUrl())
                .isOwnProfile(isOwnProfile)
                .premiumUser(monetizationService.isPremium(user))
                .upcomingTrips(upcomingTrips)
                .posts(posts)
                .travelMemories(travelMemories)
                .averageRating(
                        travelerReviewService.getAverage(
                                user.getId()
                        )
                )
                .reviewCount(
                        travelerReviewService.getCount(
                                user.getId()
                        )
                )

.reviews(reviews)
                .friends(friends)
                .friendCount(friendCount)
                .postsHasMore(postsHasMore)
                .memoriesHasMore(memoriesHasMore)
                .friendsHasMore(friendsHasMore)
                .reviewsHasMore(reviewsHasMore)
                .build();
    }

    private List<ProfileTripResponse> toProfileTripResponses(
            List<TravelPlan> plans,
            User currentUser,
            boolean isOwnProfile) {

        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream().map(TravelPlan::getId).toList();

        Map<Long, Long> likes = new HashMap<>();
        Map<Long, Long> comments = new HashMap<>();
        for (Object[] row : postReactionRepository.countByPlanIdsGrouped(planIds)) {
            if ("LIKE".equals(String.valueOf(row[1]))) {
                likes.put(((Number) row[0]).longValue(),
                        ((Number) row[2]).longValue());
            }
        }
        for (Object[] row : travelCommentRepository.countByPlanIds(planIds)) {
            comments.put(((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue());
        }

        Map<Long, String> reactions = new HashMap<>();
        for (PostReaction reaction :
                postReactionRepository.findByUserAndTravelPlanIn(
                        currentUser, plans)) {
            reactions.put(reaction.getTravelPlan().getId(),
                    reaction.getReactionType());
        }

        Set<Long> savedIds = new HashSet<>(
                savedTravelPlanRepository.findSavedPlanIds(
                        currentUser.getId(), planIds));

        Map<Long, String> requestStatuses = new HashMap<>();
        if (!isOwnProfile) {
            for (MatchRequest request :
                    matchRequestRepository.findBySenderIdAndTravelPlanIds(
                            currentUser.getId(), planIds)) {
                requestStatuses.put(
                        request.getTravelPlan().getId(),
                        request.getStatus());
            }
        }

        boolean partners = !isOwnProfile
                && plans.get(0).getUser() != null
                && travelPartnerRepository.arePartners(
                        currentUser, plans.get(0).getUser());

        return plans.stream()
                .map(plan -> ProfileTripResponse.builder()
                        .id(plan.getId())
                        .fromLocation(plan.getFromLocation())
                        .destination(plan.getDestination())
                        .startDate(plan.getStartDate())
                        .endDate(plan.getEndDate())
                        .budget(plan.getBudget())
                        .travelType(plan.getTravelType())
                        .status(plan.getStatus())
                        .createdAt(plan.getCreatedAt())
                        .likeCount(likes.getOrDefault(plan.getId(), 0L))
                        .shareCount(plan.getShareCount() == null ? 0 : plan.getShareCount())
                        .commentCount(comments.getOrDefault(plan.getId(), 0L))
                        .currentUserReaction(reactions.get(plan.getId()))
                        .currentUserSaved(savedIds.contains(plan.getId()))
                        .matchRequestStatus(
                                isOwnProfile
                                        ? null
                                        : partners
                                            ? "FRIENDS"
                                            : requestStatuses.getOrDefault(
                                                plan.getId(), "NONE"))
                        .build())
                .toList();
    }

    private ProfileTripResponse toProfileTripResponse(TravelPlan plan, User currentUser, boolean isOwnProfile) {

        String matchRequestStatus;
        if (isOwnProfile) {
            matchRequestStatus = null;
        } else if (travelPartnerRepository.arePartners(currentUser, plan.getUser())) {
            matchRequestStatus = "FRIENDS";
        } else {
            matchRequestStatus = matchRequestRepository.findBySenderIdAndTravelPlanId(currentUser.getId(), plan.getId())
                    .map(MatchRequest::getStatus)
                    .orElse("NONE");
        }

        String reaction = postReactionRepository.findByTravelPlanAndUser(plan, currentUser)
                .map(PostReaction::getReactionType)
                .orElse(null);

        return ProfileTripResponse.builder()
                .id(plan.getId())
                .fromLocation(plan.getFromLocation())
                .destination(plan.getDestination())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .budget(plan.getBudget())
                .travelType(plan.getTravelType())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .likeCount(postReactionRepository.countByTravelPlanAndReactionType(plan, "LIKE"))
                .shareCount(plan.getShareCount() == null ? 0 : plan.getShareCount())
                .commentCount(travelCommentRepository.countByTravelPlanId(plan.getId()))
                .currentUserReaction(reaction)
                .currentUserSaved(savedTravelPlanRepository.existsByUserIdAndTravelPlanId(currentUser.getId(), plan.getId()))
                .matchRequestStatus(matchRequestStatus)
                .build();
    }

    // ==================== EDIT OWN PROFILE ====================

    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {

        // Authorization always comes from the authenticated principal, never
        // from a client-supplied user id, so a user can only ever edit themself.
        User currentUser = getCurrentUser();

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new RuntimeException("Name cannot be empty");
            }
            currentUser.setName(name);
        }

        if (request.getUsername() != null) {
            currentUser.setUsername(validateAndNormalizeUsername(request.getUsername(), currentUser.getId()));
        }

        if (request.getAge() != null) {
            if (request.getAge() < 1 || request.getAge() > 120) {
                throw new RuntimeException("Please enter a valid age");
            }
            currentUser.setAge(request.getAge());
        }

        if (request.getGender() != null) {
            currentUser.setGender(blankToNull(request.getGender()));
        }

        if (request.getCity() != null) {
            currentUser.setCity(blankToNull(request.getCity()));
        }

        if (request.getState() != null) {
            currentUser.setState(blankToNull(request.getState()));
        }

        if (request.getCountry() != null) {
            currentUser.setCountry(blankToNull(request.getCountry()));
        }

        if (request.getBio() != null) {
            if (request.getBio().length() > MAX_BIO_LENGTH) {
                throw new RuntimeException("Bio must be " + MAX_BIO_LENGTH + " characters or fewer");
            }
            currentUser.setBio(blankToNull(request.getBio()));
        }

        if (request.getBudgetPreference() != null) {
            currentUser.setBudgetPreference(blankToNull(request.getBudgetPreference()));
        }

        if (request.getTravelFrequency() != null) {
            currentUser.setTravelFrequency(blankToNull(request.getTravelFrequency()));
        }

        if (request.getIdealTravelPartner() != null) {
            if (request.getIdealTravelPartner().length() > MAX_IDEAL_PARTNER_LENGTH) {
                throw new RuntimeException("Ideal travel partner description must be "
                        + MAX_IDEAL_PARTNER_LENGTH + " characters or fewer");
            }
            currentUser.setIdealTravelPartner(blankToNull(request.getIdealTravelPartner()));
        }

        if (request.getInstagramUrl() != null) {
            currentUser.setInstagramUrl(validateAndNormalizeUrl(request.getInstagramUrl()));
        }

        if (request.getLinkedinUrl() != null) {
            currentUser.setLinkedinUrl(validateAndNormalizeUrl(request.getLinkedinUrl()));
        }

        if (request.getWebsiteUrl() != null) {
            currentUser.setWebsiteUrl(validateAndNormalizeUrl(request.getWebsiteUrl()));
        }

        if (request.getTravelStyle() != null) {
            currentUser.setTravelStyle(joinDistinct(request.getTravelStyle()));
        }

        if (request.getTravelInterests() != null) {
            currentUser.setTravelInterests(joinDistinct(request.getTravelInterests()));
        }

        if (request.getPreferredDestinations() != null) {
            currentUser.setPreferredDestinations(joinDistinct(request.getPreferredDestinations()));
        }

        if (request.getLanguages() != null) {
            currentUser.setLanguages(joinDistinct(request.getLanguages()));
        }

        userRepository.save(currentUser);

        return getProfile(currentUser.getId());
    }

    // ==================== PROFILE PHOTO ====================

    public UserProfileResponse uploadProfilePhoto(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please choose a photo to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PHOTO_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Photo must be a JPEG, PNG, or WEBP image");
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new RuntimeException("Photo must be smaller than 2MB");
        }

        User currentUser = getCurrentUser();

        try {
            currentUser.setProfilePhoto(file.getBytes());
            currentUser.setProfilePhotoContentType(contentType);
        } catch (Exception e) {
            throw new RuntimeException("Could not read uploaded photo");
        }

        userRepository.save(currentUser);

        return getProfile(currentUser.getId());
    }

    public UserProfileResponse removeProfilePhoto() {

        User currentUser = getCurrentUser();

        currentUser.setProfilePhoto(null);
        currentUser.setProfilePhotoContentType(null);

        userRepository.save(currentUser);

        return getProfile(currentUser.getId());
    }

    // ==================== HELPERS ====================

    public byte[] getProfilePhotoBytes(Long userId) {
        return userRepository.findById(userId)
                .map(User::getProfilePhoto)
                .orElse(null);
    }

    public String getProfilePhotoContentType(Long userId) {
        return userRepository.findById(userId)
                .map(User::getProfilePhotoContentType)
                .orElse(null);
    }

    private String toPhotoDataUri(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }

        // Do not access the LONGBLOB while building profile JSON.
        // The browser fetches the image through the dedicated photo endpoint.
        return "/api/users/" + user.getId() + "/photo";
    }

    private String validateAndNormalizeUsername(String rawUsername, Long currentUserId) {
        String username = rawUsername.trim();

        if (username.isEmpty()) {
            return null; // explicit clear
        }

        if (!username.matches("^[a-zA-Z0-9_.]{3,30}$")) {
            throw new RuntimeException(
                    "Username must be 3-30 characters and contain only letters, numbers, dots, or underscores"
            );
        }

        userRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new RuntimeException("That username is already taken");
            }
        });

        return username;
    }

    private String validateAndNormalizeUrl(String rawUrl) {
        String url = rawUrl.trim();

        if (url.isEmpty()) {
            return null; // explicit clear
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new RuntimeException("Links must start with http:// or https://");
        }

        return url;
    }

    private String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String joinDistinct(List<String> values) {
        List<String> cleaned = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    private List<String> splitToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}