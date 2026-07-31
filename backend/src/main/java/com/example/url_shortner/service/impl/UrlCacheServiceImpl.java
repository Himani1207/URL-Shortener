package com.example.url_shortner.service.impl;

import com.example.url_shortner.cache.CacheNames;
import com.example.url_shortner.cache.CachedUrl;
import com.example.url_shortner.entity.Url;
import com.example.url_shortner.repository.UrlRepository;
import com.example.url_shortner.service.UrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis-backed implementation of {@link UrlCacheService}.
 *
 * <p>Interacts with the rest of the application as follows:
 * <ul>
 *   <li>{@code UrlServiceImpl} calls {@link #findByShortCode(String)} on the redirect
 *       path and {@link #put(Url)} / {@link #evict(String)} on every write.</li>
 *   <li>{@code UrlExpirationScheduler} calls {@link #evict(String)} for each link it
 *       deactivates, so an expired link stops resolving immediately rather than
 *       lingering until its TTL lapses.</li>
 *   <li>{@code CacheConfig} supplies the TTLs, serializers and the error handler that
 *       lets every method here degrade to a direct database read if Redis is down.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCacheServiceImpl implements UrlCacheService {

    private final UrlRepository urlRepository;

    /**
     * Set to {@code false} by {@link #findByShortCode(String)} whenever its body
     * executes. Because Spring only executes that body on a cache miss, a value
     * still holding {@code true} after the call proves the value came from Redis.
     * See {@link UrlCacheService#lastLookupWasCacheHit()} for why this indirection
     * is necessary.
     */
    private static final ThreadLocal<Boolean> CACHE_HIT = ThreadLocal.withInitial(() -> Boolean.TRUE);

    /**
     * {@inheritDoc}
     *
     * <p>Implementation notes:
     * <ul>
     *   <li>Null results <i>are</i> cached, giving negative caching for free — Spring
     *       stores its {@code NullValue} marker, which the JSON serializer configured
     *       in {@code CacheConfig} knows how to round-trip.</li>
     *   <li>{@code sync = true} is intentionally <b>not</b> used. It would collapse
     *       concurrent misses into one database read, but Spring bypasses the
     *       {@code CacheErrorHandler} on the synchronized path, so a Redis outage
     *       would surface as HTTP 500 instead of degrading to a database read.
     *       Availability of redirects outweighs a handful of duplicate indexed
     *       lookups during a stampede.</li>
     * </ul>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.URL_BY_SHORT_CODE, key = "#shortCode")
    public CachedUrl findByShortCode(String shortCode) {
        // Reached only on a miss: Spring short-circuits this method on a hit.
        CACHE_HIT.set(Boolean.FALSE);
        log.debug("Cache MISS [cache={}, shortCode={}] - loading from database",
                CacheNames.URL_BY_SHORT_CODE, shortCode);

        CachedUrl loaded = urlRepository.findByShortCode(shortCode)
                .map(CachedUrl::from)
                .orElse(null);

        if (loaded == null) {
            log.debug("Short code '{}' does not exist - caching negative result", shortCode);
        }
        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code @CachePut} always executes the method body and then stores the
     * returned value, which is exactly the semantics needed after a write: the
     * caller has a fresh entity in hand and we want the cache to reflect it without
     * a redundant read.
     */
    @Override
    @CachePut(cacheNames = CacheNames.URL_BY_SHORT_CODE, key = "#url.shortCode")
    public CachedUrl put(Url url) {
        log.debug("Cache PUT [cache={}, shortCode={}]",
                CacheNames.URL_BY_SHORT_CODE, url.getShortCode());
        return CachedUrl.from(url);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Evicts the QR entry alongside the URL entry: a QR image encodes the short
     * link, so if the code stops resolving the rendered image is meaningless too.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.URL_BY_SHORT_CODE, key = "#shortCode"),
            @CacheEvict(cacheNames = CacheNames.QR_CODE, key = "#shortCode")
    })
    public void evict(String shortCode) {
        log.debug("Cache EVICT [shortCode={}]", shortCode);
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.URL_BY_SHORT_CODE, allEntries = true)
    public void evictAll() {
        log.info("Cache EVICT ALL [cache={}]", CacheNames.URL_BY_SHORT_CODE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads and resets the thread-local in one step so a thread returned to the
     * servlet container's pool never carries a stale flag into the next request.
     */
    @Override
    public boolean lastLookupWasCacheHit() {
        boolean hit = Boolean.TRUE.equals(CACHE_HIT.get());
        CACHE_HIT.remove();
        return hit;
    }
}
