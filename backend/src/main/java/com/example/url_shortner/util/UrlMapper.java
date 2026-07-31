package com.example.url_shortner.util;

import com.example.url_shortner.dto.response.ClickAnalyticsResponse;
import com.example.url_shortner.dto.response.LabelCountResponse;
import com.example.url_shortner.dto.response.UrlResponse;
import com.example.url_shortner.entity.ClickAnalytics;
import com.example.url_shortner.entity.Url;
import com.example.url_shortner.repository.projection.LabelCount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
public class UrlMapper {

    private final String baseUrl;

    public UrlMapper(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
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
        return baseUrl + "/" + shortCode;
    }
}
