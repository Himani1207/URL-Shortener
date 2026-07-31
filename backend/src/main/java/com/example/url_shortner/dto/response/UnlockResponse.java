package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * The destination revealed once a protected link has been unlocked.
 *
 * <p>Returned as JSON rather than as a 302 because the caller is the unlock page's
 * fetch, not the address bar — a redirect status would be followed by fetch itself
 * and the page would never learn where to send the visitor.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Destination for a successfully unlocked link")
public class UnlockResponse {

    @Schema(example = "https://www.example.com/a/very/long/path")
    private String originalUrl;
}
