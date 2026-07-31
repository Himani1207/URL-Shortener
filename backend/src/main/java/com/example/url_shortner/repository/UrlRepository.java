package com.example.url_shortner.repository;

import com.example.url_shortner.entity.Url;
import com.example.url_shortner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByUser(User user);

    // ------------------------------------------------------------------
    // Additions
    // ------------------------------------------------------------------

    /** Newest first — the order the dashboard's "Recent Links" table expects. */
    List<Url> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Ownership-scoped lookup.
     *
     * <p>Used by update/delete so the ownership check is part of the query rather
     * than a separate {@code if} after loading the row. A caller cannot forget it.
     */
    Optional<Url> findByIdAndUser(Long id, User user);

    Optional<Url> findByShortCodeAndUser(String shortCode, User user);

    /**
     * Increments the click counter atomically in the database.
     *
     * <p><b>Why not {@code url.setClickCount(url.getClickCount() + 1)}:</b> that is a
     * read-modify-write across two statements. Two concurrent redirects both read
     * the same value and both write value+1, so one click is silently lost — and
     * redirects are precisely the endpoint that receives concurrent traffic.
     * A single {@code UPDATE ... SET click_count = click_count + 1} is atomic under
     * the database's row lock and cannot lose an increment.
     *
     * <p>{@code clearAutomatically}/{@code flushAutomatically} keep the persistence
     * context consistent with the bulk update.
     *
     * @return number of rows updated: 1 on success, 0 if the row no longer exists
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    int incrementClickCount(@Param("id") Long id);

    /**
     * Links that are still flagged active but whose expiry has passed.
     * Drives the hourly expiry sweep.
     */
    List<Url> findByActiveTrueAndExpiresAtNotNullAndExpiresAtBefore(LocalDateTime cutoff);

    // --- Dashboard aggregates -----------------------------------------

    long countByUser(User user);

    long countByUserAndActiveTrue(User user);

    /**
     * {@code COALESCE} so a user with no links gets 0 rather than {@code null},
     * which would NPE on unboxing to {@code long}.
     */
    @Query("SELECT COALESCE(SUM(u.clickCount), 0) FROM Url u WHERE u.user = :user")
    long sumClickCountByUser(@Param("user") User user);

    /** Links created since a cutoff — powers the "new this week" dashboard tile. */
    long countByUserAndCreatedAtAfter(User user, LocalDateTime cutoff);
}
