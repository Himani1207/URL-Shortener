package com.example.url_shortner.config;

import com.example.url_shortner.security.JWTAuthenticationFilter;
import com.example.url_shortner.security.JwtAccessDeniedHandler;
import com.example.url_shortner.security.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * HTTP security rules.
 *
 * <p>The authentication mechanism is untouched — stateless JWT, same provider, same
 * filter position. What changed, and why:
 *
 * <ul>
 *   <li><b>CORS is enabled.</b> Without it the browser blocks every call from the
 *       React client. It must be configured on the security chain, not just on MVC,
 *       because the security filters run first and would reject the unauthenticated
 *       preflight {@code OPTIONS} with a 401.</li>
 *   <li><b>The {@code /{shortCode}} rule now has a controller behind it.</b> The rule
 *       was already here; {@code RedirectController} is the route it was written for.
 *       It is narrowed to the short-code alphabet so it does not blanket-permit every
 *       single-segment path.</li>
 *   <li><b>Swagger and the health probe are permitted</b>, otherwise the API docs
 *       are unreachable and container health checks fail closed.</li>
 *   <li><b>Explicit 401/403 handlers</b>, so authentication failures return the same
 *       JSON error shape as everything else.</li>
 * </ul>
 *
 * <p><b>Deliberately left authenticated:</b> {@code GET /api/urls/{shortCode}}, the
 * legacy redirect path. Permitting it would need a pattern like {@code /api/urls/*},
 * which also matches {@code /api/urls/stats} and would expose that endpoint
 * anonymously. Public redirects belong on the root route, which is what actually
 * gets shared.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Safe to disable: the API is stateless and authenticated by a bearer
                // token, never by an ambiently-sent cookie, so there is no CSRF vector.
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        // Preflight must never require credentials.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Unlocking a protected link is done by the person the link
                        // was shared with, who has no account. Scoped to one exact
                        // path so nothing else becomes anonymous by association.
                        .requestMatchers(HttpMethod.POST, "/api/public/links/*/unlock").permitAll()

                        // Public redirect. Constrained to the short-code alphabet so
                        // it cannot shadow static resources or documentation routes.
                        .requestMatchers(HttpMethod.GET, "/{shortCode:[A-Za-z0-9_-]{3,50}}").permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
