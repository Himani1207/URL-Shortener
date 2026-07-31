package com.example.url_shortner.repository;

import com.example.url_shortner.entity.ClickAnalytics;
import com.example.url_shortner.entity.Url;
import com.example.url_shortner.repository.projection.LabelCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    long countByUrl(Url url);

    long countByUrlAndDevice(Url url, String device);

    List<ClickAnalytics> findByUrl(Url url);

    // ------------------------------------------------------------------
    // Additions
    // ------------------------------------------------------------------

    /**
     * Click history, newest first and page-limited.
     *
     * <p>The unpaged {@link #findByUrl(Url)} loads the entire history into memory;
     * on a link with a hundred thousand clicks that is enough to exhaust the heap.
     * The analytics endpoint uses this variant.
     */
    List<ClickAnalytics> findByUrlOrderByClickedAtDesc(Url url, Pageable pageable);

    /** Bounded window used to build the daily time series. */
    List<ClickAnalytics> findByUrlAndClickedAtAfterOrderByClickedAtAsc(
            Url url, LocalDateTime since);

    /**
     * Distinct IP addresses, used as a rough unique-visitor count.
     *
     * <p>Genuinely approximate: NAT collapses many people behind one address and
     * mobile networks rotate addresses. It is reported as "unique visitors" for
     * consistency with what comparable products show, not as a precise figure.
     */
    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickAnalytics c WHERE c.url = :url")
    long countDistinctIpByUrl(@Param("url") Url url);

    @Query("""
            SELECT c.browser AS label, COUNT(c) AS count
            FROM ClickAnalytics c
            WHERE c.url = :url
            GROUP BY c.browser
            ORDER BY COUNT(c) DESC
            """)
    List<LabelCount> countGroupedByBrowser(@Param("url") Url url);

    @Query("""
            SELECT c.operatingSystem AS label, COUNT(c) AS count
            FROM ClickAnalytics c
            WHERE c.url = :url
            GROUP BY c.operatingSystem
            ORDER BY COUNT(c) DESC
            """)
    List<LabelCount> countGroupedByOperatingSystem(@Param("url") Url url);

    @Query("""
            SELECT c.device AS label, COUNT(c) AS count
            FROM ClickAnalytics c
            WHERE c.url = :url
            GROUP BY c.device
            ORDER BY COUNT(c) DESC
            """)
    List<LabelCount> countGroupedByDevice(@Param("url") Url url);

    /**
     * Removes a link's click history.
     *
     * <p>Required because {@code ClickAnalytics} has no cascade from {@code Url};
     * deleting a URL without this leaves orphan rows that violate the
     * {@code url_id} foreign key.
     */
    @Modifying
    @Query("DELETE FROM ClickAnalytics c WHERE c.url = :url")
    void deleteByUrl(@Param("url") Url url);

    /** Total clicks across every link owned by a user (dashboard tile). */
    @Query("SELECT COUNT(c) FROM ClickAnalytics c WHERE c.url.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /** Clicks across a user's links since a cutoff (dashboard trend tile). */
    @Query("""
            SELECT COUNT(c) FROM ClickAnalytics c
            WHERE c.url.user.id = :userId AND c.clickedAt >= :since
            """)
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
