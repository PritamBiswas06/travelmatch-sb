package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    List<MatchRequest> findByReceiver(User receiver);
    List<MatchRequest> findBySender(User sender);

    Optional<MatchRequest> findBySenderIdAndTravelPlanId(Long senderId, Long travelPlanId);

    void deleteByTravelPlan(TravelPlan travelPlan);

    Page<MatchRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
}
