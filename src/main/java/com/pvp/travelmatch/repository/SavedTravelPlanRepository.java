package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.SavedTravelPlan;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedTravelPlanRepository extends JpaRepository<SavedTravelPlan, Long> {
    List<SavedTravelPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedTravelPlan> findByUserIdAndTravelPlanId(Long userId, Long travelPlanId);
    boolean existsByUserIdAndTravelPlanId(Long userId, Long travelPlanId);
    long countByTravelPlanId(Long travelPlanId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
