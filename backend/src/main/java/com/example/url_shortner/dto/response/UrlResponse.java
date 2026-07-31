package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A link as returned to the client.
 *
 * <p>Three fields were added for the dashboard's "Recent Links" table, all purely
 * additive so existing consumers are unaffected:
 * <ul>
 *   <li>{@code shortUrl} — the fully qualified link. The server owns the public base
 *       URL, so it should assemble this rather than making every client concatenate
 *       a host it has to be told about separately.</li>
 *   <li>{@code createdAt} — the table has a "Created Date" column, and the entity
 *       already tracked it.</li>
 *   <li>{@code expired} — so the UI can render an "Expired" badge without duplicating
 *       the date comparison, and without disagreeing with the server when the two
 *       clocks differ.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A shortened link")
public class UrlResponse {

    private Long id;

    private String originalUrl;

    private String shortCode;

    /** Fully qualified short link, e.g. {@code https://sho.rt/aB3xY9z}. */
    private String shortUrl;

    private Long clickCount;

    private LocalDateTime createdAt;

    /** {@code null} when the link never expires. */
    private LocalDateTime expiresAt;

    private Boolean active;

    /** True when {@link #expiresAt} is set and already in the past. */
    private Boolean expired;

    /**
     * True when a visitor must enter a password to be redirected.
     *
     * <p>Only the flag crosses the wire. The hash stays on the server: a client has
     * no use for it, and shipping one would turn an owner-only field into something
     * an offline attack could be run against.
     */
    private Boolean passwordProtected;
}
