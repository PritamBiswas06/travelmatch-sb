package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.service.TrustCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trust")
@RequiredArgsConstructor
public class TrustCenterController {
    private final TrustCenterService service;

    @GetMapping("/me") public TrustCenterResponse getMine() { return service.getMine(); }
    @PostMapping("/phone/start") @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> startPhone(@Valid @RequestBody PhoneStartRequest request) {
        service.startPhoneVerification(request); return Map.of("message", "Verification code sent.");
    }
    @PostMapping("/phone/verify")
    public Map<String, String> verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        service.verifyPhone(request); return Map.of("message", "Phone number verified.");
    }
    @PostMapping("/photo-review/start")
    public Map<String, String> submitPhotoForReview() { return Map.of("message", service.submitProfilePhotoForReview()); }
    @PostMapping("/identity/start") public Map<String, String> startIdentity() { return Map.of("message", service.startIdentityVerification()); }
    @PostMapping("/selfie/start") public Map<String, String> startSelfie() { return Map.of("message", service.startSelfieVerification()); }
    @PostMapping("/safety-circle") @ResponseStatus(HttpStatus.CREATED)
    public TrustCenterResponse.SafetyContactResponse addContact(@Valid @RequestBody SafetyCircleContactRequest request) { return service.addContact(request); }
    @DeleteMapping("/safety-circle/{contactId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@PathVariable Long contactId) { service.deleteContact(contactId); }
    @PutMapping("/privacy") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePrivacy(@RequestBody TrustPrivacyRequest request) { service.updatePrivacy(request); }
}
