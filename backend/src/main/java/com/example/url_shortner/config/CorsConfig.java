package com.example.url_shortner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cross-origin rules for the React client.
 *
 * <p><b>Why this is needed:</b> the frontend is served from its own origin — Vite on
 * {@code :5173} in development, a static host in production — while the API lives
 * on {@code :8080}. Without an explicit CORS policy the browser blocks every
 * request before it is even sent, and no amount of correct backend code helps.
 *
 * <p><b>Where it plugs in:</b> {@code SecurityConfig} references this bean via
 * {@code http.cors(...)}. Spring Security has to own the CORS handling, because its
 * filter chain runs before the MVC layer and would otherwise reject the unauthenticated
 * preflight {@code OPTIONS} request with a 401.
 *
 * <p><b>Why the origin list is explicit rather than {@code "*"}:</b> credentials are
 * allowed, and the CORS spec forbids pairing a wildcard origin with credentials.
 * More importantly, a wildcard would let any site on the internet drive this API
 * using a victim's stored token.
 */
@Slf4j
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // allowedOriginPatterns rather than allowedOrigins so entries may contain
        // wildcards, e.g. http://localhost:* while trying different dev ports.
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));

        // Without this the browser hides these from JavaScript: Location is set on
        // create, Content-Disposition on the QR download.
        configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        // Cache preflight for an hour so the browser stops re-asking on every call.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configured for origins: {}", allowedOrigins);
        return source;
    }
}
