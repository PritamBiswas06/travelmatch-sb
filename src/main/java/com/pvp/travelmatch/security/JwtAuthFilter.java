package com.pvp.travelmatch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authentication endpoints don't need JWT
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        System.out.println(
                "JWT DEBUG | " +
                        request.getMethod() +
                        " " +
                        request.getRequestURI() +
                        " | Authorization present = " +
                        (authHeader != null)
        );

        // No Authorization header
//        if (authHeader == null || authHeader.trim().isEmpty()) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JWT DEBUG | Authorization header missing");
            filterChain.doFilter(request, response);
            return;
        }

        // Wrong authentication scheme
        if (!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String email = jwtService.extractEmail(token);

            if (email != null && !email.isBlank()) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception ex) {

            // Invalid/expired JWT.
            // Do not create an authentication.
            SecurityContextHolder.clearContext();

            System.out.println(
                    "JWT authentication failed: "
                            + ex.getMessage()
            );
        }

        filterChain.doFilter(request, response);

    }
}