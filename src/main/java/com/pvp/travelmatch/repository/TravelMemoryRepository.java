package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelMemory;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelMemoryRepository extends JpaRepository<TravelMemory, Long> {
    List<TravelMemory> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByTravelPlanId(Long travelPlanId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
