package com.example.url_shortner.cache;

/**
 * Central registry of Redis cache names.
 *
 * <p>Cache names are referenced from annotations ({@code @Cacheable},
 * {@code @CacheEvict}) and from {@link com.example.url_shortner.config.CacheConfig},
 * which configures a different TTL and serializer per cache. Keeping them as
 * compile-time constants in one place prevents a typo in an annotation from
 * silently creating a second, unconfigured cache region.
 */
public final class CacheNames {

    /** shortCode -> {@link CachedUrl}. Read on every redirect. */
    public static final String URL_BY_SHORT_CODE = "urlByShortCode";

    /** shortCode -> rendered PNG bytes. */
    public static final String QR_CODE = "qrCode";

    private CacheNames() {
        // Constants holder — never instantiated.
    }
}
