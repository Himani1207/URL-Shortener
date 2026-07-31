package com.example.url_shortner.integration;

import com.example.url_shortner.dto.request.LoginRequest;
import com.example.url_shortner.dto.request.RegisterRequest;
import com.example.url_shortner.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for registration and login through the real filter chain.
 *
 * <p>Unlike the service unit tests, these exercise the whole stack: JSON binding,
 * bean validation, the security filter chain and the exception handler. That is
 * where the status codes actually get decided, and where the old code's blanket
 * HTTP 500 for a duplicate email came from.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth API")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private static final String EMAIL = "integration@example.com";
    private static final String PASSWORD = "S3curePassw0rd!";

    @BeforeEach
    void cleanSlate() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register creates the account and returns a token")
    void registerReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registerRequest("Kunal", EMAIL, PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.name").value("Kunal"))
                // The hash must never appear in a response body.
                .andExpect(jsonPath("$.user.password").doesNotExist());

        assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("the stored password is hashed, not the raw value")
    void passwordIsHashedAtRest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registerRequest("Kunal", EMAIL, PASSWORD))))
                .andExpect(status().isCreated());

        String stored = userRepository.findByEmail(EMAIL).orElseThrow().getPassword();

        assertThat(stored).isNotEqualTo(PASSWORD);
        assertThat(stored).startsWith("$2a$");   // BCrypt marker
    }

    @Test
    @DisplayName("a duplicate email returns 409, not 500")
    void duplicateEmailReturnsConflict() throws Exception {
        String body = objectMapper.writeValueAsString(registerRequest("Kunal", EMAIL, PASSWORD));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Before the typed exceptions existed this produced a bare 500.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("invalid registration input returns 400 with per-field messages")
    void validationFailureReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registerRequest("", "not-an-email", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/login returns a token for correct credentials")
    void loginSucceeds() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    @DisplayName("a wrong password returns 401")
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(EMAIL, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("an unknown email returns the same 401 and message as a wrong password")
    void loginDoesNotRevealAccountExistence() throws Exception {
        register();

        String unknownEmailMessage = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginRequest("nobody@example.com", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongPasswordMessage = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(EMAIL, "wrong"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(unknownEmailMessage).get("message"))
                .isEqualTo(objectMapper.readTree(wrongPasswordMessage).get("message"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns the caller's profile")
    void meReturnsProfile() throws Exception {
        String token = register();

        // servletPath is set explicitly because MockMvc leaves it empty while a
        // real container sets it to the full path. A filter rule written against
        // getServletPath() therefore behaved differently here than in production,
        // which is exactly how a bug that broke /me on every request survived a
        // green test suite. Setting it makes this test represent production.
        mockMvc.perform(get("/api/auth/me")
                        .servletPath("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("Kunal"));
    }

    @Test
    @DisplayName("a session survives repeated /me calls, as a page refresh would make")
    void sessionPersistsAcrossRefreshes() throws Exception {
        String token = register();

        // The frontend calls /me on every page load to validate a restored token.
        // If that ever returns 401 for a valid token, the user is signed out on
        // each refresh.
        for (int refresh = 0; refresh < 3; refresh++) {
            mockMvc.perform(get("/api/auth/me")
                            .servletPath("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }
    }

    @Test
    @DisplayName("GET /api/auth/me without a token returns a JSON 401")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("a malformed token returns 401 rather than 500")
    void malformedTokenReturnsUnauthorized() throws Exception {
        // The regression guard for the filter-chain bug: an exception thrown inside
        // a servlet filter never reaches @RestControllerAdvice, so an unguarded
        // parse turned every bad token into a 500.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------

    private String register() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registerRequest("Kunal", EMAIL, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private RegisterRequest registerRequest(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
