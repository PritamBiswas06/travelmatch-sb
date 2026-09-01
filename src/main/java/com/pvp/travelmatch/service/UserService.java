package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.ProfileTripResponse;
import com.pvp.travelmatch.dto.UpdateProfileRequest;
import com.pvp.travelmatch.dto.UserProfileResponse;
import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.MatchRequestRepository;
import com.pvp.travelmatch.repository.PostReactionRepository;
import com.pvp.travelmatch.repository.SavedTravelPlanRepository;
import com.pvp.travelmatch.repository.TravelCommentRepository;
import com.pvp.travelmatch.repository.TravelMemoryRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_BIO_LENGTH = 500;
    private static final int MAX_IDEAL_PARTNER_LENGTH = 500;
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
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

        User currentUser = getCurrentUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());

        // 🔔 Notify the profile owner that someone viewed their profile.
        // - Only when it's NOT the owner's own profile.
        // - Viewer/owner are both resolved server-side above (JWT + path id
        //   lookup), never trusted from any request body/query param.
        // - Deduplicated inside NotificationService (max 1 per viewer/owner
        //   pair per 24h), so refreshing the page repeatedly won't spam.
        // - Wrapped so a notification failure can never break profile viewing.
        if (!isOwnProfile) {
            try {
                monetizationService.recordProfileView(targetUser.getId());
                notificationService.createProfileViewNotification(targetUser, currentUser);
            } catch (Exception e) {
                // Deliberately swallow: viewing a profile must always succeed
                // even if the notification side-effect fails.
            }
        }

        List<TravelPlan> userPlans = travelPlanRepository.findByUser(targetUser).stream()
                .sorted(Comparator.comparing(TravelPlan::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // All public travel plans are shown on the profile, including completed
        // trips, so a profile behaves like a social travel timeline.
        List<ProfileTripResponse> posts = userPlans.stream()
                .map(plan -> toProfileTripResponse(plan, currentUser, isOwnProfile))
                .toList();

        List<ProfileTripResponse> upcomingTrips = userPlans.stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus())
                        && plan.getEndDate() != null
                        && !plan.getEndDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(TravelPlan::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(plan -> toProfileTripResponse(plan, currentUser, isOwnProfile))
                .toList();

        List<com.pvp.travelmatch.dto.TravelMemoryResponse> travelMemories =
                travelMemoryRepository.findByUserIdOrderByCreatedAtDesc(targetUser.getId()).stream()
                        .map(com.pvp.travelmatch.dto.TravelMemoryResponse::fromEntity)
                        .toList();

        return buildProfileResponse(targetUser, isOwnProfile, upcomingTrips, posts, travelMemories);
    }

    private UserProfileResponse buildProfileResponse(User user, boolean isOwnProfile, List<ProfileTripResponse> upcomingTrips, List<ProfileTripResponse> posts, List<com.pvp.travelmatch.dto.TravelMemoryResponse> travelMemories) {
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

                .reviews(
                        travelerReviewService.getForUser(
                                user.getId()
                        )
                )
                .build();
    }

    private ProfileTripResponse toProfileTripResponse(TravelPlan plan, User currentUser, boolean isOwnProfile) {

        String matchRequestStatus = isOwnProfile
                ? null
                : matchRequestRepository.findBySenderIdAndTravelPlanId(currentUser.getId(), plan.getId())
                .map(MatchRequest::getStatus)
                .orElse("NONE");

        String reaction = postReactionRepository.findByTravelPlanAndUser(plan, currentUser)
                .map(com.pvp.travelmatch.entity.PostReaction::getReactionType)
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

    private String toPhotoDataUri(User user) {
        if (user.getProfilePhoto() == null || user.getProfilePhoto().length == 0
                || user.getProfilePhotoContentType() == null) {
            return null;
        }
        String base64 = Base64.getEncoder().encodeToString(user.getProfilePhoto());
        return "data:" + user.getProfilePhotoContentType() + ";base64," + base64;
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