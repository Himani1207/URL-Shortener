package com.example.url_shortner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * The password a visitor supplies to open a protected link.
 *
 * <p>Sent in a POST body rather than as a query parameter on purpose: query strings
 * land in access logs, browser history and {@code Referer} headers, all of which
 * would leak the password of every protected link on the instance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Password submitted to unlock a protected short link")
public class UnlockRequest {

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    @Schema(example = "s3cret-launch")
    private String password;
}
