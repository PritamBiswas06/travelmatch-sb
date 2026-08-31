package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelComment;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelCommentRepository extends JpaRepository<TravelComment, Long> {
    List<TravelComment> findByTravelPlanIdOrderByCreatedAtAsc(Long travelPlanId);
    long countByTravelPlanId(Long travelPlanId);
    Optional<TravelComment> findByIdAndUserId(Long id, Long userId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
