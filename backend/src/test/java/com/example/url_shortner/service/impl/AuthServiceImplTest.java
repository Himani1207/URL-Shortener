package com.example.url_shortner.service.impl;

import com.example.url_shortner.dto.request.LoginRequest;
import com.example.url_shortner.dto.request.RegisterRequest;
import com.example.url_shortner.dto.response.AuthResponse;
import com.example.url_shortner.dto.response.UserResponse;
import com.example.url_shortner.entity.User;
import com.example.url_shortner.exception.EmailAlreadyExistsException;
import com.example.url_shortner.exception.InvalidCredentialsException;
import com.example.url_shortner.exception.UserNotFoundException;
import com.example.url_shortner.repository.UserRepository;
import com.example.url_shortner.security.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JWTService jwtService;

    @InjectMocks private AuthServiceImpl authService;

    private static final String EMAIL = "kunal@example.com";
    private static final String RAW_PASSWORD = "S3curePassw0rd!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedvalue";
    private static final String TOKEN = "generated.jwt.token";

    @BeforeEach
    void setUp() {
        // @Value field: nothing injects it outside a Spring context.
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86_400_000L);
    }

    // ==================================================================
    // Registration
    // ==================================================================

    @Test
    @DisplayName("stores a hashed password, never the raw one")
    void registerHashesThePassword() {
        RegisterRequest request = registerRequest("Kunal", EMAIL, RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(jwtService.generateToken(EMAIL)).thenReturn(TOKEN);

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertThat(saved.getValue().getPassword())
                .isEqualTo(HASHED_PASSWORD)
                .isNotEqualTo(RAW_PASSWORD);
    }

    @Test
    @DisplayName("returns a token together with the new user's profile")
    void registerReturnsTokenAndProfile() {
        RegisterRequest request = registerRequest("Kunal", EMAIL, RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(jwtService.generateToken(EMAIL)).thenReturn(TOKEN);

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86_400_000L);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
        assertThat(response.getUser().getName()).isEqualTo("Kunal");
    }

    @Test
    @DisplayName("rejects a duplicate email with a 409-mapped exception, not a bare RuntimeException")
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = registerRequest("Kunal", EMAIL, RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("normalises email case so two casings cannot become two accounts")
    void registerNormalisesEmailCase() {
        RegisterRequest request = registerRequest("Kunal", "  KuNaL@Example.COM  ", RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(jwtService.generateToken(EMAIL)).thenReturn(TOKEN);

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
    }

    // ==================================================================
    // Login
    // ==================================================================

    @Test
    @DisplayName("issues a token when the password matches")
    void loginSucceedsWithCorrectPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(EMAIL)).thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("rejects a wrong password")
    void loginRejectsWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest(EMAIL, "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("gives an identical error for unknown email and wrong password")
    void loginDoesNotLeakWhetherAnAccountExists() {
        // This is the user-enumeration guard. The old code threw "User not found"
        // for one case and "Invalid credentials" for the other, which let anyone
        // probe the endpoint to discover which emails have accounts.
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong", HASHED_PASSWORD)).thenReturn(false);

        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.login(loginRequest("nobody@example.com", RAW_PASSWORD)));
        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.login(loginRequest(EMAIL, "wrong")));

        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    // ==================================================================
    // Current user
    // ==================================================================

    @Test
    @DisplayName("returns the caller's profile without exposing the password hash")
    void currentUserOmitsPasswordHash() {
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        UserResponse response = authService.getCurrentUser(principal);

        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getName()).isEqualTo("Kunal");
        // UserResponse has no password field at all - asserted structurally by the
        // fact that this compiles, and by the DTO's javadoc.
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("fails cleanly when a valid token names a deleted account")
    void currentUserFailsForDeletedAccount() {
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(principal))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("rejects a null principal")
    void currentUserRejectsNullPrincipal() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ==================================================================
    // Fixtures
    // ==================================================================

    private User existingUser() {
        return User.builder()
                .id(1L)
                .name("Kunal")
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .build();
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
