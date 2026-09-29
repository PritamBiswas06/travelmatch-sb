package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelSquadMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelSquadMemberRepository extends JpaRepository<TravelSquadMember, Long> {

    Optional<TravelSquadMember> findBySquadIdAndUserId(Long squadId, Long userId);

    List<TravelSquadMember> findBySquadIdAndStatusOrderByJoinedAtAsc(Long squadId, String status);

    Page<TravelSquadMember> findByUserIdAndStatusOrderByUpdatedAtDesc(
            Long userId,
            String status,
            Pageable pageable
    );

    long countBySquadIdAndStatus(Long squadId, String status);

    boolean existsBySquadIdAndUserIdAndStatus(Long squadId, Long userId, String status);
}
