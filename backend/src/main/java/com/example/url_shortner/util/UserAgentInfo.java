package com.example.url_shortner.util;

/**
 * Immutable result of parsing a raw {@code User-Agent} header.
 *
 * <p>Lives in {@code util} because it is a pure value object with no persistence
 * or transport concerns — {@link com.example.url_shortner.service.impl.UrlServiceImpl}
 * maps it onto a {@link com.example.url_shortner.entity.ClickAnalytics} row.
 *
 * <p>The {@link #device()} values are deliberately constrained to
 * {@code Desktop | Mobile | Tablet | Bot | Unknown} because
 * {@code ClickAnalyticsRepository.countByUrlAndDevice} queries those exact
 * literals when building the analytics summary.
 */
public record UserAgentInfo(String browser, String operatingSystem, String device) {

    public static final String UNKNOWN = "Unknown";

    public static final String DEVICE_DESKTOP = "Desktop";
    public static final String DEVICE_MOBILE = "Mobile";
    public static final String DEVICE_TABLET = "Tablet";
    public static final String DEVICE_BOT = "Bot";

    /** Fallback used when the User-Agent header is absent or unparseable. */
    public static UserAgentInfo unknown() {
        return new UserAgentInfo(UNKNOWN, UNKNOWN, UNKNOWN);
    }
}
