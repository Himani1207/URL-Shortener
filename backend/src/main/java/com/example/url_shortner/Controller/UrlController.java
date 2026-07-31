package com.example.url_shortner.Controller;

import com.example.url_shortner.dto.request.UrlRequest;
import com.example.url_shortner.dto.request.UrlUpdateRequest;
import com.example.url_shortner.dto.response.AnalyticsSummaryResponse;
import com.example.url_shortner.dto.response.ClickAnalyticsResponse;
import com.example.url_shortner.dto.response.DashboardStatsResponse;
import com.example.url_shortner.dto.response.ErrorResponse;
import com.example.url_shortner.dto.response.UrlResponse;
import com.example.url_shortner.service.QrCodeService;
import com.example.url_shortner.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * Link management API.
 *
 * <p><b>All five original endpoints are preserved with identical paths and methods.</b>
 * The additions are what the dashboard needs — edit, delete, pause/resume, QR and
 * account totals — plus the authenticated principal now being passed to the
 * analytics endpoints so the service can enforce ownership.
 */
@Slf4j
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "Links", description = "Create, manage and analyse short links")
@SecurityRequirement(name = "bearerAuth")
public class UrlController {

    private final UrlService urlService;
    private final QrCodeService qrCodeService;

    // ==================================================================
    // Original endpoints
    // ==================================================================

    @PostMapping
    @Operation(summary = "Create a short link",
            description = "Generates a random code, or claims the supplied custom alias.")
    @ApiResponse(responseCode = "201", description = "Link created")
    @ApiResponse(responseCode = "409", description = "Alias already taken",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody UrlRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UrlResponse created = urlService.createShortUrl(request, userDetails);

        // 201 with a Location header is the correct semantic for a create; the
        // previous 200 was harmless but told the client less.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, created.getShortUrl())
                .body(created);
    }

    @GetMapping
    @Operation(summary = "List my links", description = "Newest first.")
    public ResponseEntity<List<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.getMyUrls(userDetails));
    }

    /**
     * Legacy redirect endpoint, kept for backwards compatibility.
     *
     * <p>New traffic should use the root-level {@code /{shortCode}} route served by
     * {@link RedirectController} — that is the link that actually gets shared, and
     * it is the one {@code SecurityConfig} was already written to permit.
     */
    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to the destination (legacy path)",
            description = "Prefer GET /{shortCode} at the root. Records a click either way.")
    @ApiResponse(responseCode = "302", description = "Redirect issued")
    @ApiResponse(responseCode = "410", description = "Link expired or deactivated")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String originalUrl = urlService.getOriginalUrl(shortCode, request);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @GetMapping("/{shortCode}/analytics")
    @Operation(summary = "Click history for a link",
            description = "Newest first. Only the link's owner may read this.")
    @ApiResponse(responseCode = "403", description = "Not your link")
    public ResponseEntity<List<ClickAnalyticsResponse>> getAnalytics(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.getAnalytics(shortCode, userDetails, limit));
    }

    @GetMapping("/{shortCode}/summary")
    @Operation(summary = "Aggregated analytics for a link",
            description = "Totals, unique visitors, device/browser/OS breakdowns and a daily series.")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.getSummary(shortCode, userDetails, days));
    }

    // ==================================================================
    // Added endpoints
    // ==================================================================

    /**
     * QR code as a PNG.
     *
     * <p>Owner-only, consistent with the analytics endpoints. The frontend fetches it
     * with the bearer token and renders the response as an object URL rather than
     * pointing an {@code <img src>} straight at it, since an image tag cannot carry
     * an Authorization header.
     */
    @GetMapping(value = "/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "QR code for a link",
            description = "Returns a PNG encoding the short URL, so scans are counted as clicks.")
    @ApiResponse(responseCode = "200", description = "PNG image",
            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE))
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserDetails userDetails) {

        urlService.assertOwnership(shortCode, userDetails);

        // Only the default size is served from Redis; see QrCodeService for why.
        byte[] png = (size == null || size == qrCodeService.defaultSize())
                ? qrCodeService.getQrCodeForShortCode(shortCode)
                : qrCodeService.renderQrCode(shortCode, size);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"qr-" + shortCode + ".png\"")
                .body(png);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a link",
            description = "Partial update: null fields are left unchanged. The short code is immutable.")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable Long id,
            @Valid @RequestBody UrlUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.updateUrl(id, request, userDetails));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Pause or resume a link",
            description = "Flips the active flag without deleting the link or its history.")
    public ResponseEntity<UrlResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.toggleActive(id, userDetails));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a link",
            description = "Also removes its click history. Not reversible.")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        urlService.deleteUrl(id, userDetails);
        return ResponseEntity.noContent().build();
    }

    /**
     * Account-level totals.
     *
     * <p>Mapped before {@code /{shortCode}} in intent, though Spring resolves this
     * regardless: a literal path segment always outranks a template. "stats" is in
     * the reserved-alias list so no link can ever claim it.
     */
    @GetMapping("/stats")
    @Operation(summary = "Dashboard totals",
            description = "Aggregates computed in the database rather than by summing the link list client-side.")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.getDashboardStats(userDetails));
    }
}
