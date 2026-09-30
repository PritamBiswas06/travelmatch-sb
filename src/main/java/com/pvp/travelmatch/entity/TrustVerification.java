package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trust_verification", uniqueConstraints = @UniqueConstraint(name = "uk_trust_verification_user", columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrustVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone_number", length = 24)
    private String phoneNumber;
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default private boolean phoneVerified = false;
    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;
    @Column(name = "phone_otp_hash", length = 100)
    private String phoneOtpHash;
    @Column(name = "phone_otp_expiry")
    private LocalDateTime phoneOtpExpiry;
    @Column(name = "phone_otp_last_sent_at")
    private LocalDateTime phoneOtpLastSentAt;
    @Column(name = "phone_otp_attempts", nullable = false)
    @Builder.Default private int phoneOtpAttempts = 0;

    // These states can only be updated by a trusted verification-provider callback/admin workflow.
    @Column(name = "identity_status", nullable = false, length = 24)
    @Builder.Default private String identityStatus = "NOT_STARTED";
    @Column(name = "selfie_status", nullable = false, length = 24)
    @Builder.Default private String selfieStatus = "NOT_STARTED";

    @Column(name = "emergency_sharing_enabled", nullable = false)
    @Builder.Default private boolean emergencySharingEnabled = false;
    @Column(name = "profile_discoverable", nullable = false)
    @Builder.Default private boolean profileDiscoverable = true;
    @Column(name = "profile_photo_review_status", nullable = false, length = 24)
    @Builder.Default private String profilePhotoReviewStatus = "NOT_SUBMITTED";
    @Column(name = "profile_photo_submitted_at")
    private LocalDateTime profilePhotoSubmittedAt;
    @Column(name = "profile_photo_reviewed_at")
    private LocalDateTime profilePhotoReviewedAt;
    @Column(name = "profile_photo_review_note", length = 250)
    private String profilePhotoReviewNote;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
