package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.UpdateProfileRequest;
import com.pvp.travelmatch.dto.UserProfileResponse;
import com.pvp.travelmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Public (authenticated) profile view — used by Feed "View Profile"
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return userService.getProfile(userId);
    }

    // Edit own profile only — the authenticated user is resolved from the
    // JWT/security context, never from a path/body user id.
    @PutMapping("/me/profile")
    public UserProfileResponse updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(request);
    }
}