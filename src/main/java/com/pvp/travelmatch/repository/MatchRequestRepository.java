package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    List<MatchRequest> findByReceiver(User receiver);

    List<MatchRequest> findBySender(User sender);

    Optional<MatchRequest> findBySenderIdAndTravelPlanId(Long senderId, Long travelPlanId);
}