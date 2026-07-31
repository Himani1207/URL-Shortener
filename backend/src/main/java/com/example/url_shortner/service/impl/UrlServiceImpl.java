package com.example.url_shortner.service.impl;

import com.example.url_shortner.cache.CachedUrl;
import com.example.url_shortner.dto.request.UrlRequest;
import com.example.url_shortner.dto.request.UrlUpdateRequest;
import com.example.url_shortner.dto.response.AnalyticsSummaryResponse;
import com.example.url_shortner.dto.response.ClickAnalyticsResponse;
import com.example.url_shortner.dto.response.DailyClickResponse;
import com.example.url_shortner.dto.response.DashboardStatsResponse;
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
import com.example.url_shortner.exception.UserNotFoundException;
import com.example.url_shortner.repository.ClickAnalyticsRepository;
import com.example.url_shortner.repository.UrlRepository;
import com.example.url_shortner.repository.UserRepository;
import com.example.url_shortner.service.UrlCacheService;
import com.example.url_shortner.service.UrlService;
import com.example.url_shortner.util.ClientIpResolver;
import com.example.url_shortner.util.ShortCodeGenerator;
import com.example.url_shortner.util.UrlMapper;
import com.example.url_shortner.util.UserAgentInfo;
import com.example.url_shortner.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core link operations.
 *
 * <p>Collaborators and why each is here:
 * <ul>
 *   <li>{@link UrlCacheService} — short-code resolution on the redirect path. It is a
 *       separate bean because Spring's cache annotations are proxy-based and would
 *       be inert if these methods called each other internally.</li>
 *   <li>{@link ShortCodeGenerator} — secure code generation plus alias validation.</li>
 *   <li>{@link UserAgentParser} / {@link ClientIpResolver} — real click attribution,
 *       replacing the hardcoded {@code "Unknown"} values.</li>
 *   <li>{@link UrlMapper} — entity to DTO projection in one place.</li>
 *   <li>{@link PasswordEncoder} — the same BCrypt bean that hashes account
 *       passwords, reused for visitor link passwords. A second encoder would be a
 *       second place to get the work factor wrong.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;
    private final UrlCacheService urlCacheService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UserAgentParser userAgentParser;
    private final ClientIpResolver clientIpResolver;
    private final UrlMapper urlMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.shortcode.max-generation-attempts:5}")
    private int maxGenerationAttempts;

    // ==================================================================
    // Create
    // ==================================================================

    @Override
    @Transactional
    public UrlResponse createShortUrl(UrlRequest request, UserDetails userDetails) {

        User user = requireUser(userDetails);
        String shortCode = resolveShortCode(request.getCustomAlias());

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .expiresAt(request.getExpiresAt())
                .passwordHash(hashOrNull(request.getPassword()))
                .user(user)
                .build();

        try {
            urlRepository.saveAndFlush(url);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent requests can both pass the existsByShortCode check
            // before either commits; the unique index is the real arbiter, so the
            // constraint violation is translated rather than surfaced as a 500.
            log.warn("Short code '{}' was taken concurrently", shortCode);
            throw new AliasAlreadyExistsException(
                    "That alias was just taken. Please choose another.");
        }

        // Warm the cache so the first redirect is already a hit.
        urlCacheService.put(url);

        // The password itself is never logged, only whether one was set.
        log.info("Link created [shortCode={}, userId={}, expiresAt={}, protected={}]",
                shortCode, user.getId(), url.getExpiresAt(), url.isPasswordProtected());

        return urlMapper.toResponse(url);
    }

    /**
     * Hashes a supplied visitor password, treating blank as absent.
     *
     * <p>A client that always sends the field — which is the natural thing for a form
     * with an optional password toggle to do — would otherwise protect every link
     * with an empty string, locking visitors out of links their owner believed were
     * open.
     */
    private String hashOrNull(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) return null;
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Chooses the short code: validates a requested alias, or generates one.
     *
     * <p>Generation retries on collision. The loop is bounded — an unbounded
     * {@code while} would spin forever if the keyspace were ever exhausted or the
     * database were unreachable.
     */
    private String resolveShortCode(String customAlias) {

        if (customAlias != null && !customAlias.isBlank()) {
            String alias = customAlias.trim();

            if (!shortCodeGenerator.isValidAlias(alias)) {
                throw new InvalidAliasException(
                        "Alias must be 3-50 characters and contain only letters, "
                                + "numbers, hyphens or underscores");
            }
            if (shortCodeGenerator.isReserved(alias)) {
                throw new InvalidAliasException("'" + alias + "' is a reserved word");
            }
            if (urlRepository.existsByShortCode(alias)) {
                throw new AliasAlreadyExistsException("Alias '" + alias + "' is already taken");
            }
            return alias;
        }

        for (int attempt = 1; attempt <= maxGenerationAttempts; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
            log.warn("Generated short code '{}' collided (attempt {}/{})",
                    candidate, attempt, maxGenerationAttempts);
        }

        throw new IllegalStateException(
                "Could not generate a unique short code after "
                        + maxGenerationAttempts + " attempts");
    }

    // ==================================================================
    // Read
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<UrlResponse> getMyUrls(UserDetails userDetails) {
        User user = requireUser(userDetails);
        List<Url> urls = urlRepository.findByUserOrderByCreatedAtDesc(user);
        log.debug("Listed {} links [userId={}]", urls.size(), user.getId());
        return urlMapper.toResponseList(urls);
    }

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getUrl(Long id, UserDetails userDetails) {
        return urlMapper.toResponse(requireOwnedUrl(id, requireUser(userDetails)));
    }

    // ==================================================================
    // Redirect - the hot path
    // ==================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Ordering matters here:
     * <ol>
     *   <li>Resolve through the cache. On a hit no database read happens at all.</li>
     *   <li>Validate active/expiry <i>before</i> recording anything — a click on a
     *       dead link is not a click on the destination.</li>
     *   <li>Increment the counter with a single atomic {@code UPDATE}, not a
     *       read-modify-write, so concurrent redirects cannot lose clicks.</li>
     *   <li>Record the analytics row using a lazy entity reference, which needs no
     *       additional {@code SELECT}.</li>
     * </ol>
     *
     * <p>The two writes are synchronous. Moving them to {@code @Async} would shave
     * a few milliseconds off redirect latency, at the cost of losing in-flight
     * clicks whenever an instance is replaced. Correct counts are worth more than
     * the milliseconds here.
     */
    @Override
    @Transactional
    public String getOriginalUrl(String shortCode, HttpServletRequest request) {

        CachedUrl cached = urlCacheService.findByShortCode(shortCode);
        boolean cacheHit = urlCacheService.lastLookupWasCacheHit();

        if (cached == null) {
            log.warn("Redirect failed, unknown short code [shortCode={}, cacheHit={}]",
                    shortCode, cacheHit);
            throw new UrlNotFoundException("Short URL not found");
        }

        if (cacheHit) {
            log.debug("Cache HIT [cache=urlByShortCode, shortCode={}]", shortCode);
        }

        if (!Boolean.TRUE.equals(cached.getActive())) {
            log.info("Redirect refused, link is inactive [shortCode={}]", shortCode);
            throw new UrlInactiveException("This link has been deactivated");
        }

        if (cached.isExpired()) {
            log.info("Redirect refused, link expired [shortCode={}, expiresAt={}]",
                    shortCode, cached.getExpiresAt());
            throw new UrlExpiredException("This link has expired");
        }

        // Checked after active/expiry so a dead link is reported as dead rather
        // than prompting for a password it will never honour, and before
        // recordClick so an unanswered prompt is not counted as a visit.
        if (Boolean.TRUE.equals(cached.getPasswordProtected())) {
            log.info("Redirect deferred, link is password protected [shortCode={}]", shortCode);
            throw new UrlPasswordRequiredException("This link is password protected");
        }

        recordClick(cached, request);

        log.info("Redirect [shortCode={}, cacheHit={}, destination={}]",
                shortCode, cacheHit, cached.getOriginalUrl());

        return cached.getOriginalUrl();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the row directly rather than through the cache, because the BCrypt
     * digest deliberately does not live in the cache — see {@link CachedUrl}. One
     * indexed lookup on an interactive, human-paced request is the right price for
     * keeping credential material in a single datastore.
     *
     * <p>Every failure path throws the same exception with the same message. An
     * unauthenticated caller must not be able to tell "wrong password" from
     * "no such link" or "link paused", or this endpoint becomes a way to enumerate
     * which short codes exist.
     */
    @Override
    @Transactional
    public String unlock(String shortCode, String password, HttpServletRequest request) {

        Url url = urlRepository.findByShortCode(shortCode).orElse(null);

        if (url == null || !Boolean.TRUE.equals(url.getActive()) || isExpired(url)) {
            // Still run a hash comparison on a dummy value. Returning early would
            // make a missing link answer in microseconds and a real one in the
            // ~100ms BCrypt takes, which is a timing oracle for link existence.
            passwordEncoder.matches(password, DUMMY_HASH);
            log.info("Unlock refused, link unavailable [shortCode={}]", shortCode);
            throw new InvalidLinkPasswordException("Incorrect password");
        }

        if (!url.isPasswordProtected()) {
            // Not protected at all: nothing to verify, so serve it exactly as a
            // plain redirect would, click recording included.
            log.debug("Unlock called on an unprotected link [shortCode={}]", shortCode);
            return getOriginalUrl(shortCode, request);
        }

        if (!passwordEncoder.matches(password, url.getPasswordHash())) {
            log.info("Unlock refused, incorrect password [shortCode={}]", shortCode);
            throw new InvalidLinkPasswordException("Incorrect password");
        }

        // Read before recordClick: incrementClickCount clears the persistence
        // context, which detaches this entity.
        String destination = url.getOriginalUrl();

        recordClick(CachedUrl.from(url), request);

        log.info("Link unlocked [shortCode={}]", shortCode);
        return destination;
    }

    /**
     * A well-formed BCrypt digest of a value nothing will ever match.
     *
     * <p>Exists purely so the unavailable-link path costs the same wall time as a
     * genuine password check.
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private boolean isExpired(Url url) {
        return url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Persists one click: an atomic counter bump plus an analytics row.
     *
     * <p>Note the ordering — {@code incrementClickCount} is annotated
     * {@code @Modifying(clearAutomatically = true)}, so the persistence context is
     * cleared after it runs. The lazy reference for the analytics row must therefore
     * be obtained afterwards, or it would already be detached.
     */
    private void recordClick(CachedUrl cached, HttpServletRequest request) {

        int updated = urlRepository.incrementClickCount(cached.getId());
        if (updated == 0) {
            // The row was deleted between the cache read and this update. The cache
            // entry is now provably stale, so drop it rather than serve it again.
            log.warn("Click counter update affected 0 rows - evicting stale cache entry [shortCode={}]",
                    cached.getShortCode());
            urlCacheService.evict(cached.getShortCode());
            throw new UrlNotFoundException("Short URL not found");
        }

        UserAgentInfo agent = userAgentParser.parse(request.getHeader("User-Agent"));
        String ipAddress = clientIpResolver.resolve(request);

        ClickAnalytics analytics = ClickAnalytics.builder()
                // getReferenceById returns a lazy proxy: enough to populate the
                // foreign key without issuing a SELECT for a row we do not read.
                .url(urlRepository.getReferenceById(cached.getId()))
                .ipAddress(ipAddress)
                .browser(agent.browser())
                .operatingSystem(agent.operatingSystem())
                .device(agent.device())
                .build();

        clickAnalyticsRepository.save(analytics);

        log.debug("Click recorded [shortCode={}, browser={}, os={}, device={}, ip={}]",
                cached.getShortCode(), agent.browser(), agent.operatingSystem(),
                agent.device(), ipAddress);
    }

    // ==================================================================
    // Update / delete
    // ==================================================================

    @Override
    @Transactional
    public UrlResponse updateUrl(Long id, UrlUpdateRequest request, UserDetails userDetails) {

        User user = requireUser(userDetails);
        Url url = requireOwnedUrl(id, user);

        // Null means "leave unchanged" - see UrlUpdateRequest.
        if (request.getOriginalUrl() != null && !request.getOriginalUrl().isBlank()) {
            url.setOriginalUrl(request.getOriginalUrl());
        }
        if (request.getExpiresAt() != null) {
            url.setExpiresAt(request.getExpiresAt());
        }
        if (request.getActive() != null) {
            url.setActive(request.getActive());
        }

        // Removal wins over replacement: a client that sends both is contradicting
        // itself, and dropping protection is the safer reading of the two — it
        // cannot lock anyone out of a link by accident.
        if (Boolean.TRUE.equals(request.getRemovePassword())) {
            url.setPasswordHash(null);
        } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
            url.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        urlRepository.save(url);

        // Overwrite rather than evict: the fresh entity is already in hand, so a
        // put keeps the next redirect a cache hit.
        urlCacheService.put(url);

        log.info("Link updated [shortCode={}, userId={}, active={}, expiresAt={}, protected={}]",
                url.getShortCode(), user.getId(), url.getActive(), url.getExpiresAt(),
                url.isPasswordProtected());

        return urlMapper.toResponse(url);
    }

    @Override
    @Transactional
    public void deleteUrl(Long id, UserDetails userDetails) {

        User user = requireUser(userDetails);
        Url url = requireOwnedUrl(id, user);
        String shortCode = url.getShortCode();

        // ClickAnalytics has no cascade from Url, so its rows must go first or the
        // url_id foreign key blocks the delete.
        clickAnalyticsRepository.deleteByUrl(url);
        urlRepository.delete(url);

        urlCacheService.evict(shortCode);

        log.info("Link deleted [shortCode={}, userId={}]", shortCode, user.getId());
    }

    @Override
    @Transactional
    public UrlResponse toggleActive(Long id, UserDetails userDetails) {

        User user = requireUser(userDetails);
        Url url = requireOwnedUrl(id, user);

        url.setActive(!Boolean.TRUE.equals(url.getActive()));
        urlRepository.save(url);
        urlCacheService.put(url);

        log.info("Link {} [shortCode={}, userId={}]",
                url.getActive() ? "activated" : "deactivated", url.getShortCode(), user.getId());

        return urlMapper.toResponse(url);
    }

    // ==================================================================
    // Analytics
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ClickAnalyticsResponse> getAnalytics(
            String shortCode, UserDetails userDetails, int limit) {

        Url url = requireOwnedUrl(shortCode, requireUser(userDetails));

        // Bounded: the previous unpaged query loaded every click ever recorded.
        int safeLimit = Math.max(1, Math.min(limit, 1000));

        List<ClickAnalytics> clicks = clickAnalyticsRepository
                .findByUrlOrderByClickedAtDesc(url, PageRequest.of(0, safeLimit));

        return urlMapper.toClickResponseList(clicks);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(
            String shortCode, UserDetails userDetails, int days) {

        Url url = requireOwnedUrl(shortCode, requireUser(userDetails));
        int window = Math.max(1, Math.min(days, 365));

        return AnalyticsSummaryResponse.builder()
                .shortCode(shortCode)
                .totalClicks(clickAnalyticsRepository.countByUrl(url))
                .uniqueVisitors(clickAnalyticsRepository.countDistinctIpByUrl(url))
                .desktopUsers(clickAnalyticsRepository.countByUrlAndDevice(url, UserAgentInfo.DEVICE_DESKTOP))
                .mobileUsers(clickAnalyticsRepository.countByUrlAndDevice(url, UserAgentInfo.DEVICE_MOBILE))
                .tabletUsers(clickAnalyticsRepository.countByUrlAndDevice(url, UserAgentInfo.DEVICE_TABLET))
                .browsers(urlMapper.toLabelCounts(clickAnalyticsRepository.countGroupedByBrowser(url)))
                .operatingSystems(urlMapper.toLabelCounts(clickAnalyticsRepository.countGroupedByOperatingSystem(url)))
                .devices(urlMapper.toLabelCounts(clickAnalyticsRepository.countGroupedByDevice(url)))
                .clicksPerDay(buildDailySeries(url, window))
                .build();
    }

    /**
     * Builds a gap-free daily click series for the trend chart.
     *
     * <p>Aggregated in Java rather than SQL because date truncation is one of the
     * few genuinely non-portable pieces of SQL, and the integration tests run on H2
     * while production runs on Postgres. The query is bounded by the window, so the
     * row count stays proportional to recent traffic. If a single link ever sustains
     * enough volume for this to matter, replace it with a native
     * {@code date_trunc} aggregation behind this same method.
     */
    private List<DailyClickResponse> buildDailySeries(Url url, int days) {

        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(days - 1L).atStartOfDay();

        Map<LocalDate, Long> countsByDay = clickAnalyticsRepository
                .findByUrlAndClickedAtAfterOrderByClickedAtAsc(url, since)
                .stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickedAt().toLocalDate(),
                        Collectors.counting()));

        List<DailyClickResponse> series = new ArrayList<>(days);
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            series.add(DailyClickResponse.builder()
                    .date(day)
                    // Zero-fill: a chart drawn from a sparse series draws a straight
                    // line across missing days, which reads as steady traffic.
                    .count(countsByDay.getOrDefault(day, 0L))
                    .build());
        }
        return series;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(UserDetails userDetails) {

        User user = requireUser(userDetails);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        return DashboardStatsResponse.builder()
                .totalLinks(urlRepository.countByUser(user))
                .activeLinks(urlRepository.countByUserAndActiveTrue(user))
                .totalClicks(urlRepository.sumClickCountByUser(user))
                .clicksLast7Days(clickAnalyticsRepository.countByUserIdSince(user.getId(), weekAgo))
                .linksLast7Days(urlRepository.countByUserAndCreatedAtAfter(user, weekAgo))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertOwnership(String shortCode, UserDetails userDetails) {
        requireOwnedUrl(shortCode, requireUser(userDetails));
    }

    // ==================================================================
    // Shared guards
    // ==================================================================

    /**
     * Resolves the authenticated principal to a managed entity.
     *
     * <p>The principal supplied by Spring Security is a detached snapshot from token
     * validation time. Re-reading it keeps every write inside the current
     * persistence context and means a deleted account is caught here rather than
     * failing later with a foreign-key error.
     */
    private User requireUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedResourceAccessException("Authentication required");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Valid token presented for an account that no longer exists");
                    return new UserNotFoundException("Account no longer exists");
                });
    }

    /** Loads a link by id and asserts the caller owns it. */
    private Url requireOwnedUrl(Long id, User user) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("Link not found"));
        assertOwnedBy(url, user);
        return url;
    }

    /** Loads a link by short code and asserts the caller owns it. */
    private Url requireOwnedUrl(String shortCode, User user) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Link not found"));
        assertOwnedBy(url, user);
        return url;
    }

    private void assertOwnedBy(Url url, User user) {
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            log.warn("Ownership check failed [shortCode={}, requestedByUserId={}]",
                    url.getShortCode(), user.getId());
            throw new UnauthorizedResourceAccessException(
                    "You do not have access to this link");
        }
    }
}
