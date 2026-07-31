package com.example.url_shortner.Controller;

import com.example.url_shortner.dto.request.LoginRequest;
import com.example.url_shortner.dto.request.RegisterRequest;
import com.example.url_shortner.dto.response.AuthResponse;
import com.example.url_shortner.dto.response.ErrorResponse;
import com.example.url_shortner.dto.response.UserResponse;
import com.example.url_shortner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * <p>{@code /register} and {@code /login} are unchanged in path, method and request
 * body. {@code /me} is new — see {@code AuthService#getCurrentUser}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Create an account", description = "Returns a JWT on success.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "409", description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in", description = "Returns a JWT on success.")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user",
            description = "Also serves as a cheap token-validity probe on page load.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(authService.getCurrentUser(userDetails));
    }
}
