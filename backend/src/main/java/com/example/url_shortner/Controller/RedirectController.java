package com.example.url_shortner.Controller;

import com.example.url_shortner.exception.UrlExpiredException;
import com.example.url_shortner.exception.UrlInactiveException;
import com.example.url_shortner.exception.UrlNotFoundException;
import com.example.url_shortner.exception.UrlPasswordRequiredException;
import com.example.url_shortner.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/**
 * Serves the public redirect at the root path.
 *
 * <p><b>Why this file was needed.</b> {@code SecurityConfig} already permitted
 * {@code /{shortCode}} anonymously, but no controller was mapped to it — the only
 * redirect lived at {@code /api/urls/{shortCode}}, which falls under
 * {@code anyRequest().authenticated()}. The practical effect was that every shared
 * short link demanded a JWT, so no link worked for the person it was sent to. This
 * controller fills in the route the security rules were already written for.
 *
 * <p><b>The path variable is regex-constrained</b> to the short-code alphabet.
 * Without it, {@code /{shortCode}} is greedy enough to swallow {@code /favicon.ico},
 * {@code /swagger-ui.html} and every other single-segment resource, because
 * controller mappings are consulted before static-resource handlers. Requiring 3-50
 * characters from {@code [A-Za-z0-9_-]} means anything containing a dot falls
 * through to the handler that should have had it.
 *
 * <p><b>Error handling deviates from the rest of the API on purpose.</b> These
 * requests come from a browser address bar, not from JavaScript, so answering a dead
 * link with a JSON error body would show the visitor a wall of braces. Failures
 * redirect to a human-readable page on the frontend instead.
 */
@Slf4j
@RestController
@Tag(name = "Redirect", description = "Public short-link resolution")
public class RedirectController {

    private final UrlService urlService;
    private final String frontendUrl;

    public RedirectController(
            UrlService urlService,
            @Value("${app.frontend-url:${app.cors.allowed-origins:http://localhost:5173}}") String frontendUrl) {

        this.urlService = urlService;
        // The CORS property may hold a comma-separated list; the first entry is the
        // primary origin and is the sensible place to send a human.
        String primary = frontendUrl.split(",")[0].trim();
        this.frontendUrl = primary.endsWith("/")
                ? primary.substring(0, primary.length() - 1)
                : primary;
    }

    @GetMapping("/{shortCode:[A-Za-z0-9_-]{3,50}}")
    @Operation(
            summary = "Resolve a short link",
            description = "Public endpoint. Issues a 302 to the destination and records the click.")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        try {
            String destination = urlService.getOriginalUrl(shortCode, request);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, destination)
                    // Must not be cached: a cached 302 would let the browser skip
                    // the server entirely, so later clicks would go uncounted and
                    // an edited destination would never take effect.
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .build();

        } catch (UrlPasswordRequiredException ex) {
            // Not a failure: the link is fine, the visitor simply has to prove they
            // were meant to have it. Sending them to the unlock page keeps the
            // whole exchange in the address bar, exactly like the redirect itself.
            return unlockPage(shortCode);

        } catch (UrlNotFoundException ex) {
            return errorPage(shortCode, "not-found");

        } catch (UrlExpiredException ex) {
            return errorPage(shortCode, "expired");

        } catch (UrlInactiveException ex) {
            return errorPage(shortCode, "inactive");
        }
    }

    /** Sends the visitor to the frontend's password prompt for this link. */
    private ResponseEntity<Void> unlockPage(String shortCode) {

        log.info("Serving unlock page [shortCode={}]", shortCode);

        String location = UriComponentsBuilder
                .fromUriString(frontendUrl + "/protected")
                .queryParam("code", shortCode)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    /**
     * Sends the visitor to the frontend's explanation page.
     *
     * <p>302 rather than 404 keeps the browser's behaviour simple and predictable;
     * the destination page states plainly what happened. The short code is echoed
     * back URL-encoded so it can be displayed without opening an injection hole.
     */
    private ResponseEntity<Void> errorPage(String shortCode, String reason) {

        log.info("Serving link-unavailable page [shortCode={}, reason={}]", shortCode, reason);

        String location = UriComponentsBuilder
                .fromUriString(frontendUrl + "/link-unavailable")
                .queryParam("code", shortCode)
                .queryParam("reason", reason)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
