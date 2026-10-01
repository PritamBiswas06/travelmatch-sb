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

    /** Admin-only queue for profile photos and optional manual selfie reviews. */
    @GetMapping("/photo-reviews")
    public List<PhotoReviewQueueItem> pendingPhotoReviews() {
        List<PhotoReviewQueueItem> profilePhotos = verifications
                .findByProfilePhotoReviewStatusOrderByProfilePhotoSubmittedAtAsc("PENDING")
                .stream().map(trust -> {
                    User user = trust.getUser();
                    return PhotoReviewQueueItem.builder().userId(user.getId()).name(user.getName()).email(user.getEmail())
                            .photoDataUrl(toDataUrl(user.getProfilePhoto(), user.getProfilePhotoContentType()))
                            .status(trust.getProfilePhotoReviewStatus()).reviewType("PROFILE_PHOTO")
                            .submittedAt(trust.getProfilePhotoSubmittedAt()).build();
                }).toList();
        List<PhotoReviewQueueItem> selfies = verifications
                .findBySelfieReviewStatusOrderBySelfieSubmittedAtAsc("PENDING")
                .stream().map(trust -> {
                    User user = trust.getUser();
                    return PhotoReviewQueueItem.builder().userId(user.getId()).name(user.getName()).email(user.getEmail())
                            .photoDataUrl(toDataUrl(trust.getSelfiePhoto(), trust.getSelfieContentType()))
                            .status(trust.getSelfieReviewStatus()).reviewType("SELFIE")
                            .submittedAt(trust.getSelfieSubmittedAt()).build();
                }).toList();
        List<PhotoReviewQueueItem> all = new ArrayList<>();
        all.addAll(profilePhotos); all.addAll(selfies);
        return all.stream().sorted((left, right) -> {
            if (left.getSubmittedAt() == null) return right.getSubmittedAt() == null ? 0 : 1;
            if (right.getSubmittedAt() == null) return -1;
            return left.getSubmittedAt().compareTo(right.getSubmittedAt());
        }).toList();
    }

    private String toDataUrl(byte[] photo, String mime) {
        boolean safeImageType = mime != null && (mime.equalsIgnoreCase("image/jpeg")
                || mime.equalsIgnoreCase("image/png") || mime.equalsIgnoreCase("image/webp"));
        return photo == null || photo.length == 0 || !safeImageType ? null
                : "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(photo);
    }

    @PutMapping("/photo-reviews/{userId}")
    public java.util.Map<String, String> decidePhotoReview(@PathVariable Long userId,
                                                           @RequestParam(defaultValue = "PROFILE_PHOTO") String type,
                                                           @Valid @RequestBody PhotoReviewDecisionRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be APPROVED or REJECTED.");
        }
        String note = request.getNote() == null ? "" : request.getNote().trim();
        if (status.equals("REJECTED") && note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a short note explaining what the user needs to change.");
        }
        users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        var trust = verifications.findByUserId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review request not found."));
        if ("SELFIE".equalsIgnoreCase(type)) {
            if (!"PENDING".equals(trust.getSelfieReviewStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This selfie is not waiting for review.");
            trust.setSelfieReviewStatus(status); trust.setSelfieReviewedAt(LocalDateTime.now()); trust.setSelfieReviewNote(note.isBlank() ? null : note);
            // Keep the submitted image only while pending; remove it after a decision.
            trust.setSelfiePhoto(null); trust.setSelfieContentType(null);
        } else if ("PROFILE_PHOTO".equalsIgnoreCase(type)) {
            if (!"PENDING".equals(trust.getProfilePhotoReviewStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This photo is not waiting for review.");
            trust.setProfilePhotoReviewStatus(status); trust.setProfilePhotoReviewedAt(LocalDateTime.now()); trust.setProfilePhotoReviewNote(note.isBlank() ? null : note);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown review type.");
        }
        verifications.save(trust);
        return java.util.Map.of("message", ("SELFIE".equalsIgnoreCase(type) ? "Selfie" : "Profile photo") + " review marked " + status.toLowerCase() + ".");
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
