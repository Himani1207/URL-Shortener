package com.example.url_shortner.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the originating client IP address for a request.
 *
 * <p><b>Why this exists:</b> the redirect handler previously used
 * {@code request.getRemoteAddr()} directly. As soon as the application sits behind
 * any reverse proxy or load balancer, that method returns the <i>proxy's</i>
 * address, so every recorded click would share one meaningless IP.
 *
 * <p><b>Security note:</b> forwarding headers are client-controlled and trivially
 * spoofed. They are only trustworthy when the app is guaranteed to sit behind a
 * proxy that overwrites them. Where the app is reachable directly, the recorded IP
 * should be treated as advisory rather than authoritative. It is used here purely
 * for analytics, never for authorisation.
 */
@Component
public class ClientIpResolver {

    /** Longest possible textual IPv6 address, matching the column width budget. */
    private static final int MAX_IP_LENGTH = 45;

    /**
     * Checked in order of specificity: platform-specific headers first (a CDN sets
     * exactly one client IP), then the generic proxy chain.
     */
    private static final List<String> CANDIDATE_HEADERS = List.of(
            "CF-Connecting-IP",     // Cloudflare
            "True-Client-IP",       // Akamai / Cloudflare Enterprise
            "X-Real-IP",            // Nginx convention
            "X-Forwarded-For"       // de-facto standard proxy chain
    );

    /**
     * @param request current servlet request
     * @return the best available client IP, or {@code "Unknown"} if none could be
     *         determined. Never {@code null}.
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UserAgentInfo.UNKNOWN;
        }

        for (String header : CANDIDATE_HEADERS) {
            String value = request.getHeader(header);
            String candidate = firstValidAddress(value);
            if (candidate != null) {
                return candidate;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return isUsable(remoteAddr) ? truncate(remoteAddr) : UserAgentInfo.UNKNOWN;
    }

    /**
     * {@code X-Forwarded-For} is a comma-separated chain of the form
     * {@code client, proxy1, proxy2}. The left-most entry is the original client.
     */
    private String firstValidAddress(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        for (String part : headerValue.split(",")) {
            String candidate = part.trim();
            if (isUsable(candidate)) {
                return truncate(candidate);
            }
        }
        return null;
    }

    /** Filters out blanks and the literal "unknown" some proxies insert. */
    private boolean isUsable(String value) {
        return value != null
                && !value.isBlank()
                && !"unknown".equalsIgnoreCase(value);
    }

    private String truncate(String value) {
        return value.length() > MAX_IP_LENGTH
                ? value.substring(0, MAX_IP_LENGTH)
                : value;
    }
}
