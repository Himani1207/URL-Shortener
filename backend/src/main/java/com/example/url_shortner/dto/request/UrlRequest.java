package com.example.url_shortner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Payload for creating a short link.
 *
 * <p>Validation hardening applied here: {@code originalUrl} previously carried only
 * {@code @Pattern}, which Bean Validation skips for {@code null} values — so a body
 * of <code>{}</code> passed validation and persisted a row with a null destination.
 * {@code @NotBlank} closes that gap.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for creating a short link")
public class UrlRequest {

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            message = "Invalid URL"
    )
    @Schema(example = "https://www.example.com/a/very/long/path")
    private String originalUrl;

    /**
     * Optional vanity alias. Format and reserved-word checks live in
     * {@link com.example.url_shortner.util.ShortCodeGenerator} so the same rules
     * apply to create and update.
     */
    @Size(min = 3, max = 50, message = "Alias must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Alias may only contain letters, numbers, hyphens and underscores"
    )
    @Schema(example = "my-campaign")
    private String customAlias;

    /** {@code null} means the link never expires. */
    @Schema(example = "2026-12-31T23:59:00")
    private LocalDateTime expiresAt;

    /**
     * Optional visitor password, in plaintext.
     *
     * <p>Hashed with BCrypt in the service before the row is written; it is never
     * persisted, cached or logged as given. Blank is treated as absent, so a client
     * that always sends the field does not accidentally protect every link with an
     * empty password.
     *
     * <p>The 72-character ceiling is BCrypt's own limit — bytes beyond it are
     * silently ignored by the algorithm, so accepting more would mean two different
     * passwords could unlock the same link.
     */
    @Size(min = 4, max = 72, message = "Password must be between 4 and 72 characters")
    @Schema(example = "s3cret-launch", description = "Plaintext; hashed server-side and never stored as given")
    private String password;
}
