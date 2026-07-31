package com.example.url_shortner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A shortened link.
 *
 * <p><b>Relationships and existing columns are unchanged.</b> The adjustments are
 * storage-level only:
 * <ul>
 *   <li>{@code originalUrl} is now explicitly {@code length = 2048}. Without it
 *       Hibernate generates {@code varchar(255)}, while the request DTO accepts up
 *       to 2048 characters — so a long campaign URL passed validation and then failed
 *       on insert. 2048 is the practical browser limit.</li>
 *   <li>{@code shortCode} is {@code nullable = false, length = 50}, matching the
 *       alias rules. It was already unique, which is what the redirect lookup relies
 *       on for its index.</li>
 *   <li>An index on {@code user_id}, because every dashboard query filters by owner
 *       and a foreign key does not get one automatically in PostgreSQL.</li>
 *   <li>A composite index on {@code (active, expires_at)} for the hourly expiry
 *       sweep, which would otherwise scan the whole table every hour.</li>
 * </ul>
 */
@Entity
@Table(
        name = "urls",
        indexes = {
                @Index(name = "idx_urls_user_id", columnList = "user_id"),
                @Index(name = "idx_urls_active_expires_at", columnList = "active, expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(unique = true, nullable = false, length = 50)
    private String shortCode;

    private Long clickCount;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    /**
     * BCrypt hash of the visitor password, or {@code null} when the link is open.
     *
     * <p>Only ever written through {@code PasswordEncoder#encode}; the plaintext is
     * read off the request, hashed, and never assigned to a field or logged. 100
     * characters is the conventional column width for a BCrypt digest (60 today,
     * with headroom for a future algorithm prefix).
     *
     * <p>The column is deliberately absent from {@code UrlResponse} — clients are
     * told <i>whether</i> a link is protected, never the digest.
     */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** @return true when a visitor must supply a password before being redirected. */
    @Transient
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        clickCount = 0L;
        active = true;
    }
}
