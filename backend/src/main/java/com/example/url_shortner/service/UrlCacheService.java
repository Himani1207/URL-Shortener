package com.example.url_shortner.service;

import com.example.url_shortner.cache.CachedUrl;
import com.example.url_shortner.entity.Url;

/**
 * Read-through cache for short-code resolution.
 *
 * <p><b>Why this is its own bean rather than methods on {@code UrlServiceImpl}:</b>
 * Spring's caching is implemented with a proxy. An annotated method only goes
 * through the cache interceptor when it is invoked <i>from another bean</i>;
 * a call from one method of {@code UrlServiceImpl} to another bypasses the proxy
 * entirely and the annotation silently does nothing. Moving the annotated methods
 * onto a separate collaborator is what makes the caching actually take effect,
 * and it keeps {@code UrlServiceImpl} focused on URL business rules
 * (Single Responsibility).
 *
 * <p><b>Consistency contract:</b> every write path that changes a URL's
 * addressability — create, update, delete, activate/deactivate, scheduled
 * expiry — must call {@link #put(Url)} or {@link #evict(String)}. Click counts are
 * deliberately <i>not</i> part of the cached payload, so redirects can be served
 * from cache without ever serving a stale counter.
 */
public interface UrlCacheService {

    /**
     * Resolves a short code, consulting Redis first and falling back to the
     * database on a miss (read-through).
     *
     * <p>Misses for codes that do not exist are cached as well ("negative
     * caching"). Without it, a burst of requests for random non-existent codes
     * would reach the database on every single request.
     *
     * @param shortCode code to resolve
     * @return the cached projection, or {@code null} if no such short code exists
     */
    CachedUrl findByShortCode(String shortCode);

    /**
     * Writes the current state of a URL into the cache, replacing any existing
     * entry. Used after create and update so the very next redirect is a hit.
     *
     * @return the value that was cached
     */
    CachedUrl put(Url url);

    /** Removes both the URL and QR entries for a short code. */
    void evict(String shortCode);

    /** Clears every cached URL. Used by bulk operations such as the expiry sweep. */
    void evictAll();

    /**
     * Whether the most recent {@link #findByShortCode(String)} call on the current
     * thread was served from Redis without touching the database.
     *
     * <p>Exists purely for the cache hit/miss logging required by the observability
     * work. A cache <i>hit</i> cannot be observed from inside the annotated method,
     * because Spring short-circuits the method body entirely on a hit — so the
     * implementation records the fact on a thread-local during lookup and the caller
     * reads it back immediately afterwards. Callers must not rely on this across
     * asynchronous boundaries.
     */
    boolean lastLookupWasCacheHit();
}
