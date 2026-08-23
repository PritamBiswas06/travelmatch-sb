package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.ProfileTripResponse;
import com.pvp.travelmatch.dto.UpdateProfileRequest;
import com.pvp.travelmatch.dto.UserProfileResponse;
import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.MatchRequestRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final MatchRequestRepository matchRequestRepository;

    // ==================== VIEW PROFILE ====================

    public UserProfileResponse getProfile(Long userId) {

        User currentUser = getCurrentUser();

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Traveler not found"));

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());

        List<ProfileTripResponse> upcomingTrips = travelPlanRepository.findByUser(targetUser).stream()
                .filter(plan -> "ACTIVE".equalsIgnoreCase(plan.getStatus())
                        && plan.getEndDate() != null
                        && !plan.getEndDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(TravelPlan::getStartDate))
                .map(plan -> toProfileTripResponse(plan, currentUser, isOwnProfile))
                .toList();

        return UserProfileResponse.builder()
                .userId(targetUser.getId())
                .name(targetUser.getName())
                .city(targetUser.getCity())
                .verified(Boolean.TRUE.equals(targetUser.getVerified()))
                .bio(targetUser.getBio())
                .travelStyle(targetUser.getTravelStyle())
                .travelInterests(splitToList(targetUser.getTravelInterests()))
                .preferredDestinations(splitToList(targetUser.getPreferredDestinations()))
                .budgetPreference(targetUser.getBudgetPreference())
                .isOwnProfile(isOwnProfile)
                .upcomingTrips(upcomingTrips)
                .build();
    }

    private ProfileTripResponse toProfileTripResponse(TravelPlan plan, User currentUser, boolean isOwnProfile) {

        String matchRequestStatus = isOwnProfile
                ? null
                : matchRequestRepository.findBySenderIdAndTravelPlanId(currentUser.getId(), plan.getId())
                .map(MatchRequest::getStatus)
                .orElse("NONE");

        return ProfileTripResponse.builder()
                .id(plan.getId())
                .fromLocation(plan.getFromLocation())
                .destination(plan.getDestination())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .budget(plan.getBudget())
                .travelType(plan.getTravelType())
                .status(plan.getStatus())
                .matchRequestStatus(matchRequestStatus)
                .build();
    }

    // ==================== EDIT OWN PROFILE ====================

    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {

        // Authorization always comes from the authenticated principal, never
        // from a client-supplied user id, so a user can only ever edit themself.
        User currentUser = getCurrentUser();

        if (request.getCity() != null) {
            currentUser.setCity(request.getCity());
        }
        if (request.getBio() != null) {
            currentUser.setBio(request.getBio());
        }
        if (request.getTravelStyle() != null) {
            currentUser.setTravelStyle(request.getTravelStyle());
        }
        if (request.getBudgetPreference() != null) {
            currentUser.setBudgetPreference(request.getBudgetPreference());
        }
        if (request.getTravelInterests() != null) {
            currentUser.setTravelInterests(String.join(",", request.getTravelInterests()));
        }
        if (request.getPreferredDestinations() != null) {
            currentUser.setPreferredDestinations(String.join(",", request.getPreferredDestinations()));
        }

        userRepository.save(currentUser);

        return getProfile(currentUser.getId());
    }

    // ==================== HELPERS ====================

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