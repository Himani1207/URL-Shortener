package com.example.url_shortner.cache;

import com.example.url_shortner.entity.Url;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The value stored in Redis under {@link CacheNames#URL_BY_SHORT_CODE}.
 *
 * <p><b>Why not cache the {@link Url} entity directly:</b> a JPA entity carries a
 * lazy {@code @ManyToOne} proxy to {@link com.example.url_shortner.entity.User},
 * a persistence-context identity, and a mutable {@code clickCount}. Serialising it
 * would either trigger a lazy-load failure outside a transaction or cache a
 * click count that is stale the moment it is written.
 *
 * <p><b>Why not cache just the original URL string:</b> the redirect path also has
 * to evaluate {@code active} and {@code expiresAt}, and it needs the row {@code id}
 * to attribute the click without a second {@code SELECT}. Carrying those four
 * fields makes a cache hit sufficient to serve the whole redirect.
 *
 * <p>Note the deliberate absence of {@code clickCount} — counters are incremented
 * atomically in the database and are never read from this cache, so a hit can
 * never serve a stale count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUrl implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Primary key, used to attach analytics and increment the counter. */
    private Long id;

    private String shortCode;

    private String originalUrl;

    /** Mirrors {@code Url.active}; a redirect on an inactive link must 410. */
    private Boolean active;

    /** {@code null} means the link never expires. */
    private LocalDateTime expiresAt;

    /** Owner id, used for ownership checks without loading the User entity. */
    private Long userId;

    /**
     * Whether a visitor password is required — the flag only, never the hash.
     *
     * <p>Redis holds this cache, so anything put here is readable by whoever can
     * read the Redis instance. The redirect path only needs to know that it must
     * stop and ask; verification happens in the service against the row in the
     * database. Caching the digest would spread credential material across a second
     * datastore to save one indexed lookup on an already-interactive path.
     */
    private Boolean passwordProtected;

    /** Projects a persistent entity onto its cacheable form. */
    public static CachedUrl from(Url url) {
        return CachedUrl.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .active(url.getActive())
                .expiresAt(url.getExpiresAt())
                .userId(url.getUser() != null ? url.getUser().getId() : null)
                .passwordProtected(url.isPasswordProtected())
                .build();
    }

    /** @return true when an expiry is set and already in the past. */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /** @return true when the link may be served. */
    public boolean isServable() {
        return Boolean.TRUE.equals(active) && !isExpired();
    }
}
