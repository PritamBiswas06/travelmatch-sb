package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.entity.SafetyCircleContact;
import com.pvp.travelmatch.entity.TrustVerification;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.SafetyCircleContactRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.TrustVerificationRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrustCenterService {
    private final UserRepository users;
    private final TrustVerificationRepository verifications;
    private final SafetyCircleContactRepository contacts;
    private final TravelPlanRepository plans;
    private final TravelerReviewService reviews;
    private final TrustSmsService sms;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${travelmatch.identity.provider-url:}") private String identityProviderUrl;
    @Value("${travelmatch.selfie.provider-url:}") private String selfieProviderUrl;

    @Transactional(readOnly = true)
    public TrustCenterResponse getMine() {
        User user = currentUser();
        TrustVerification trust = findOrCreateReadSafe(user);
        List<String> badges = new ArrayList<>();
        if (Boolean.TRUE.equals(user.getVerified())) badges.add("EMAIL_VERIFIED");
        if (trust.isPhoneVerified()) badges.add("PHONE_VERIFIED");
        if ("VERIFIED".equals(trust.getIdentityStatus())) badges.add("IDENTITY_VERIFIED");
        if ("VERIFIED".equals(trust.getSelfieStatus())) badges.add("SELFIE_CHECKED");
        if ("APPROVED".equals(trust.getProfilePhotoReviewStatus())) badges.add("PHOTO_REVIEWED");
        if (travelDnaComplete(user)) badges.add("TRAVEL_DNA_COMPLETE");
        int completion = profileCompletion(user);
        if (completion >= 90) badges.add("PROFILE_COMPLETE");
        long completedTrips = plans.countByUserIdAndStatus(user.getId(), "COMPLETED");
        double average = reviews.getAverage(user.getId());
        long reviewCount = reviews.getCount(user.getId());
        if (completedTrips >= 1 && reviewCount >= 3 && average >= 4.0) badges.add("TRAVEL_REPUTATION");
        List<TrustCenterResponse.SafetyContactResponse> circle = contacts.findTop10ByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(c -> TrustCenterResponse.SafetyContactResponse.builder()
                        .id(c.getId()).name(c.getName()).phone(c.getPhone()).email(c.getEmail()).relationship(c.getRelationship()).build()).toList();
        return TrustCenterResponse.builder()
                .emailVerified(Boolean.TRUE.equals(user.getVerified()))
                .phoneVerified(trust.isPhoneVerified()).phoneNumber(maskPhone(trust.getPhoneNumber()))
                .identityStatus(trust.getIdentityStatus()).selfieStatus(trust.getSelfieStatus())
                .profilePhotoReviewStatus(trust.getProfilePhotoReviewStatus())
                .profilePhotoReviewNote(trust.getProfilePhotoReviewNote())
                .profileCompletion(completion).travelDnaComplete(travelDnaComplete(user))
                .completedTrips(completedTrips).averageRating(average).reviewCount(reviewCount)
                .emergencySharingEnabled(trust.isEmergencySharingEnabled()).profileDiscoverable(trust.isProfileDiscoverable())
                .earnedBadges(badges).safetyCircle(circle)
                .identityProviderStatus(blank(identityProviderUrl) ? "NOT_CONFIGURED" : "ADAPTER_REQUIRED")
                .selfieProviderStatus(blank(selfieProviderUrl) ? "NOT_CONFIGURED" : "ADAPTER_REQUIRED")
                .build();
    }

    @Transactional
    public void startPhoneVerification(PhoneStartRequest request) {
        User user = currentUser();
        TrustVerification trust = findOrCreate(user);
        LocalDateTime now = LocalDateTime.now();
        if (trust.getPhoneOtpLastSentAt() != null && trust.getPhoneOtpLastSentAt().isAfter(now.minusSeconds(60))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait at least 60 seconds before requesting another code.");
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        // Send first: do not persist an OTP that the provider failed to deliver.
        sms.sendOtp(request.getPhoneNumber(), code);
        trust.setPhoneNumber(request.getPhoneNumber());
        trust.setPhoneVerified(false);
        trust.setPhoneVerifiedAt(null);
        trust.setPhoneOtpHash(passwordEncoder.encode(code));
        trust.setPhoneOtpExpiry(now.plusMinutes(5));
        trust.setPhoneOtpLastSentAt(now);
        trust.setPhoneOtpAttempts(0);
        verifications.save(trust);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verifyPhone(PhoneVerifyRequest request) {
        TrustVerification trust = findOrCreate(currentUser());
        LocalDateTime now = LocalDateTime.now();
        if (trust.getPhoneOtpHash() == null || trust.getPhoneOtpExpiry() == null || trust.getPhoneOtpExpiry().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired. Request a new code.");
        }
        if (trust.getPhoneOtpAttempts() >= 5) {
            trust.setPhoneOtpHash(null);
            verifications.save(trust);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect attempts. Request a new code.");
        }
        trust.setPhoneOtpAttempts(trust.getPhoneOtpAttempts() + 1);
        if (!passwordEncoder.matches(request.getCode(), trust.getPhoneOtpHash())) {
            verifications.save(trust);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That code is incorrect.");
        }
        trust.setPhoneVerified(true);
        trust.setPhoneVerifiedAt(now);
        trust.setPhoneOtpHash(null);
        trust.setPhoneOtpExpiry(null);
        trust.setPhoneOtpAttempts(0);
        verifications.save(trust);
    }

    @Transactional
    public String submitProfilePhotoForReview() {
        User user = currentUser();
        if (user.getProfilePhoto() == null || user.getProfilePhoto().length == 0
                || user.getProfilePhotoContentType() == null || user.getProfilePhotoContentType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a profile photo before requesting review.");
        }
        TrustVerification trust = findOrCreate(user);
        if ("PENDING".equals(trust.getProfilePhotoReviewStatus())) {
            return "Your profile photo is already waiting for moderator review.";
        }
        trust.setProfilePhotoReviewStatus("PENDING");
        trust.setProfilePhotoSubmittedAt(LocalDateTime.now());
        trust.setProfilePhotoReviewedAt(null);
        trust.setProfilePhotoReviewNote(null);
        verifications.save(trust);
        return "Your profile photo was submitted for manual review.";
    }

    public String startIdentityVerification() {
        if (blank(identityProviderUrl)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Identity verification provider is not configured. No identity badge has been issued.");
        // A provider-specific signed session/callback must be implemented for the selected KYC vendor.
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "The configured identity provider needs its signed session adapter before verification can start.");
    }

    public String startSelfieVerification() {
        if (blank(selfieProviderUrl)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Selfie/liveness provider is not configured. No selfie badge has been issued.");
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "The configured liveness provider needs its signed session adapter before verification can start.");
    }

    @Transactional
    public TrustCenterResponse.SafetyContactResponse addContact(SafetyCircleContactRequest request) {
        User user = currentUser();
        if (contacts.countByOwnerId(user.getId()) >= 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can add up to 10 safety contacts.");
        if ((request.getPhone() == null || request.getPhone().isBlank()) && (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a phone number or email address for this contact.");
        }
        SafetyCircleContact contact = contacts.save(SafetyCircleContact.builder().owner(user).name(request.getName().trim())
                .phone(clean(request.getPhone())).email(clean(request.getEmail())).relationship(clean(request.getRelationship())).build());
        return TrustCenterResponse.SafetyContactResponse.builder().id(contact.getId()).name(contact.getName())
                .phone(contact.getPhone()).email(contact.getEmail()).relationship(contact.getRelationship()).build();
    }

    @Transactional
    public void deleteContact(Long contactId) {
        User user = currentUser();
        SafetyCircleContact contact = contacts.findByIdAndOwnerId(contactId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Safety contact not found."));
        contacts.delete(contact);
    }

    @Transactional
    public void updatePrivacy(TrustPrivacyRequest request) {
        TrustVerification trust = findOrCreate(currentUser());
        trust.setEmergencySharingEnabled(request.isEmergencySharingEnabled());
        trust.setProfileDiscoverable(request.isProfileDiscoverable());
        verifications.save(trust);
    }

    @Transactional(readOnly = true)
    public TrustVerification findForUser(Long userId) { return verifications.findByUserId(userId).orElse(null); }

    private TrustVerification findOrCreateReadSafe(User user) {
        // getMine is a read request; defaults are represented without creating a row.
        return verifications.findByUserId(user.getId()).orElseGet(() -> TrustVerification.builder().user(user).build());
    }
    private TrustVerification findOrCreate(User user) {
        return verifications.findByUserId(user.getId()).orElseGet(() -> verifications.save(TrustVerification.builder().user(user).build()));
    }
    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        return users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
    private int profileCompletion(User u) {
        int total = 12, filled = 0;
        if (present(u.getName())) filled++; if (present(u.getBio())) filled++;
        if (present(u.getCity()) || present(u.getCountry())) filled++;
        if (present(u.getProfilePhotoContentType()) && u.getProfilePhoto() != null) filled++;
        if (present(u.getTravelStyle())) filled++; if (present(u.getTravelInterests())) filled++;
        if (present(u.getLanguages())) filled++; if (present(u.getPreferredDestinations())) filled++;
        if (present(u.getBudgetPreference())) filled++; if (present(u.getTravelFrequency())) filled++;
        if (present(u.getIdealTravelPartner())) filled++; if (present(u.getUsername())) filled++;
        return Math.round((float) filled * 100 / total);
    }
    private boolean travelDnaComplete(User u) {
        return u.getDnaAdventureRelaxation() != null && u.getDnaBudgetLuxury() != null && u.getDnaSunriseNightlife() != null
                && u.getDnaTrekkingSightseeing() != null && u.getDnaFoodCulture() != null && u.getDnaPlannedSpontaneous() != null
                && u.getDnaSoloGroup() != null && u.getDnaNatureCity() != null && u.getDnaPhotographyActivities() != null && u.getDnaFastSlow() != null;
    }
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) return phone;
        return "•".repeat(Math.max(0, phone.length() - 4)) + phone.substring(phone.length() - 4);
    }
    private boolean present(String value) { return value != null && !value.isBlank(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
