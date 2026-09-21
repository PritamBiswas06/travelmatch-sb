package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.PostReaction;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    void deleteByTravelPlan(TravelPlan travelPlan);
    Optional<PostReaction> findByTravelPlanAndUser(TravelPlan travelPlan, User user);

    long countByTravelPlanAndReactionType(TravelPlan travelPlan, String reactionType);

    // Bulk-fetch a user's reactions across many plans (avoids N+1 in the feed)
    List<PostReaction> findByUserAndTravelPlanIn(User user, List<TravelPlan> travelPlans);

    @Query("""
        SELECT r.travelPlan.id, r.reactionType, COUNT(r)
        FROM PostReaction r
        WHERE r.travelPlan.id IN :planIds
        GROUP BY r.travelPlan.id, r.reactionType
    """)
    List<Object[]> countByPlanIdsGrouped(
            @Param("planIds") List<Long> planIds
    );
}
