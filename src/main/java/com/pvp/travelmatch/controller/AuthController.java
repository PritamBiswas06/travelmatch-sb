package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.AuthResponse;
import com.pvp.travelmatch.dto.LoginRequest;
import com.pvp.travelmatch.entity.AccountStatus;
import com.pvp.travelmatch.entity.Role;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.UserRepository;
import com.pvp.travelmatch.security.JwtService;
import com.pvp.travelmatch.service.AdminEmailService;
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

    /*
     * Determines whether an email is configured as an administrator
     * through TRAVELMATCH_ADMIN_EMAILS.
     */
    private final AdminEmailService adminEmailService;


    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {

        Optional<User> existingUser =
                userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {

            User dbUser = existingUser.get();

            /*
             * If the email is already verified, don't allow
             * another registration.
             */
            if (Boolean.TRUE.equals(dbUser.getVerified())) {

                throw new RuntimeException(
                        "Email already registered"
                );
            }

            /*
             * Existing unverified account:
             * generate a new OTP.
             */
            dbUser.setVerificationCode(generateOTP());
            dbUser.setCodeExpiry(
                    LocalDateTime.now().plusMinutes(10)
            );

            userRepository.save(dbUser);

            emailService.sendOtpEmail(
                    dbUser.getEmail(),
                    dbUser.getVerificationCode()
            );

            return Map.of(
                    "status",
                    "otp_resent",

                    "message",
                    "Verification code resent to your email"
            );
        }


        // =====================================================
        // SECURITY
        // =====================================================

        /*
         * NEVER trust role/accountStatus coming from the frontend.
         *
         * Every newly registered user starts as USER.
         *
         * Admin privileges are assigned from the controlled
         * TRAVELMATCH_ADMIN_EMAILS environment variable.
         */
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);


        // =====================================================
        // PASSWORD
        // =====================================================

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        user.setCreatedAt(
                LocalDateTime.now()
        );


        // =====================================================
        // EMAIL VERIFICATION
        // =====================================================

        String otp = generateOTP();

        user.setVerificationCode(otp);

        user.setCodeExpiry(
                LocalDateTime.now().plusMinutes(10)
        );

        user.setVerified(false);


        // =====================================================
        // SAVE USER
        // =====================================================

        userRepository.save(user);


        // =====================================================
        // EMAIL
        // =====================================================

        String htmlEmail = """
                <html>
                <body style="font-family:Arial;background:#f4f6fb;padding:30px;">

                <div style="
                    max-width:600px;
                    margin:auto;
                    background:white;
                    border-radius:12px;
                    box-shadow:0 10px 40px rgba(0,0,0,0.1);
                    overflow:hidden;
                ">

                    <div style="
                        background:#0d78e3;
                        color:white;
                        padding:20px;
                        text-align:center;
                        font-size:22px;
                    ">
                        ✈ TravelMatch
                    </div>

                    <div style="
                        padding:30px;
                        text-align:center;
                    ">

                        <h2>Email Verification</h2>

                        <p>
                            Hello <b>%s</b>,
                        </p>

                        <p>
                            To complete your TravelMatch registration,
                            please verify your email.
                        </p>

                        <div style="
                            margin:25px 0;
                            padding:25px;
                            background:#f7f9ff;
                            border-radius:10px;
                            font-size:28px;
                            font-weight:bold;
                            letter-spacing:4px;
                            color:#0d78e3;
                        ">
                            %s
                        </div>

                        <p style="color:#666;">
                            This code will expire in
                            <b>10 minutes</b>.
                        </p>

                        <p style="
                            margin-top:25px;
                            font-size:13px;
                            color:#888;
                        ">
                            If you didn't request this,
                            please ignore this email.
                        </p>

                    </div>

                </div>

                </body>
                </html>
                """
                .formatted(
                        user.getName(),
                        otp
                );


        emailService.sendHtmlEmail(
                user.getEmail(),
                "Verify your TravelMatch account",
                htmlEmail
        );


        return Map.of(
                "status",
                "success",

                "message",
                "Verification code sent to your email"
        );
    }


    // =========================================================
    // OTP
    // =========================================================

    private String generateOTP() {

        return String.valueOf(
                new Random().nextInt(900000) + 100000
        );
    }


    // =========================================================
    // RESEND OTP
    // =========================================================

    @PostMapping("/resend-otp")
    public Map<String, String> resendOtp(
            @RequestParam String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        String otp = generateOTP();

        user.setVerificationCode(otp);

        user.setCodeExpiry(
                LocalDateTime.now().plusMinutes(10)
        );

        userRepository.save(user);


        emailService.sendOtpEmail(
                user.getEmail(),
                otp
        );


        return Map.of(
                "status",
                "success",

                "message",
                "OTP sent again"
        );
    }


    // =========================================================
    // VERIFY EMAIL
    // =========================================================

    @PostMapping("/verify")
    public Map<String, String> verifyEmail(
            @RequestParam String email,
            @RequestParam String code) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        if (
                user.getVerificationCode() == null
                        ||
                        !user.getVerificationCode().equals(code)
        ) {

            throw new RuntimeException(
                    "Invalid verification code"
            );
        }


        if (
                user.getCodeExpiry() == null
                        ||
                        user.getCodeExpiry()
                                .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Verification code expired"
            );
        }


        user.setVerified(true);

        user.setVerificationCode(null);

        user.setCodeExpiry(null);

        userRepository.save(user);


        return Map.of(
                "status",
                "success",

                "message",
                "Email verified successfully"
        );
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request) {


        // -----------------------------------------------------
        // Find user
        // -----------------------------------------------------

        User user =
                userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Email not registered"
                                )
                        );


        // -----------------------------------------------------
        // Email verification
        // -----------------------------------------------------

        if (
                Boolean.FALSE.equals(
                        user.getVerified()
                )
        ) {

            throw new RuntimeException(
                    "Please verify your email first"
            );
        }


        // -----------------------------------------------------
        // Password
        // -----------------------------------------------------

        if (
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                )
        ) {

            throw new RuntimeException(
                    "Incorrect password"
            );
        }


        // -----------------------------------------------------
        // Account status
        // -----------------------------------------------------

        AccountStatus status =
                user.getAccountStatus() == null
                        ? AccountStatus.ACTIVE
                        : user.getAccountStatus();


        if (status == AccountStatus.SUSPENDED) {

            throw new RuntimeException(
                    "Your account has been suspended"
            );
        }


        if (status == AccountStatus.DEACTIVATED) {

            throw new RuntimeException(
                    "Your account has been deactivated"
            );
        }


        // =====================================================
        // ADMIN ROLE SYNCHRONIZATION
        // =====================================================

        /*
         * The backend decides the role.
         *
         * Example Railway variable:
         *
         * TRAVELMATCH_ADMIN_EMAILS=
         * your@email.com,admin@email.com
         *
         * If the logged-in email is present:
         *
         *      ADMIN
         *
         * Otherwise:
         *
         *      USER
         */

        if (
                adminEmailService.isAdminEmail(
                        user.getEmail()
                )
        ) {

            user.setRole(Role.ADMIN);

        } else {

            user.setRole(Role.USER);
        }


        /*
         * Save the synchronized role to the database.
         *
         * This fixes existing accounts as well.
         */
        userRepository.save(user);


        // -----------------------------------------------------
        // Generate JWT
        // -----------------------------------------------------

        Role role =
                user.getRole() == null
                        ? Role.USER
                        : user.getRole();


        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        role.name()
                );


        // -----------------------------------------------------
        // Response
        // -----------------------------------------------------

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                role.name()
        );
    }


    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(
            @RequestParam String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Email not registered"
                                )
                        );


        String otp = generateOTP();


        user.setResetCode(otp);

        user.setResetCodeExpiry(
                LocalDateTime.now().plusMinutes(10)
        );


        userRepository.save(user);


        emailService.sendOtpEmail(
                email,
                otp
        );


        return Map.of(
                "message",
                "Password reset code sent to email"
        );
    }


    // =========================================================
    // VERIFY RESET CODE
    // =========================================================

    @PostMapping("/verify-reset")
    public Map<String, String> verifyResetCode(
            @RequestParam String email,
            @RequestParam String code) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        if (
                !code.equals(
                        user.getResetCode()
                )
        ) {

            throw new RuntimeException(
                    "Invalid code"
            );
        }


        if (
                user.getResetCodeExpiry() == null
                        ||
                        user.getResetCodeExpiry()
                                .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Code expired"
            );
        }


        return Map.of(
                "message",
                "Code verified"
        );
    }


    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @RequestParam String email,
            @RequestParam String password) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        user.setPassword(
                passwordEncoder.encode(password)
        );


        user.setResetCode(null);

        user.setResetCodeExpiry(null);


        userRepository.save(user);


        return Map.of(
                "message",
                "Password updated successfully"
        );
    }
}