package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.UpdateProfileRequest;
import com.pvp.travelmatch.dto.UserProfileResponse;
import com.pvp.travelmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Public (authenticated) profile view — used by Feed "View Profile" and
    // by the sidebar's "Profile" link for the logged-in user's own profile.
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getProfile(userId, page, size);
    }

    // Edit own profile only — the authenticated user is resolved from the
    // JWT/security context, never from a path/body user id.
    @PutMapping("/me/profile")
    public UserProfileResponse updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(request);
    }

    // Profile photo upload — stored on the User row itself (no existing
    // file/cloud storage mechanism in this project to reuse).
    @GetMapping("/{userId}/photo")
    public ResponseEntity<byte[]> getProfilePhoto(@PathVariable Long userId) {
        byte[] bytes = userService.getProfilePhotoBytes(userId);
        String contentType = userService.getProfilePhotoContentType(userId);

        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType;
        try {
            mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS).cachePublic())
                .body(bytes);
    }

    @PostMapping(value = "/me/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        return userService.uploadProfilePhoto(file);
    }

    @DeleteMapping("/me/profile/photo")
    public UserProfileResponse removeProfilePhoto() {
        return userService.removeProfilePhoto();
    }
}