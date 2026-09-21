package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelComment;
import com.pvp.travelmatch.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelCommentRepository extends JpaRepository<TravelComment, Long> {
    List<TravelComment> findByTravelPlanIdOrderByCreatedAtAsc(Long travelPlanId);
    long countByTravelPlanId(Long travelPlanId);

    @Query("""
        SELECT c.travelPlan.id, COUNT(c)
        FROM TravelComment c
        WHERE c.travelPlan.id IN :planIds
        GROUP BY c.travelPlan.id
    """)
    List<Object[]> countByPlanIds(
            @Param("planIds") List<Long> planIds
    );
    Optional<TravelComment> findByIdAndUserId(Long id, Long userId);
    void deleteByTravelPlan(TravelPlan travelPlan);
}
