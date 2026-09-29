package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.TrustRiskSignalResponse;
import com.pvp.travelmatch.dto.PhotoReviewDecisionRequest;
import com.pvp.travelmatch.dto.PhotoReviewQueueItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import java.util.Base64;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.ReportRepository;
import com.pvp.travelmatch.repository.TrustVerificationRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/trust")
@RequiredArgsConstructor
public class AdminTrustController {
    private final UserRepository users;
    private final ReportRepository reports;
    private final TrustVerificationRepository verifications;

    /** Admin-only queue for manually reviewing the existing profile photo. */
    @GetMapping("/photo-reviews")
    public List<PhotoReviewQueueItem> pendingPhotoReviews() {
        return users.findAll().stream().map(user -> {
            var trust = verifications.findByUserId(user.getId()).orElse(null);
            if (trust == null || !"PENDING".equals(trust.getProfilePhotoReviewStatus())) return null;
            byte[] photo = user.getProfilePhoto();
            String mime = user.getProfilePhotoContentType();
            String dataUrl = photo == null || photo.length == 0 || mime == null ? null
                    : "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(photo);
            return PhotoReviewQueueItem.builder().userId(user.getId()).name(user.getName())
                    .email(user.getEmail()).photoDataUrl(dataUrl).status(trust.getProfilePhotoReviewStatus())
                    .submittedAt(trust.getUpdatedAt()).build();
        }).filter(java.util.Objects::nonNull).toList();
    }

    @PutMapping("/photo-reviews/{userId}")
    public java.util.Map<String, String> decidePhotoReview(@PathVariable Long userId,
                                                           @Valid @RequestBody PhotoReviewDecisionRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be APPROVED or REJECTED.");
        }
        User user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        var trust = verifications.findByUserId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo review request not found."));
        if (!"PENDING".equals(trust.getProfilePhotoReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This photo is not waiting for review.");
        }
        trust.setProfilePhotoReviewStatus(status);
        trust.setProfilePhotoReviewedAt(LocalDateTime.now());
        String note = request.getNote() == null ? null : request.getNote().trim();
        trust.setProfilePhotoReviewNote(note == null || note.isBlank() ? null : note);
        verifications.save(trust);
        return java.util.Map.of("message", "Photo review marked " + status.toLowerCase() + ".");
    }

    /** Admin-only review queue. Signals are descriptive, not an automatic fraud verdict. */
    @GetMapping("/review-signals")
    public List<TrustRiskSignalResponse> reviewSignals(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "25") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        return users.findAll(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(this::toResponse).filter(r -> !r.getReviewSignals().isEmpty()).toList();
    }

    private TrustRiskSignalResponse toResponse(User user) {
        List<String> signals = new ArrayList<>();
        if (!Boolean.TRUE.equals(user.getVerified())) signals.add("Email address is not verified");
        if (user.getProfilePhotoContentType() == null || user.getProfilePhotoContentType().isBlank()) signals.add("No profile photo uploaded");
        if (user.getBio() == null || user.getBio().isBlank()) signals.add("Profile bio is empty");
        if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(LocalDateTime.now().minusHours(72))) signals.add("Account was created within the last 72 hours");
        var trust = verifications.findByUserId(user.getId()).orElse(null);
        if (trust != null && trust.isPhoneVerified() && trust.getPhoneNumber() != null
                && verifications.countVerifiedPhoneDuplicates(trust.getPhoneNumber(), user.getId()) > 0) {
            signals.add("Verified phone number is also used by another account");
        }
        long reportCount = reports.countByReportedUserId(user.getId());
        if (reportCount > 0) signals.add("One or more user reports exist; review report context");
        return TrustRiskSignalResponse.builder().userId(user.getId()).name(user.getName()).email(user.getEmail())
                .createdAt(user.getCreatedAt()).reviewSignals(signals).reportCount(reportCount).build();
    }
}
