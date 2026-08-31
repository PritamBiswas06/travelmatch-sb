package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.SavedTravelPlanResponse;
import com.pvp.travelmatch.entity.SavedTravelPlan;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.SavedTravelPlanRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedTravelPlanService {
    private final SavedTravelPlanRepository savedRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;

    @Transactional
    public SavedTravelPlanResponse save(Long travelPlanId) {
        User user = currentUser();
        TravelPlan plan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel plan not found"));

        if (savedRepository.existsByUserIdAndTravelPlanId(user.getId(), plan.getId())) {
            return SavedTravelPlanResponse.fromEntity(
                    savedRepository.findByUserIdAndTravelPlanId(user.getId(), plan.getId()).orElseThrow());
        }

        SavedTravelPlan saved = SavedTravelPlan.builder()
                .user(user)
                .travelPlan(plan)
                .createdAt(LocalDateTime.now())
                .build();
        return SavedTravelPlanResponse.fromEntity(savedRepository.save(saved));
    }

    @Transactional
    public void unsave(Long travelPlanId) {
        User user = currentUser();
        SavedTravelPlan saved = savedRepository.findByUserIdAndTravelPlanId(user.getId(), travelPlanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved trip not found"));
        savedRepository.delete(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedTravelPlanResponse> getMine() {
        User user = currentUser();
        return savedRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SavedTravelPlanResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSaved(Long travelPlanId) {
        return savedRepository.existsByUserIdAndTravelPlanId(currentUser().getId(), travelPlanId);
    }

    private User currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
