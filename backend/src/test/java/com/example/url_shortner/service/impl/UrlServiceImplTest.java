package com.example.url_shortner.service.impl;

import com.example.url_shortner.cache.CachedUrl;
import com.example.url_shortner.dto.request.UrlRequest;
import com.example.url_shortner.dto.request.UrlUpdateRequest;
import com.example.url_shortner.dto.response.UrlResponse;
import com.example.url_shortner.entity.ClickAnalytics;
import com.example.url_shortner.entity.Url;
import com.example.url_shortner.entity.User;
import com.example.url_shortner.exception.AliasAlreadyExistsException;
import com.example.url_shortner.exception.InvalidAliasException;
import com.example.url_shortner.exception.InvalidLinkPasswordException;
import com.example.url_shortner.exception.UnauthorizedResourceAccessException;
import com.example.url_shortner.exception.UrlExpiredException;
import com.example.url_shortner.exception.UrlInactiveException;
import com.example.url_shortner.exception.UrlNotFoundException;
import com.example.url_shortner.exception.UrlPasswordRequiredException;
import com.example.url_shortner.repository.ClickAnalyticsRepository;
import com.example.url_shortner.repository.UrlRepository;
import com.example.url_shortner.repository.UserRepository;
import com.example.url_shortner.service.UrlCacheService;
import com.example.url_shortner.util.ClientIpResolver;
import com.example.url_shortner.util.ShortCodeGenerator;
import com.example.url_shortner.util.UrlMapper;
import com.example.url_shortner.util.UserAgentInfo;
import com.example.url_shortner.util.UserAgentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core link service.
 *
 * <p>The stateless helpers ({@link UrlMapper}, {@link ShortCodeGenerator},
 * {@link UserAgentParser}, {@link ClientIpResolver}) are wired in as real instances
 * via {@code @Spy} rather than mocked. Their behaviour is deterministic and is what
 * the assertions are actually about — stubbing them would mean asserting against the
 * stubs instead of the code. Everything with I/O is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UrlServiceImpl")
class UrlServiceImplTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String EMAIL = "kunal@example.com";
    private static final String SHORT_CODE = "aB3xY9z";
    private static final String DESTINATION = "https://www.example.com/some/long/path";

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Mock private UserRepository userRepository;
    @Mock private UrlRepository urlRepository;
    @Mock private ClickAnalyticsRepository clickAnalyticsRepository;
    @Mock private UrlCacheService urlCacheService;

    @Spy private ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator(7);
    @Spy private UserAgentParser userAgentParser = new UserAgentParser();
    @Spy private ClientIpResolver clientIpResolver = new ClientIpResolver();
    @Spy private UrlMapper urlMapper = new UrlMapper(BASE_URL);

    /**
     * The real encoder, at the lowest permitted cost factor.
     *
     * <p>Spied rather than mocked because the password tests are about hashing
     * actually working — a stub would assert that Mockito returns what it was told
     * to. Strength 4 keeps a full run in milliseconds; the production bean uses the
     * BCrypt default.
     */
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UrlServiceImpl urlService;

    private User owner;
    private User otherUser;
    private UserDetails principal;

    @BeforeEach
    void setUp() {
        // Constructed explicitly rather than with @InjectMocks: the constructor
        // argument order is what documents the collaborators, and this keeps the
        // wiring obvious if a dependency is added later.
        urlService = new UrlServiceImpl(
                userRepository, urlRepository, clickAnalyticsRepository,
                urlCacheService, shortCodeGenerator, userAgentParser,
                clientIpResolver, urlMapper, passwordEncoder);

        ReflectionTestUtils.setField(urlService, "maxGenerationAttempts", 5);

        owner = User.builder().id(1L).name("Kunal").email(EMAIL).password("x").build();
        otherUser = User.builder().id(2L).name("Someone").email("other@example.com").password("x").build();

        principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
    }

    // ==================================================================
    // Creation
    // ==================================================================

    @Nested
    @DisplayName("createShortUrl")
    class Create {

        @Test
        @DisplayName("generates a code and returns the fully qualified short URL")
        void generatesCodeWhenNoAliasGiven() {
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            UrlResponse response = urlService.createShortUrl(
                    request(DESTINATION, null, null), principal);

            assertThat(response.getShortCode()).hasSize(7);
            assertThat(response.getShortUrl()).isEqualTo(BASE_URL + "/" + response.getShortCode());
            assertThat(response.getOriginalUrl()).isEqualTo(DESTINATION);
            verify(urlRepository).saveAndFlush(any(Url.class));
        }

        @Test
        @DisplayName("warms the cache so the first redirect is a hit")
        void populatesCacheOnCreate() {
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            urlService.createShortUrl(request(DESTINATION, null, null), principal);

            verify(urlCacheService).put(any(Url.class));
        }

        @Test
        @DisplayName("uses a custom alias when one is supplied")
        void honoursCustomAlias() {
            when(urlRepository.existsByShortCode("my-campaign")).thenReturn(false);

            UrlResponse response = urlService.createShortUrl(
                    request(DESTINATION, "my-campaign", null), principal);

            assertThat(response.getShortCode()).isEqualTo("my-campaign");
        }

        @Test
        @DisplayName("rejects an alias that is already taken")
        void rejectsTakenAlias() {
            when(urlRepository.existsByShortCode("taken")).thenReturn(true);

            assertThatThrownBy(() -> urlService.createShortUrl(
                    request(DESTINATION, "taken", null), principal))
                    .isInstanceOf(AliasAlreadyExistsException.class);

            verify(urlRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a reserved alias that would shadow an application route")
        void rejectsReservedAlias() {
            // "api" as an alias would collide with the API itself, since redirects
            // are served from the root path.
            assertThatThrownBy(() -> urlService.createShortUrl(
                    request(DESTINATION, "api", null), principal))
                    .isInstanceOf(InvalidAliasException.class)
                    .hasMessageContaining("reserved");
        }

        @Test
        @DisplayName("rejects an alias containing illegal characters")
        void rejectsMalformedAlias() {
            assertThatThrownBy(() -> urlService.createShortUrl(
                    request(DESTINATION, "has spaces!", null), principal))
                    .isInstanceOf(InvalidAliasException.class);
        }

        @Test
        @DisplayName("retries on generated-code collision instead of failing")
        void retriesOnCollision() {
            // First two candidates collide, third is free.
            when(urlRepository.existsByShortCode(anyString()))
                    .thenReturn(true, true, false);

            UrlResponse response = urlService.createShortUrl(
                    request(DESTINATION, null, null), principal);

            assertThat(response.getShortCode()).isNotBlank();
            verify(urlRepository, times(3)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("gives up after the configured number of attempts rather than spinning forever")
        void generationIsBounded() {
            when(urlRepository.existsByShortCode(anyString())).thenReturn(true);

            assertThatThrownBy(() -> urlService.createShortUrl(
                    request(DESTINATION, null, null), principal))
                    .isInstanceOf(IllegalStateException.class);

            verify(urlRepository, times(5)).existsByShortCode(anyString());
        }
    }

    // ==================================================================
    // Redirect
    // ==================================================================

    @Nested
    @DisplayName("getOriginalUrl")
    class Redirect {

        @Test
        @DisplayName("returns the destination and counts the click")
        void resolvesAndCountsClick() {
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(servableCachedUrl());
            when(urlRepository.incrementClickCount(10L)).thenReturn(1);
            when(urlRepository.getReferenceById(10L)).thenReturn(urlEntity(owner));

            String destination = urlService.getOriginalUrl(SHORT_CODE, requestWith(CHROME_UA, "203.0.113.7"));

            assertThat(destination).isEqualTo(DESTINATION);
            // Atomic UPDATE, not a read-modify-write - concurrent redirects cannot
            // lose a click.
            verify(urlRepository).incrementClickCount(10L);
            verify(clickAnalyticsRepository).save(any(ClickAnalytics.class));
        }

        @Test
        @DisplayName("records real browser, OS, device and IP rather than 'Unknown'")
        void recordsRealAnalytics() {
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(servableCachedUrl());
            when(urlRepository.incrementClickCount(10L)).thenReturn(1);
            when(urlRepository.getReferenceById(10L)).thenReturn(urlEntity(owner));

            urlService.getOriginalUrl(SHORT_CODE, requestWith(CHROME_UA, "203.0.113.7"));

            ArgumentCaptor<ClickAnalytics> captor = ArgumentCaptor.forClass(ClickAnalytics.class);
            verify(clickAnalyticsRepository).save(captor.capture());
            ClickAnalytics saved = captor.getValue();

            assertThat(saved.getBrowser()).isEqualTo("Chrome 120");
            assertThat(saved.getOperatingSystem()).isEqualTo("Windows 10/11");
            assertThat(saved.getDevice()).isEqualTo(UserAgentInfo.DEVICE_DESKTOP);
            assertThat(saved.getIpAddress()).isEqualTo("203.0.113.7");
        }

        @Test
        @DisplayName("prefers X-Forwarded-For over the proxy's own address")
        void usesForwardedClientIp() {
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(servableCachedUrl());
            when(urlRepository.incrementClickCount(10L)).thenReturn(1);
            when(urlRepository.getReferenceById(10L)).thenReturn(urlEntity(owner));

            MockHttpServletRequest request = requestWith(CHROME_UA, "10.0.0.1");
            request.addHeader("X-Forwarded-For", "198.51.100.42, 10.0.0.1");

            urlService.getOriginalUrl(SHORT_CODE, request);

            ArgumentCaptor<ClickAnalytics> captor = ArgumentCaptor.forClass(ClickAnalytics.class);
            verify(clickAnalyticsRepository).save(captor.capture());
            // The left-most entry is the original client, not the load balancer.
            assertThat(captor.getValue().getIpAddress()).isEqualTo("198.51.100.42");
        }

        @Test
        @DisplayName("404s on an unknown code")
        void unknownCodeIsNotFound() {
            when(urlCacheService.findByShortCode("nope123")).thenReturn(null);

            assertThatThrownBy(() -> urlService.getOriginalUrl("nope123", requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(UrlNotFoundException.class);

            verify(clickAnalyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses a deactivated link and records no click")
        void inactiveLinkIsRefused() {
            CachedUrl inactive = servableCachedUrl();
            inactive.setActive(false);
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(inactive);

            assertThatThrownBy(() -> urlService.getOriginalUrl(SHORT_CODE, requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(UrlInactiveException.class);

            // A click on a dead link is not a click on the destination.
            verify(urlRepository, never()).incrementClickCount(anyLong());
            verify(clickAnalyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses an expired link and records no click")
        void expiredLinkIsRefused() {
            CachedUrl expired = servableCachedUrl();
            expired.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(expired);

            assertThatThrownBy(() -> urlService.getOriginalUrl(SHORT_CODE, requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(UrlExpiredException.class);

            verify(clickAnalyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("evicts a cache entry proven stale by a zero-row counter update")
        void evictsStaleCacheEntry() {
            // The row was deleted between the cache read and the update, so the
            // cached value is provably wrong and must not be served again.
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(servableCachedUrl());
            when(urlRepository.incrementClickCount(10L)).thenReturn(0);

            assertThatThrownBy(() -> urlService.getOriginalUrl(SHORT_CODE, requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(UrlNotFoundException.class);

            verify(urlCacheService).evict(SHORT_CODE);
        }

        @Test
        @DisplayName("handles a missing User-Agent header without failing the redirect")
        void missingUserAgentDoesNotBreakRedirect() {
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(servableCachedUrl());
            when(urlRepository.incrementClickCount(10L)).thenReturn(1);
            when(urlRepository.getReferenceById(10L)).thenReturn(urlEntity(owner));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.0.2.1");

            String destination = urlService.getOriginalUrl(SHORT_CODE, request);

            assertThat(destination).isEqualTo(DESTINATION);
        }
    }

    // ==================================================================
    // Ownership
    // ==================================================================

    @Nested
    @DisplayName("ownership enforcement")
    class Ownership {

        @Test
        @DisplayName("blocks reading another user's analytics")
        void analyticsRequireOwnership() {
            // The bug this covers: previously any authenticated user could read any
            // link's click history, including visitor IPs, just by knowing the code.
            when(urlRepository.findByShortCode(SHORT_CODE))
                    .thenReturn(Optional.of(urlEntity(otherUser)));

            assertThatThrownBy(() -> urlService.getAnalytics(SHORT_CODE, principal, 100))
                    .isInstanceOf(UnauthorizedResourceAccessException.class);
        }

        @Test
        @DisplayName("blocks reading another user's summary")
        void summaryRequiresOwnership() {
            when(urlRepository.findByShortCode(SHORT_CODE))
                    .thenReturn(Optional.of(urlEntity(otherUser)));

            assertThatThrownBy(() -> urlService.getSummary(SHORT_CODE, principal, 30))
                    .isInstanceOf(UnauthorizedResourceAccessException.class);
        }

        @Test
        @DisplayName("blocks deleting another user's link")
        void deleteRequiresOwnership() {
            when(urlRepository.findById(10L)).thenReturn(Optional.of(urlEntity(otherUser)));

            assertThatThrownBy(() -> urlService.deleteUrl(10L, principal))
                    .isInstanceOf(UnauthorizedResourceAccessException.class);

            verify(urlRepository, never()).delete(any());
        }

        @Test
        @DisplayName("allows the owner through")
        void ownerIsAllowed() {
            when(urlRepository.findByShortCode(SHORT_CODE))
                    .thenReturn(Optional.of(urlEntity(owner)));
            when(clickAnalyticsRepository.findByUrlOrderByClickedAtDesc(any(), any()))
                    .thenReturn(List.of());

            assertThat(urlService.getAnalytics(SHORT_CODE, principal, 100)).isEmpty();
        }
    }

    // ==================================================================
    // Update / delete
    // ==================================================================

    @Nested
    @DisplayName("update and delete")
    class Mutations {

        @Test
        @DisplayName("leaves null fields untouched")
        void partialUpdateOnlyChangesSuppliedFields() {
            Url existing = urlEntity(owner);
            existing.setOriginalUrl(DESTINATION);
            when(urlRepository.findById(10L)).thenReturn(Optional.of(existing));

            UrlUpdateRequest request = UrlUpdateRequest.builder().active(false).build();
            UrlResponse response = urlService.updateUrl(10L, request, principal);

            assertThat(response.getActive()).isFalse();
            assertThat(response.getOriginalUrl()).isEqualTo(DESTINATION);
        }

        @Test
        @DisplayName("refreshes the cache after an update")
        void updateRefreshesCache() {
            when(urlRepository.findById(10L)).thenReturn(Optional.of(urlEntity(owner)));

            urlService.updateUrl(10L,
                    UrlUpdateRequest.builder().originalUrl("https://new.example.com").build(),
                    principal);

            // Without this a redirect would keep serving the old destination until
            // the TTL lapsed.
            verify(urlCacheService).put(any(Url.class));
        }

        @Test
        @DisplayName("removes click history before the link, and evicts the cache")
        void deleteRemovesHistoryAndCache() {
            Url existing = urlEntity(owner);
            when(urlRepository.findById(10L)).thenReturn(Optional.of(existing));

            urlService.deleteUrl(10L, principal);

            // Ordering matters: ClickAnalytics has no cascade, so leftover rows
            // would violate the url_id foreign key.
            var inOrder = inOrder(clickAnalyticsRepository, urlRepository, urlCacheService);
            inOrder.verify(clickAnalyticsRepository).deleteByUrl(existing);
            inOrder.verify(urlRepository).delete(existing);
            inOrder.verify(urlCacheService).evict(SHORT_CODE);
        }

        @Test
        @DisplayName("toggle flips the active flag")
        void toggleFlipsActive() {
            Url existing = urlEntity(owner);
            existing.setActive(true);
            when(urlRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThat(urlService.toggleActive(10L, principal).getActive()).isFalse();
        }
    }

    // ==================================================================
    // Analytics shaping
    // ==================================================================

    @Test
    @DisplayName("daily series is contiguous and zero-filled")
    void dailySeriesIsZeroFilled() {
        // A sparse series makes a chart draw a straight line across missing days,
        // which reads as steady traffic rather than none.
        Url url = urlEntity(owner);
        when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(url));
        when(clickAnalyticsRepository.findByUrlAndClickedAtAfterOrderByClickedAtAsc(any(), any()))
                .thenReturn(List.of(clickAt(LocalDateTime.now().minusDays(2))));

        var summary = urlService.getSummary(SHORT_CODE, principal, 7);

        assertThat(summary.getClicksPerDay()).hasSize(7);
        assertThat(summary.getClicksPerDay())
                .extracting("count")
                .containsExactly(0L, 0L, 0L, 0L, 1L, 0L, 0L);
    }

    @Test
    @DisplayName("analytics row count is capped even when a larger limit is asked for")
    void analyticsLimitIsCapped() {
        when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(urlEntity(owner)));
        when(clickAnalyticsRepository.findByUrlOrderByClickedAtDesc(any(), any()))
                .thenReturn(List.of());

        urlService.getAnalytics(SHORT_CODE, principal, 999_999);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(clickAnalyticsRepository).findByUrlOrderByClickedAtDesc(any(), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(1000);
    }

    // ==================================================================
    // Password protection
    // ==================================================================

    @Nested
    @DisplayName("password protection")
    class PasswordProtection {

        private static final String PASSWORD = "s3cret-launch";

        @Test
        @DisplayName("stores a BCrypt hash, never the plaintext")
        void hashesPasswordOnCreate() {
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            UrlRequest create = UrlRequest.builder()
                    .originalUrl(DESTINATION)
                    .password(PASSWORD)
                    .build();

            UrlResponse response = urlService.createShortUrl(create, principal);

            ArgumentCaptor<Url> captor = ArgumentCaptor.forClass(Url.class);
            verify(urlRepository).saveAndFlush(captor.capture());
            String stored = captor.getValue().getPasswordHash();

            assertThat(stored).isNotNull().isNotEqualTo(PASSWORD).startsWith("$2");
            assertThat(passwordEncoder.matches(PASSWORD, stored)).isTrue();
            assertThat(response.getPasswordProtected()).isTrue();
        }

        @Test
        @DisplayName("treats a blank password as no password")
        void blankPasswordLeavesLinkOpen() {
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            UrlRequest create = UrlRequest.builder()
                    .originalUrl(DESTINATION)
                    .password("   ")
                    .build();

            UrlResponse response = urlService.createShortUrl(create, principal);

            assertThat(response.getPasswordProtected()).isFalse();
        }

        @Test
        @DisplayName("stops a redirect on a protected link without counting a click")
        void redirectDefersToUnlock() {
            CachedUrl locked = servableCachedUrl();
            locked.setPasswordProtected(true);
            when(urlCacheService.findByShortCode(SHORT_CODE)).thenReturn(locked);

            assertThatThrownBy(() -> urlService.getOriginalUrl(
                    SHORT_CODE, requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(UrlPasswordRequiredException.class);

            // An unanswered prompt is not a visit.
            verify(urlRepository, never()).incrementClickCount(anyLong());
            verify(clickAnalyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns the destination and counts the click on the correct password")
        void unlockSucceedsWithCorrectPassword() {
            Url locked = urlEntity(owner);
            locked.setPasswordHash(passwordEncoder.encode(PASSWORD));
            when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(locked));
            when(urlRepository.incrementClickCount(10L)).thenReturn(1);
            when(urlRepository.getReferenceById(10L)).thenReturn(locked);

            String destination = urlService.unlock(
                    SHORT_CODE, PASSWORD, requestWith(CHROME_UA, "203.0.113.7"));

            assertThat(destination).isEqualTo(DESTINATION);
            verify(urlRepository).incrementClickCount(10L);
            verify(clickAnalyticsRepository).save(any(ClickAnalytics.class));
        }

        @Test
        @DisplayName("rejects a wrong password without counting a click")
        void unlockFailsWithWrongPassword() {
            Url locked = urlEntity(owner);
            locked.setPasswordHash(passwordEncoder.encode(PASSWORD));
            when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(locked));

            assertThatThrownBy(() -> urlService.unlock(
                    SHORT_CODE, "wrong", requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(InvalidLinkPasswordException.class);

            // A brute-force attempt must not show up as traffic on the link.
            verify(urlRepository, never()).incrementClickCount(anyLong());
            verify(clickAnalyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("answers an unknown code exactly as it answers a wrong password")
        void unlockDoesNotLeakLinkExistence() {
            when(urlRepository.findByShortCode("nope123")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> urlService.unlock(
                    "nope123", PASSWORD, requestWith(CHROME_UA, "1.1.1.1")))
                    .isInstanceOf(InvalidLinkPasswordException.class)
                    .hasMessage("Incorrect password");
        }

        @Test
        @DisplayName("clears the hash when the owner removes protection")
        void updateCanRemoveThePassword() {
            Url locked = urlEntity(owner);
            locked.setPasswordHash(passwordEncoder.encode(PASSWORD));
            when(urlRepository.findById(10L)).thenReturn(Optional.of(locked));

            UrlResponse response = urlService.updateUrl(
                    10L, UrlUpdateRequest.builder().removePassword(true).build(), principal);

            assertThat(locked.getPasswordHash()).isNull();
            assertThat(response.getPasswordProtected()).isFalse();
        }

        @Test
        @DisplayName("replaces the hash when the owner sets a new password")
        void updateCanReplaceThePassword() {
            Url open = urlEntity(owner);
            when(urlRepository.findById(10L)).thenReturn(Optional.of(open));

            urlService.updateUrl(
                    10L, UrlUpdateRequest.builder().password("brand-new").build(), principal);

            assertThat(passwordEncoder.matches("brand-new", open.getPasswordHash())).isTrue();
        }
    }

    // ==================================================================
    // Fixtures
    // ==================================================================

    private UrlRequest request(String url, String alias, LocalDateTime expiresAt) {
        return UrlRequest.builder()
                .originalUrl(url)
                .customAlias(alias)
                .expiresAt(expiresAt)
                .build();
    }

    private Url urlEntity(User user) {
        return Url.builder()
                .id(10L)
                .originalUrl(DESTINATION)
                .shortCode(SHORT_CODE)
                .clickCount(0L)
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .user(user)
                .build();
    }

    private CachedUrl servableCachedUrl() {
        return CachedUrl.builder()
                .id(10L)
                .shortCode(SHORT_CODE)
                .originalUrl(DESTINATION)
                .active(true)
                .expiresAt(null)
                .userId(1L)
                .build();
    }

    private ClickAnalytics clickAt(LocalDateTime when) {
        return ClickAnalytics.builder().id(1L).clickedAt(when).build();
    }

    private MockHttpServletRequest requestWith(String userAgent, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
