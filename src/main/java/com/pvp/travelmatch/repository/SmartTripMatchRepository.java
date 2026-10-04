package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.SmartTripMatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmartTripMatchRepository extends JpaRepository<SmartTripMatch, Long> {

    Optional<SmartTripMatch> findByPlanAIdAndPlanBId(Long planAId, Long planBId);

    @EntityGraph(attributePaths = {"planA", "planA.user", "planB", "planB.user"})
    @Query("""
        SELECT m FROM SmartTripMatch m
        WHERE (m.planA.user.id = :userId AND m.dismissedByA = false)
           OR (m.planB.user.id = :userId AND m.dismissedByB = false)
        ORDER BY m.score DESC, m.createdAt DESC
    """)
    List<SmartTripMatch> findVisibleMatchesForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"planA", "planA.user", "planB", "planB.user"})
    @Query("""
        SELECT m FROM SmartTripMatch m
        WHERE m.planA.id = :planId OR m.planB.id = :planId
        ORDER BY m.score DESC, m.createdAt DESC
    """)
    List<SmartTripMatch> findByPlanIdWithUsers(@Param("planId") Long planId);
}
