package com.pvp.travelmatch.security;

import com.pvp.travelmatch.dto.AuthResponse;
import com.pvp.travelmatch.service.SocialAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialAuthService socialAuthService;

    @Value("${app.frontend-url:https://tripmatch.fun}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        try {
            AuthResponse auth = socialAuthService.authenticate(oauthUser, provider);

            String fragment =
                    "token=" + encode(auth.getToken())
                    + "&userId=" + encode(String.valueOf(auth.getUserId()))
                    + "&name=" + encode(auth.getName())
                    + "&role=" + encode(auth.getRole())
                    + "&requiresOnboarding=" + encode(String.valueOf(auth.isRequiresOnboarding()));

            response.sendRedirect(
                    frontendUrl.replaceAll("/$", "")
                            + "/oauth2/callback#" + fragment
            );

        } catch (Exception ex) {
            String message = ex.getMessage() == null
                    ? "Social login failed"
                    : ex.getMessage();

            response.sendRedirect(
                    frontendUrl.replaceAll("/$", "")
                            + "/login?oauth2Error=" + encode(message)
            );
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
