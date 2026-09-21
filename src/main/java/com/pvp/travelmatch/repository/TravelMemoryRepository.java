package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelMemory;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TravelMemoryRepository extends JpaRepository<TravelMemory, Long> {
    List<TravelMemory> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<TravelMemory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByTravelPlanId(Long travelPlanId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
