package com.example.url_shortner.util;

import com.example.url_shortner.dto.response.ClickAnalyticsResponse;
import com.example.url_shortner.dto.response.LabelCountResponse;
import com.example.url_shortner.dto.response.UrlResponse;
import com.example.url_shortner.entity.ClickAnalytics;
import com.example.url_shortner.entity.Url;
import com.example.url_shortner.repository.projection.LabelCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converts entities to their response DTOs.
 *
 * <p><b>Why this exists:</b> the same {@code UrlResponse.builder()...build()} block
 * was repeated in {@code createShortUrl} and {@code getMyUrls}, and adding the
 * {@code shortUrl}, {@code createdAt} and {@code expired} fields would have meant
 * repeating it in every new endpoint (update, toggle, get-one). One mapper keeps
 * the projection in a single place, so a field added to the DTO cannot be
 * accidentally populated in one endpoint and left null in another.
 *
 * <p><b>Where it belongs:</b> {@code util}, alongside the other stateless helpers.
 * It is a {@code @Component} rather than a class of static methods because it needs
 * the injected public base URL to assemble {@code shortUrl}.
 *
 * <p><b>Why not MapStruct or ModelMapper:</b> the mappings are few and involve
 * derived fields; a code generator or a reflective mapper would add a build-time
 * dependency and hide logic that reads perfectly clearly as plain Java.
 */
@Component
@Slf4j
public class UrlMapper {

    private static final String LOCAL_FALLBACK = "http://localhost:8080";

    /** Configured public origin, or {@code null} when {@code app.base-url} is unset. */
    private final String configuredBaseUrl;

    public UrlMapper(@Value("${app.base-url:}") String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();

        if (trimmed.isEmpty()) {
            this.configuredBaseUrl = null;
            log.warn("app.base-url is not set - short links will be built from the "
                    + "incoming request. Set APP_BASE_URL to the public origin in any "
                    + "environment sitting behind a proxy or load balancer.");
        } else {
            this.configuredBaseUrl = trimmed.endsWith("/")
                    ? trimmed.substring(0, trimmed.length() - 1)
                    : trimmed;
        }
    }

    /**
     * The origin that short links are built from.
     *
     * <p><b>Why this is not simply a constructor value with a localhost default.</b>
     * It used to be, and the default was silently wrong in exactly the place it
     * mattered: deployed with {@code APP_BASE_URL} unset, every link came back as
     * {@code http://localhost:8080/<code>}. Nothing failed — the link was stored
     * correctly and resolved correctly on the real host — but the address handed to
     * the user pointed at their own machine, so clicking it produced "this link does
     * not exist" from whatever happened to be running locally. A wrong answer that
     * looks like a working one is the worst kind.
     *
     * <p>Explicit configuration still wins and is what production should use. When it
     * is absent the origin is derived from the current request, which is correct
     * behind Render's proxy because {@code server.forward-headers-strategy: framework}
     * makes Spring honour the {@code X-Forwarded-*} headers. Outside a request — the
     * expiration scheduler, tests — there is nothing to derive from, so the localhost
     * fallback remains.
     */
    private String baseUrl() {
        if (configuredBaseUrl != null) return configuredBaseUrl;

        try {
            String derived = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .build()
                    .toUriString();
            return derived.endsWith("/")
                    ? derived.substring(0, derived.length() - 1)
                    : derived;
        } catch (IllegalStateException ex) {
            // No request bound to this thread.
            return LOCAL_FALLBACK;
        }
    }

    /** Maps a link entity, deriving {@code shortUrl} and {@code expired}. */
    public UrlResponse toResponse(Url url) {
        boolean expired = url.getExpiresAt() != null
                && url.getExpiresAt().isBefore(LocalDateTime.now());

        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(buildShortUrl(url.getShortCode()))
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .active(url.getActive())
                .expired(expired)
                .passwordProtected(url.isPasswordProtected())
                .build();
    }

    public List<UrlResponse> toResponseList(List<Url> urls) {
        return urls.stream().map(this::toResponse).toList();
    }

    public ClickAnalyticsResponse toResponse(ClickAnalytics click) {
        return ClickAnalyticsResponse.builder()
                .ipAddress(click.getIpAddress())
                .browser(click.getBrowser())
                .operatingSystem(click.getOperatingSystem())
                .device(click.getDevice())
                .clickedAt(click.getClickedAt())
                .build();
    }

    public List<ClickAnalyticsResponse> toClickResponseList(List<ClickAnalytics> clicks) {
        return clicks.stream().map(this::toResponse).toList();
    }

    /**
     * Converts repository projections to transport DTOs, so the persistence-layer
     * interface never reaches a controller.
     */
    public List<LabelCountResponse> toLabelCounts(List<LabelCount> projections) {
        return projections.stream()
                .map(p -> LabelCountResponse.builder()
                        .label(p.getLabel() == null ? UserAgentInfo.UNKNOWN : p.getLabel())
                        .count(p.getCount())
                        .build())
                .toList();
    }

    /** @return the fully qualified short link for a code. */
    public String buildShortUrl(String shortCode) {
        return baseUrl() + "/" + shortCode;
    }
}
