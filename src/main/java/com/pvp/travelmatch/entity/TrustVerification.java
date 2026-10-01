
package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trust_verification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trust_verification_user",
                columnNames = "user_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Phone verification
    @Column(name = "phone_number", length = 24)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "phone_otp_hash", length = 100)
    private String phoneOtpHash;

    @Column(name = "phone_otp_expiry")
    private LocalDateTime phoneOtpExpiry;

    @Column(name = "phone_otp_last_sent_at")
    private LocalDateTime phoneOtpLastSentAt;

    @Column(name = "phone_otp_attempts", nullable = false)
    @Builder.Default
    private int phoneOtpAttempts = 0;

    // Identity and selfie review status
    @Column(name = "identity_status", nullable = false, length = 24)
    @Builder.Default
    private String identityStatus = "NOT_STARTED";

    @Column(name = "selfie_status", nullable = false, length = 24)
    @Builder.Default
    private String selfieStatus = "NOT_STARTED";

    // Profile and safety settings
    @Column(name = "emergency_sharing_enabled", nullable = false)
    @Builder.Default
    private boolean emergencySharingEnabled = false;

    @Column(name = "profile_discoverable", nullable = false)
    @Builder.Default
    private boolean profileDiscoverable = true;

    // Profile photo review
    @Column(name = "profile_photo_review_status", nullable = false, length = 24)
    @Builder.Default
    private String profilePhotoReviewStatus = "NOT_SUBMITTED";

    @Column(name = "profile_photo_submitted_at")
    private LocalDateTime profilePhotoSubmittedAt;

    @Column(name = "profile_photo_reviewed_at")
    private LocalDateTime profilePhotoReviewedAt;

    @Column(name = "profile_photo_review_note", length = 250)
    private String profilePhotoReviewNote;

    // Optional selfie submitted for manual moderation
    @JsonIgnore
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "selfie_photo", columnDefinition = "LONGBLOB")
    private byte[] selfiePhoto;

    @Column(name = "selfie_content_type", length = 40)
    private String selfieContentType;

    @Column(name = "selfie_review_status", nullable = false, length = 24)
    @Builder.Default
    private String selfieReviewStatus = "NOT_SUBMITTED";

    @Column(name = "selfie_submitted_at")
    private LocalDateTime selfieSubmittedAt;

    @Column(name = "selfie_reviewed_at")
    private LocalDateTime selfieReviewedAt;

    @Column(name = "selfie_review_note", length = 250)
    private String selfieReviewNote;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}