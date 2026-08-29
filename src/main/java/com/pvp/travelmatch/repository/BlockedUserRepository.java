package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockedUserRepository
        extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedUserId(
            Long blockerId,
            Long blockedUserId
    );

    Optional<BlockedUser> findByBlockerIdAndBlockedUserId(
            Long blockerId,
            Long blockedUserId
    );

    List<BlockedUser> findByBlockerId(Long blockerId);
}