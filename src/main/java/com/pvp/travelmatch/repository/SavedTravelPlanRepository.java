package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.SavedTravelPlan;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SavedTravelPlanRepository extends JpaRepository<SavedTravelPlan, Long> {
    List<SavedTravelPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedTravelPlan> findByUserIdAndTravelPlanId(Long userId, Long travelPlanId);
    boolean existsByUserIdAndTravelPlanId(Long userId, Long travelPlanId);

    @Query("""
        SELECT s.travelPlan.id
        FROM SavedTravelPlan s
        WHERE s.user.id = :userId
          AND s.travelPlan.id IN :planIds
    """)
    List<Long> findSavedPlanIds(
            @Param("userId") Long userId,
            @Param("planIds") List<Long> planIds
    );
    long countByTravelPlanId(Long travelPlanId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
