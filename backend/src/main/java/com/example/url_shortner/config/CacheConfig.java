package com.example.url_shortner.config;

import com.example.url_shortner.cache.CacheNames;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration.
 *
 * <p><b>What changed:</b> this class previously carried only {@code @EnableCaching}.
 * Because {@code spring-boot-starter-data-redis} was not on the classpath, Spring
 * fell back to an in-memory {@code ConcurrentMapCache} — unbounded, wiped on every
 * restart and not shared between instances. It now configures a real
 * {@link org.springframework.data.redis.cache.RedisCacheManager}.
 *
 * <p><b>Design notes:</b>
 * <ul>
 *   <li>Customisation goes through {@link RedisCacheManagerBuilderCustomizer} rather
 *       than defining a {@code CacheManager} bean outright. A hand-declared bean
 *       would override {@code spring.cache.type}, which the integration tests rely
 *       on to disable caching entirely ({@code spring.cache.type=none}).</li>
 *   <li>Each cache gets its own TTL and serializer: URL metadata is stored as JSON
 *       so it stays human-inspectable with {@code redis-cli}, while QR codes are
 *       stored as raw bytes to avoid a pointless base64 round-trip.</li>
 *   <li>A {@link CacheErrorHandler} makes Redis a soft dependency — see
 *       {@link #errorHandler()}.</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private final Duration urlTtl;
    private final Duration qrTtl;

    public CacheConfig(
            @Value("${app.cache.url-ttl-minutes:60}") long urlTtlMinutes,
            @Value("${app.cache.qr-ttl-minutes:1440}") long qrTtlMinutes) {

        this.urlTtl = Duration.ofMinutes(urlTtlMinutes);
        this.qrTtl = Duration.ofMinutes(qrTtlMinutes);
    }

    /**
     * Default configuration applied to every cache region.
     *
     * <p>Spring Boot's {@code RedisCacheAutoConfiguration} picks up a bean of this
     * type and uses it as the baseline for the cache manager.
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(urlTtl)
                // Namespace keys so this Redis instance can be shared safely.
                .prefixCacheNameWith("url-shortener::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())));
    }

    /**
     * Per-cache overrides.
     *
     * <p>QR codes are immutable for a given short code and expensive to render, so
     * they live far longer than URL metadata and skip JSON encoding entirely.
     * Null values are disabled on that region because a raw byte-array serializer
     * has no representation for Spring's {@code NullValue} marker.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer() {
        return builder -> builder
                .withCacheConfiguration(CacheNames.URL_BY_SHORT_CODE,
                        redisCacheConfiguration().entryTtl(urlTtl))
                .withCacheConfiguration(CacheNames.QR_CODE,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(qrTtl)
                                .disableCachingNullValues()
                                .prefixCacheNameWith("url-shortener::")
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(RedisSerializer.byteArray())));
    }

    /**
     * Jackson mapper used for cache values.
     *
     * <p>Default typing is enabled so polymorphic values — notably Spring's
     * {@code NullValue} marker, which is what makes negative caching possible —
     * round-trip correctly. The {@link PolymorphicTypeValidator} is deliberately
     * restricted to this application's own packages plus {@code java.util} and
     * {@code java.time}: unrestricted default typing is the classic Jackson
     * deserialization-gadget vector, and an allow-list closes it.
     */
    private ObjectMapper cacheObjectMapper() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.url_shortner.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("org.springframework.cache.support.NullValue")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * Treats Redis as a soft dependency.
     *
     * <p>By default any Redis failure propagates out of the cache interceptor and
     * turns into a 500 for the caller. For a URL shortener that is the wrong
     * trade-off: a cache outage should degrade latency, not availability. Swallowing
     * these errors makes the annotated method fall through to its normal body, so
     * redirects keep working straight off the database while Redis is unreachable.
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache GET failed [cache={}, key={}] - falling back to database: {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed [cache={}, key={}]: {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                // Logged at ERROR: a failed evict can serve stale data until the TTL
                // lapses, which is more serious than a failed read or write.
                log.error("Cache EVICT failed [cache={}, key={}] - entry may be stale until TTL expiry: {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.error("Cache CLEAR failed [cache={}]: {}", cache.getName(), ex.getMessage());
            }
        };
    }
}
