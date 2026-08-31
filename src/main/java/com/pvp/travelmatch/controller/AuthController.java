package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.AuthResponse;
import com.pvp.travelmatch.dto.LoginRequest;
import com.pvp.travelmatch.entity.AccountStatus;
import com.pvp.travelmatch.entity.Role;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.UserRepository;
import com.pvp.travelmatch.security.JwtService;
import com.pvp.travelmatch.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {
            User dbUser = existingUser.get();
            if (Boolean.TRUE.equals(dbUser.getVerified())) {
                throw new RuntimeException("Email already registered");
            }

            dbUser.setVerificationCode(generateOTP());
            dbUser.setCodeExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(dbUser);
            emailService.sendOtpEmail(dbUser.getEmail(), dbUser.getVerificationCode());

            return Map.of("status", "otp_resent", "message", "Verification code resent to your email");
        }

        // SECURITY: registration can never create an administrator account.
        // Role promotion must happen through the protected admin workflow or a
        // controlled database bootstrap operation.
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        String otp = generateOTP();
        user.setVerificationCode(otp);
        user.setCodeExpiry(LocalDateTime.now().plusMinutes(10));
        user.setVerified(false);

        userRepository.save(user);

        String htmlEmail = """
<html><body style="font-family:Arial;background:#f4f6fb;padding:30px;">
<div style="max-width:600px;margin:auto;background:white;border-radius:12px;box-shadow:0 10px 40px rgba(0,0,0,0.1);overflow:hidden;">
<div style="background:#0d78e3;color:white;padding:20px;text-align:center;font-size:22px;">✈ TravelMatch</div>
<div style="padding:30px;text-align:center;">
<h2>Email Verification</h2><p>Hello <b>%s</b>,</p>
<p>To complete your TravelMatch registration, please verify your email.</p>
<div style="margin:25px 0;padding:25px;background:#f7f9ff;border-radius:10px;font-size:28px;font-weight:bold;letter-spacing:4px;color:#0d78e3;">%s</div>
<p style="color:#666;">This code will expire in <b>10 minutes</b>.</p>
<p style="margin-top:25px;font-size:13px;color:#888;">If you didn't request this, please ignore this email.</p>
</div></div></body></html>
""".formatted(user.getName(), otp);

        emailService.sendHtmlEmail(user.getEmail(), "Verify your TravelMatch account", htmlEmail);

        return Map.of("status", "success", "message", "Verification code sent to your email");
    }

    private String generateOTP() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    @PostMapping("/resend-otp")
    public Map<String, String> resendOtp(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOTP();
        user.setVerificationCode(otp);
        user.setCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), otp);

        return Map.of("status", "success", "message", "OTP sent again");
    }

    @PostMapping("/verify")
    public Map<String, String> verifyEmail(@RequestParam String email, @RequestParam String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }
        if (user.getCodeExpiry() == null || user.getCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setCodeExpiry(null);
        userRepository.save(user);

        return Map.of("status", "success", "message", "Email verified successfully");
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not registered"));

        if (Boolean.FALSE.equals(user.getVerified())) {
            throw new RuntimeException("Please verify your email first");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        AccountStatus status = user.getAccountStatus() == null ? AccountStatus.ACTIVE : user.getAccountStatus();
        if (status == AccountStatus.SUSPENDED) throw new RuntimeException("Your account has been suspended");
        if (status == AccountStatus.DEACTIVATED) throw new RuntimeException("Your account has been deactivated");

        Role role = user.getRole() == null ? Role.USER : user.getRole();
        String token = jwtService.generateToken(user.getEmail(), role.name());

        return new AuthResponse(token, user.getId(), user.getName(), role.name());
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not registered"));
        String otp = generateOTP();
        user.setResetCode(otp);
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendOtpEmail(email, otp);
        return Map.of("message", "Password reset code sent to email");
    }

    @PostMapping("/verify-reset")
    public Map<String, String> verifyResetCode(@RequestParam String email, @RequestParam String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!code.equals(user.getResetCode())) throw new RuntimeException("Invalid code");
        if (user.getResetCodeExpiry() == null || user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expired");
        }
        return Map.of("message", "Code verified");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestParam String email, @RequestParam String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(password));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepository.save(user);
        return Map.of("message", "Password updated successfully");
    }
}
