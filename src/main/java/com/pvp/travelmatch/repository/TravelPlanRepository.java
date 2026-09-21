package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRepository
        extends JpaRepository<TravelPlan, Long>,
        JpaSpecificationExecutor<TravelPlan> {

    List<TravelPlan> findByUser(User user);

    Page<TravelPlan> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM TravelPlan t
        WHERE t.user.id = :userId
          AND t.status = 'ACTIVE'
          AND t.endDate IS NOT NULL
          AND t.endDate >= :today
        ORDER BY t.startDate ASC
    """)
    Page<TravelPlan> findUpcomingByUserId(
            Long userId,
            LocalDate today,
            Pageable pageable
    );

    long countByUserId(Long userId);

    Optional<TravelPlan> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<TravelPlan> findAll(Specification<TravelPlan> spec, Pageable pageable);

    @Query("""
        SELECT t.destination, COUNT(t)
        FROM TravelPlan t
        WHERE t.user.id = :userId
          AND t.destination IS NOT NULL
          AND TRIM(t.destination) <> ''
        GROUP BY t.destination
        ORDER BY COUNT(t) DESC
    """)
    List<Object[]> findDestinationCountsByUserId(
            Long userId,
            Pageable pageable
    );

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
        SELECT t
        FROM TravelPlan t
        JOIN FETCH t.user u
        WHERE u.id <> :userId
          AND t.status = 'ACTIVE'
          AND t.endDate >= :today
          AND NOT EXISTS (
              SELECT 1
              FROM BlockedUser b
              WHERE b.blocker.id = :userId
                AND b.blockedUser.id = u.id
          )
        ORDER BY t.createdAt DESC
    """)
    List<TravelPlan> findFastLatestFeed(
            Long userId,
            LocalDate today,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM TravelPlan t
        WHERE t.user.id <> :userId
        AND t.status = 'ACTIVE'
        AND t.endDate >= :today
        ORDER BY t.createdAt DESC
    """)
    List<TravelPlan> findFeedPlans(
            Long userId,
            LocalDate today
    );

    Page<TravelPlan>
    findByDestinationContainingIgnoreCaseOrFromLocationContainingIgnoreCase(
            String destination,
            String fromLocation,
            Pageable pageable
    );

    long countByStatus(String status);
}