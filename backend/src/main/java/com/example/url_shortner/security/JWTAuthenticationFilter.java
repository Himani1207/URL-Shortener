package com.example.url_shortner.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the bearer token on each request and populates the security context.
 *
 * <p><b>The authentication flow is unchanged.</b> Same header, same token, same
 * {@code UserDetails} lookup, same {@code UsernamePasswordAuthenticationToken}. One
 * bug is fixed and one optimisation added:
 *
 * <ul>
 *   <li><b>Bug:</b> {@code jwtService.extractUsername(jwt)} was called unguarded.
 *       jjwt throws on an expired, tampered or malformed token, and an exception
 *       thrown from a servlet filter never reaches {@code @RestControllerAdvice} —
 *       it escapes the whole MVC stack. Every request carrying a merely <i>expired</i>
 *       token therefore produced HTTP 500 with a stack trace instead of 401. The
 *       token failure is now caught, the request continues unauthenticated, and
 *       {@link JwtAuthenticationEntryPoint} renders a proper 401.</li>
 *   <li><b>Optimisation:</b> {@link #shouldNotFilter} skips public routes, so
 *       anonymous redirects and the login endpoint no longer perform a pointless
 *       token parse.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.trace("Authenticated request [user={}, path={}]",
                            userEmail, request.getRequestURI());
                } else {
                    log.debug("Token rejected as invalid [path={}]", request.getRequestURI());
                }
            }

        } catch (ExpiredJwtException ex) {
            // Routine: tokens expire. Logged at DEBUG so it does not pollute the logs.
            log.debug("Expired token presented [path={}]", request.getRequestURI());

        } catch (JwtException | IllegalArgumentException ex) {
            // Malformed or tampered token — worth a WARN, but still just a 401.
            log.warn("Malformed or invalid token [path={}]: {}",
                    request.getRequestURI(), ex.getMessage());

        } catch (UsernameNotFoundException ex) {
            log.warn("Token valid but the account no longer exists [path={}]",
                    request.getRequestURI());
        }

        // Always continue. Leaving the context unauthenticated lets the security
        // chain decide the outcome, which is how a 401 gets produced.
        filterChain.doFilter(request, response);
    }

    /**
     * Skips token processing for endpoints that are public anyway.
     *
     * <p><b>Only the two genuinely anonymous auth endpoints are listed.</b> A blanket
     * {@code /api/auth/} prefix would also cover {@code /api/auth/me}, which
     * <i>requires</i> authentication — skipping the filter there leaves the security
     * context empty, so the endpoint answers 401 no matter how valid the token is.
     * The visible symptom is being signed out on every page refresh, because the
     * client uses {@code /me} to validate a restored token.
     *
     * <p><b>Path is taken from {@code getRequestURI()}, not {@code getServletPath()}.</b>
     * Under Tomcat with the app mapped at "/", the servlet path is the full path, but
     * MockMvc leaves it empty — so a rule written against {@code getServletPath()}
     * behaves differently in tests than in production and hides exactly this class of
     * bug. The request URI is consistent across both.
     *
     * <p>The root redirect is matched by shape rather than by prefix: a single path
     * segment drawn from the short-code alphabet. Paths containing a dot — like
     * {@code /favicon.ico} — deliberately do not match.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        String path = requestPath(request);

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.matches("^/[A-Za-z0-9_-]{3,50}$");
    }

    /** Request URI with any deployment context path stripped off. */
    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
