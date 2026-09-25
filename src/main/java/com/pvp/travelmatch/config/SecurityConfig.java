package com.pvp.travelmatch.config;

import com.pvp.travelmatch.security.JwtAuthFilter;
import com.pvp.travelmatch.security.OAuth2FailureHandler;
import com.pvp.travelmatch.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private final OAuth2SuccessHandler oauth2SuccessHandler;

    private final OAuth2FailureHandler oauth2FailureHandler;

    private final Environment environment;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Normal authentication APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // OAuth2 endpoints
                        .requestMatchers("/api/oauth2/**").permitAll()
                        .requestMatchers("/api/login/oauth2/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()

                        // Razorpay webhook
                        .requestMatchers(
                                "/api/monetization/webhook/razorpay"
                        ).permitAll()

                        // Profile photos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/*/photo"
                        ).permitAll()

                        // Admin APIs
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                /*
                 * OAuth2 authorization-code flow requires a temporary
                 * HTTP session during the Google authentication process.
                 *
                 * JWT is still used for normal TravelMatch API authentication.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                );


        /*
         * Enable Google OAuth2 ONLY in production.
         *
         * Local:
         *   No Google OAuth configuration required.
         *
         * Production:
         *   application-prod.properties provides:
         *
         *   GOOGLE_CLIENT_ID
         *   GOOGLE_CLIENT_SECRET
         */
        if (environment.acceptsProfiles(
                Profiles.of("prod")
        )) {

            http.oauth2Login(oauth2 -> oauth2

                    .authorizationEndpoint(endpoint ->
                            endpoint.baseUri(
                                    "/api/oauth2/authorization"
                            )
                    )

                    .redirectionEndpoint(endpoint ->
                            endpoint.baseUri(
                                    "/api/login/oauth2/code/*"
                            )
                    )

                    .successHandler(
                            oauth2SuccessHandler
                    )

                    .failureHandler(
                            oauth2FailureHandler
                    )
            );
        }


        /*
         * JWT authentication remains active for both
         * local and production environments.
         */
        http.addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
        );


        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                List.of(
                        "https://tripmatch.fun",
                        "https://www.tripmatch.fun",

                        // Angular local development
                        "http://localhost:4200",

                        // Capacitor / Android
                        "http://localhost",
                        "https://localhost",
                        "capacitor://localhost"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }


//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
}