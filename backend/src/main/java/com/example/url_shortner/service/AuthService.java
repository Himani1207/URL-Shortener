package com.example.url_shortner.service;

import com.example.url_shortner.dto.request.LoginRequest;
import com.example.url_shortner.dto.request.RegisterRequest;
import com.example.url_shortner.dto.response.AuthResponse;
import com.example.url_shortner.dto.response.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Returns the authenticated caller's own profile.
     *
     * <p>Added for the dashboard header and Settings page, and so the frontend can
     * validate a token it restored from storage on page load — the alternative is
     * rendering a signed-in shell and only discovering the token is stale when the
     * first data request 401s.
     */
    UserResponse getCurrentUser(UserDetails userDetails);
}
