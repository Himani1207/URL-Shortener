package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a successful registration or login.
 *
 * <p>Fields beyond {@code token} are additive, so existing clients are unaffected.
 * They exist so the frontend can render the signed-in user's name immediately after
 * login instead of firing a second request, and can schedule a re-login before the
 * token lapses rather than discovering it through a surprise 401.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Issued JWT plus the profile it belongs to")
public class AuthResponse {

    @Schema(description = "Bearer token for the Authorization header")
    private String token;

    @Schema(description = "Token type, always 'Bearer'", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token lifetime in milliseconds", example = "86400000")
    private Long expiresIn;

    private UserResponse user;
}
