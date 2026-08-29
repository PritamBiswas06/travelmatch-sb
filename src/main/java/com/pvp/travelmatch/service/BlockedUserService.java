package com.pvp.travelmatch.service;

import com.pvp.travelmatch.entity.BlockedUser;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.BlockedUserRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockedUserService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    @Transactional
    public void block(Long userId) {

        User blocker = getCurrentUser();

        User blockedUser = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Traveler not found"));

        if (blocker.getId().equals(blockedUser.getId())) {
            throw new RuntimeException(
                    "You cannot block yourself"
            );
        }

        boolean alreadyBlocked =
                blockedUserRepository
                        .existsByBlockerIdAndBlockedUserId(
                                blocker.getId(),
                                blockedUser.getId()
                        );

        if (!alreadyBlocked) {

            BlockedUser block = BlockedUser.builder()
                    .blocker(blocker)
                    .blockedUser(blockedUser)
                    .createdAt(LocalDateTime.now())
                    .build();

            blockedUserRepository.save(block);
        }
    }

    @Transactional
    public void unblock(Long userId) {

        User blocker = getCurrentUser();

        blockedUserRepository
                .findByBlockerIdAndBlockedUserId(
                        blocker.getId(),
                        userId
                )
                .ifPresent(blockedUserRepository::delete);
    }

    public boolean isBlocked(Long userId) {

        User blocker = getCurrentUser();

        return blockedUserRepository
                .existsByBlockerIdAndBlockedUserId(
                        blocker.getId(),
                        userId
                );
    }

    public List<BlockedUser> getMyBlockedUsers() {

        User blocker = getCurrentUser();

        return blockedUserRepository
                .findByBlockerId(blocker.getId());
    }

    public boolean isBlockedEitherWay(
            Long userA,
            Long userB) {

        return blockedUserRepository
                .existsByBlockerIdAndBlockedUserId(
                        userA,
                        userB
                )
                ||
                blockedUserRepository
                        .existsByBlockerIdAndBlockedUserId(
                                userB,
                                userA
                        );
    }
}