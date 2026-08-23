package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.PostReaction;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByTravelPlanAndUser(TravelPlan travelPlan, User user);

    long countByTravelPlanAndReactionType(TravelPlan travelPlan, String reactionType);

    // Bulk-fetch a user's reactions across many plans (avoids N+1 in the feed)
    List<PostReaction> findByUserAndTravelPlanIn(User user, List<TravelPlan> travelPlans);
}
