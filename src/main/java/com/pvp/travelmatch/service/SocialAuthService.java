package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.AuthResponse;
import com.pvp.travelmatch.entity.AccountStatus;
import com.pvp.travelmatch.entity.Role;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.UserRepository;
import com.pvp.travelmatch.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdminEmailService adminEmailService;

    public AuthResponse authenticate(OAuth2User oauthUser, String provider) {

        String email = oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "The " + provider + " account did not provide an email address."
            );
        }

        String name = oauthUser.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .name(name)
                    .email(email)
                    // Social users do not use this password for login.
                    // A random BCrypt password prevents a usable plaintext/
                    // predictable password from being stored.
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .createdAt(LocalDateTime.now())
                    .verified(true)
                    .termsAccepted(false)
                    .termsAcceptedAt(null)
                    .termsVersion(null)
                    .role(Role.USER)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
        } else {
            // The provider has authenticated ownership of this email.
            // Existing local accounts can therefore use social login too.
            user.setVerified(true);

            if (user.getAccountStatus() == null) {
                user.setAccountStatus(AccountStatus.ACTIVE);
            }
        }

        AccountStatus status = user.getAccountStatus() == null
                ? AccountStatus.ACTIVE
                : user.getAccountStatus();

        if (status == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Your account has been suspended");
        }

        if (status == AccountStatus.DEACTIVATED) {
            throw new IllegalStateException("Your account has been deactivated");
        }

        if (adminEmailService.isAdminEmail(user.getEmail())) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.USER);
        }

        User saved = userRepository.save(user);

        Role role = saved.getRole() == null ? Role.USER : saved.getRole();
        String token = jwtService.generateToken(saved.getEmail(), role.name());

        boolean requiresOnboarding =
                Boolean.FALSE.equals(saved.getTermsAccepted());

        return new AuthResponse(
                token,
                saved.getId(),
                saved.getName(),
                role.name(),
                requiresOnboarding
        );
    }
}
