package com.pvp.travelmatch.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url:https://tripmatch.fun}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String message = exception.getMessage() == null
                ? "Social login failed"
                : exception.getMessage();

        response.sendRedirect(
                frontendUrl.replaceAll("/$", "")
                        + "/login?oauth2Error="
                        + URLEncoder.encode(message, StandardCharsets.UTF_8)
        );
    }
}
