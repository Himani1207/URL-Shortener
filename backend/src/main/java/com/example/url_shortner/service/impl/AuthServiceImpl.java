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
import com.example.url_shortner.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 * <p><b>The authentication flow itself is unchanged</b> — same BCrypt comparison,
 * same JWT issuance, same endpoints. What changed:
 * <ul>
 *   <li>Bare {@code RuntimeException}s became typed exceptions, so the global handler
 *       can return 409 / 401 / 404 instead of a blanket 500.</li>
 *   <li>Login now fails identically for an unknown email and a wrong password. The
 *       previous code threw "User not found" for one and "Invalid credentials" for
 *       the other, which let anyone probe the endpoint to discover which email
 *       addresses have accounts.</li>
 *   <li>Email is normalised to lowercase before storage and lookup, so
 *       {@code Kunal@x.com} and {@code kunal@x.com} cannot become two accounts that
 *       then collide on the unique index.</li>
 *   <li>SLF4J logging on both paths.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalise(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected, email already in use");
            throw new EmailAlreadyExistsException("An account with that email already exists");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        log.info("User registered [userId={}]", user.getId());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = normalise(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: no account for the supplied email");
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: password mismatch [userId={}]", user.getId());
            // Same message and status as the branch above - see the class javadoc.
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User login successful [userId={}]", user.getId());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserDetails userDetails) {

        if (userDetails == null) {
            throw new InvalidCredentialsException("Authentication required");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Account no longer exists"));

        return toUserResponse(user);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user.getEmail()))
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /** Emails are case-insensitive in practice; the unique index is not. */
    private String normalise(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
