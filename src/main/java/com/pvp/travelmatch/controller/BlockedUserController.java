package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.entity.BlockedUser;
import com.pvp.travelmatch.service.BlockedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class BlockedUserController {

    private final BlockedUserService blockedUserService;

    @PostMapping("/{userId}/block")
    public Map<String, String> block(
            @PathVariable Long userId) {

        blockedUserService.block(userId);

        return Map.of(
                "message",
                "User blocked successfully"
        );
    }

    @DeleteMapping("/{userId}/block")
    public Map<String, String> unblock(
            @PathVariable Long userId) {

        blockedUserService.unblock(userId);

        return Map.of(
                "message",
                "User unblocked successfully"
        );
    }

    @GetMapping("/{userId}/block")
    public Map<String, Boolean> isBlocked(
            @PathVariable Long userId) {

        return Map.of(
                "blocked",
                blockedUserService.isBlocked(userId)
        );
    }

    @GetMapping("/me/blocked")
    public List<BlockedUser> getMyBlockedUsers() {

        return blockedUserService.getMyBlockedUsers();
    }
}