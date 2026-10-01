package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.*;
import com.pvp.travelmatch.service.TrustCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

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
    @PostMapping(value = "/selfie-review", consumes = "multipart/form-data")
    public Map<String, String> submitSelfieReview(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image first.");
        if (file.getSize() > 5L * 1024 * 1024) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Selfie image must be 5 MB or smaller.");
        String type = file.getContentType();
        byte[] bytes = file.getBytes();
        if (!isSupportedImage(type, bytes)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a valid JPEG, PNG, or WebP image.");
        return Map.of("message", service.submitSelfieForReview(bytes, type));
    }

    private boolean isSupportedImage(String type, byte[] b) {
        if (type == null || b == null) return false;
        if ("image/jpeg".equalsIgnoreCase(type)) return b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
        if ("image/png".equalsIgnoreCase(type)) return b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47;
        if ("image/webp".equalsIgnoreCase(type)) return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return false;
    }

    @PostMapping("/identity/start") public Map<String, String> startIdentity() { return Map.of("message", service.startIdentityVerification()); }
    @PostMapping("/selfie/start") public Map<String, String> startSelfie() { return Map.of("message", service.startSelfieVerification()); }
    @PostMapping("/safety-circle") @ResponseStatus(HttpStatus.CREATED)
    public TrustCenterResponse.SafetyContactResponse addContact(@Valid @RequestBody SafetyCircleContactRequest request) { return service.addContact(request); }
    @DeleteMapping("/safety-circle/{contactId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@PathVariable Long contactId) { service.deleteContact(contactId); }
    @PutMapping("/privacy") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePrivacy(@RequestBody TrustPrivacyRequest request) { service.updatePrivacy(request); }
}
