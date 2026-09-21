package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.MatchRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRequestRepository
        extends JpaRepository<MatchRequest, Long> {

    List<MatchRequest> findByReceiver(User receiver);

    @EntityGraph(attributePaths = {"sender", "receiver", "travelPlan"})
    Page<MatchRequest> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    List<MatchRequest> findBySender(User sender);

    Optional<MatchRequest>
    findBySenderIdAndTravelPlanId(
            Long senderId,
            Long travelPlanId
    );

    @Query("""
        SELECT m FROM MatchRequest m
        WHERE m.sender.id = :senderId
          AND m.travelPlan.id IN :planIds
    """)
    List<MatchRequest> findBySenderIdAndTravelPlanIds(
            @Param("senderId") Long senderId,
            @Param("planIds") List<Long> planIds
    );

    void deleteByTravelPlan(TravelPlan travelPlan);

    Page<MatchRequest> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    long countByStatus(String status);

    long countByReceiverId(Long receiverId);

    long countBySenderIdAndCreatedAtAfter(
            Long senderId,
            LocalDateTime after
    );
}