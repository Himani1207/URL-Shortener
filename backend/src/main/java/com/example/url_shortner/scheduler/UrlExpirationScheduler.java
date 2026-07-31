package com.example.url_shortner.scheduler;

import com.example.url_shortner.entity.Url;
import com.example.url_shortner.repository.UrlRepository;
import com.example.url_shortner.service.UrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hourly sweep that deactivates links whose expiry has passed.
 *
 * <p><b>Why a sweep is needed at all</b>, given the redirect path already refuses
 * expired links: that check is a runtime guard, not a state change. Without the
 * sweep an expired link keeps {@code active = true} in the database forever, so the
 * dashboard reports it as active, the "active links" tile overcounts, and nothing
 * ever drops its stale cache entry. The sweep makes stored state agree with reality.
 *
 * <p><b>Interaction with the cache:</b> each deactivated link is evicted
 * individually. Without that, a link already resolved into Redis would keep serving
 * redirects until its TTL lapsed — up to an hour after it was supposed to stop.
 *
 * <p><b>On multiple instances:</b> every replica runs this sweep. That is harmless
 * because it is idempotent — the query only matches links that are still active, so
 * a second runner finds nothing to do — but it does mean redundant work. If this
 * ever scales beyond a couple of instances, wrap the method with ShedLock so exactly
 * one replica executes it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UrlExpirationScheduler {

    /** Safety valve: keeps one sweep from loading an unbounded result set. */
    private static final int MAX_BATCH = 5_000;

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    /**
     * Runs at the top of every hour.
     *
     * <p>{@code fixedRate} would drift relative to the wall clock and start counting
     * from application boot; a cron expression keeps the schedule predictable across
     * restarts and deploys.
     */
    @Scheduled(cron = "${app.scheduling.expiration-cron:0 0 * * * *}")
    @Transactional
    public void deactivateExpiredUrls() {

        LocalDateTime now = LocalDateTime.now();
        long startedAt = System.currentTimeMillis();

        List<Url> expired = urlRepository
                .findByActiveTrueAndExpiresAtNotNullAndExpiresAtBefore(now);

        if (expired.isEmpty()) {
            log.debug("Expiry sweep: nothing to deactivate");
            return;
        }

        if (expired.size() > MAX_BATCH) {
            log.warn("Expiry sweep found {} links, processing the first {}",
                    expired.size(), MAX_BATCH);
            expired = expired.subList(0, MAX_BATCH);
        }

        expired.forEach(url -> url.setActive(false));
        urlRepository.saveAll(expired);

        // Evict after the state change so a concurrent redirect cannot repopulate
        // the cache with the pre-update value.
        expired.forEach(url -> urlCacheService.evict(url.getShortCode()));

        log.info("Expiry sweep deactivated {} link(s) in {}ms",
                expired.size(), System.currentTimeMillis() - startedAt);

        if (log.isDebugEnabled()) {
            expired.forEach(url -> log.debug("Deactivated expired link [shortCode={}, expiredAt={}]",
                    url.getShortCode(), url.getExpiresAt()));
        }
    }
}
