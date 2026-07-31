package com.example.url_shortner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload for editing an existing link.
 *
 * <p><b>Why a separate DTO from {@link UrlRequest}:</b> the two operations have
 * genuinely different rules. On create, {@code originalUrl} is mandatory; on update
 * every field is optional and {@code null} means "leave unchanged". Reusing one DTO
 * would force the create path to accept a null destination, or the update path to
 * demand fields the client did not intend to touch. Backing the dashboard's "Edit"
 * action.
 *
 * <p>Note that the short code itself is not editable. Changing it would break every
 * link already in circulation, so the API deliberately does not offer it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Partial update for an existing link; null fields are left unchanged")
public class UrlUpdateRequest {

    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            message = "Invalid URL"
    )
    @Schema(example = "https://www.example.com/updated-destination")
    private String originalUrl;

    @Schema(example = "2026-12-31T23:59:00")
    private LocalDateTime expiresAt;

    /** Lets the owner pause a link without deleting it. */
    @Schema(example = "true")
    private Boolean active;

    /**
     * New visitor password, in plaintext. {@code null} leaves the existing one alone.
     *
     * <p>Setting a password and removing one need separate signals: {@code null}
     * already means "unchanged" everywhere else on this DTO, so it cannot also mean
     * "clear it". Hence {@link #removePassword}.
     */
    @Size(min = 4, max = 72, message = "Password must be between 4 and 72 characters")
    @Schema(example = "s3cret-launch")
    private String password;

    /** When true, drops the password so the link becomes openly accessible again. */
    @Schema(example = "false")
    private Boolean removePassword;
}
