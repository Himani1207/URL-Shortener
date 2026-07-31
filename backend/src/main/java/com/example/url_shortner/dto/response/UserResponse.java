package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The authenticated user's own profile.
 *
 * <p>Backs the dashboard header and the Settings page. A dedicated DTO rather than
 * returning the {@code User} entity, because that entity carries the BCrypt password
 * hash and implements {@code UserDetails} — serialising it would put the hash and a
 * pile of Spring Security scaffolding on the wire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile of the authenticated user")
public class UserResponse {

    private Long id;

    private String name;

    private String email;
}
