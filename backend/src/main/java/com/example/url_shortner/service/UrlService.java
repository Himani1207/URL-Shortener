package com.example.url_shortner.service;

import com.example.url_shortner.dto.request.UrlRequest;
import com.example.url_shortner.dto.request.UrlUpdateRequest;
import com.example.url_shortner.dto.response.AnalyticsSummaryResponse;
import com.example.url_shortner.dto.response.ClickAnalyticsResponse;
import com.example.url_shortner.dto.response.DashboardStatsResponse;
import com.example.url_shortner.dto.response.UrlResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Business operations on short links.
 *
 * <p><b>Signature change on the analytics methods.</b> {@code getAnalytics} and
 * {@code getSummary} now take the authenticated principal. Previously they took only
 * a short code and performed no ownership check, so any logged-in user could read
 * any other user's click history — including visitor IP addresses — just by knowing
 * or guessing a short code. The HTTP endpoints are unchanged; the controller simply
 * passes the principal it already has.
 */
public interface UrlService {

    // ------------------------------------------------------------------
    // Existing operations
    // ------------------------------------------------------------------

    UrlResponse createShortUrl(UrlRequest request, UserDetails userDetails);

    /** Links owned by the caller, newest first. */
    List<UrlResponse> getMyUrls(UserDetails userDetails);

    /**
     * Resolves a short code for redirection and records the click.
     *
     * <p>Signature deliberately unchanged. Reads go through
     * {@link UrlCacheService}; the click counter and the analytics row are always
     * written, so caching never costs a click.
     *
     * @return the destination URL
     * @throws com.example.url_shortner.exception.UrlPasswordRequiredException when
     *         the link is password-protected, in which case nothing is recorded and
     *         the caller must route the visitor through {@link #unlock}
     */
    String getOriginalUrl(String shortCode, HttpServletRequest request);

    /**
     * Verifies a visitor password and resolves the link.
     *
     * <p>Public and unauthenticated by necessity — the person opening a shared link
     * is not the account holder. The click is recorded here rather than in
     * {@link #getOriginalUrl}, so a failed password attempt never inflates the
     * link's numbers.
     *
     * @return the destination URL
     */
    String unlock(String shortCode, String password, HttpServletRequest request);

    /**
     * Click history for a link, newest first.
     *
     * @param limit maximum rows to return; bounds the response for busy links
     */
    List<ClickAnalyticsResponse> getAnalytics(String shortCode, UserDetails userDetails, int limit);

    /**
     * Aggregated analytics for a link.
     *
     * @param days size of the trend window in days
     */
    AnalyticsSummaryResponse getSummary(String shortCode, UserDetails userDetails, int days);

    // ------------------------------------------------------------------
    // Added for the dashboard
    // ------------------------------------------------------------------

    /** Single link by id, scoped to the caller. */
    UrlResponse getUrl(Long id, UserDetails userDetails);

    /** Partial update; {@code null} fields on the request are left unchanged. */
    UrlResponse updateUrl(Long id, UrlUpdateRequest request, UserDetails userDetails);

    /** Deletes a link and its click history. */
    void deleteUrl(Long id, UserDetails userDetails);

    /** Flips the active flag, letting an owner pause a link without deleting it. */
    UrlResponse toggleActive(Long id, UserDetails userDetails);

    /** Account-level totals for the dashboard tiles. */
    DashboardStatsResponse getDashboardStats(UserDetails userDetails);

    /**
     * Asserts the caller owns the link and returns it.
     * Used by the QR endpoint, which needs the ownership check but not a DTO.
     */
    void assertOwnership(String shortCode, UserDetails userDetails);
}
