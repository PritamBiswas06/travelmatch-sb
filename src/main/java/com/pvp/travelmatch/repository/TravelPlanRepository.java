package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long>, JpaSpecificationExecutor<TravelPlan> {

    List<TravelPlan> findByUser(User user);

    @Query("""
        SELECT t FROM TravelPlan t
        WHERE t.destination = :destination
        AND t.user.id <> :userId
        AND t.startDate <= :endDate
        AND t.endDate >= :startDate
    """)
    List<TravelPlan> findMatchingPlans(
            String destination,
            LocalDate startDate,
            LocalDate endDate,
            Long userId
    );

    List<TravelPlan> findByUserIdNot(Long userId);

    @Query("""
        SELECT t FROM TravelPlan t
        WHERE t.user.id <> :userId
        AND t.status = 'ACTIVE'
        AND t.endDate >= :today
        ORDER BY t.createdAt DESC
    """)
    List<TravelPlan> findFeedPlans(Long userId, LocalDate today);

    Page<TravelPlan> findByDestinationContainingIgnoreCaseOrFromLocationContainingIgnoreCase(
            String destination, String fromLocation, Pageable pageable);

    long countByStatus(String status);
}
