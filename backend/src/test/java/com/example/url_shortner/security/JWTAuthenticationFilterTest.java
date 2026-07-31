package com.example.url_shortner.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Covers which requests bypass token processing.
 *
 * <p>Written after a production-only bug: {@code shouldNotFilter} matched the whole
 * {@code /api/auth/} prefix, which silently included {@code /api/auth/me}. That
 * endpoint requires authentication, so skipping the filter left the security
 * context empty and it returned 401 for every request — signing the user out on
 * each page refresh, since the client calls {@code /me} to validate a stored token.
 *
 * <p>The integration tests could not catch it. MockMvc leaves
 * {@code getServletPath()} empty while Tomcat sets it to the full path, so the
 * original rule never matched under test and always matched in production. These
 * tests set the servlet path <i>and</i> the request URI the way a real container
 * does, so the two environments cannot diverge again.
 */
class JWTAuthenticationFilterTest {

    private JWTAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTAuthenticationFilter(mock(JWTService.class), mock(CustomUserDetailsService.class));
    }

    @Test
    @DisplayName("/api/auth/me is filtered, so a valid token can authenticate it")
    void authenticatedProfileEndpointIsFiltered() {
        assertThat(filter.shouldNotFilter(requestFor("/api/auth/me")))
                .as("skipping the filter here makes /me answer 401 for every caller")
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/login", "/api/auth/register"})
    @DisplayName("the anonymous auth endpoints skip the filter")
    void anonymousAuthEndpointsAreSkipped(String path) {
        assertThat(filter.shouldNotFilter(requestFor(path))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/actuator/health",
            "/aB3xY9z",          // root redirect
            "/my-campaign"
    })
    @DisplayName("public routes skip the filter")
    void publicRoutesAreSkipped(String path) {
        assertThat(filter.shouldNotFilter(requestFor(path))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/urls",
            "/api/urls/aB3xY9z/analytics",
            "/api/urls/stats"
    })
    @DisplayName("protected routes are filtered")
    void protectedRoutesAreFiltered(String path) {
        assertThat(filter.shouldNotFilter(requestFor(path))).isFalse();
    }

    @Test
    @DisplayName("a deployment context path is stripped before matching")
    void contextPathIsStripped() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/api/auth/login");
        request.setContextPath("/app");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("the match survives an empty servlet path, as MockMvc produces")
    void matchesRegardlessOfServletPath() {
        // The exact divergence that hid the original bug: MockMvc leaves
        // servletPath empty, Tomcat sets it. Both must give the same answer.
        MockHttpServletRequest tomcatStyle = new MockHttpServletRequest("GET", "/api/auth/me");
        tomcatStyle.setServletPath("/api/auth/me");

        MockHttpServletRequest mockMvcStyle = new MockHttpServletRequest("GET", "/api/auth/me");
        mockMvcStyle.setServletPath("");

        assertThat(filter.shouldNotFilter(tomcatStyle))
                .isEqualTo(filter.shouldNotFilter(mockMvcStyle))
                .isFalse();
    }

    /** Builds a request the way a servlet container would. */
    private MockHttpServletRequest requestFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }
}
