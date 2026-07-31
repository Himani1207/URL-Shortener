package com.example.url_shortner.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Set;

/**
 * Generates and validates short codes.
 *
 * <p><b>Why this was extracted from {@code UrlServiceImpl}:</b>
 * <ul>
 *   <li><b>Security</b> — the previous inline generator used {@link java.util.Random},
 *       which is a linear congruential PRNG. Observing a handful of generated codes
 *       is enough to recover its seed and enumerate every other user's links.
 *       {@link SecureRandom} removes that class of attack.</li>
 *   <li><b>Single Responsibility</b> — code generation and alias validation are a
 *       distinct concern from URL persistence, and are now independently testable.</li>
 *   <li><b>Route safety</b> — because redirects are served from the root path
 *       ({@code /{shortCode}}), a custom alias such as "api" or "swagger-ui" would
 *       shadow a real application route. {@link #isReserved(String)} blocks those.</li>
 * </ul>
 */
@Component
public class ShortCodeGenerator {

    /** Base62 alphabet: URL-safe, no encoding required, case-sensitive. */
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Path prefixes that must never be claimed as a custom alias, because the
     * root-level redirect mapping would otherwise compete with them.
     */
    private static final Set<String> RESERVED = Set.of(
            "api", "auth", "login", "register", "signup", "signin", "logout",
            "swagger-ui", "v3", "actuator", "admin", "dashboard", "settings",
            "analytics", "links", "qr", "stats", "static", "assets", "favicon.ico",
            "robots.txt", "sitemap.xml", "health", "docs", "pricing", "about",
            "terms", "privacy", "support", "help", "app", "www",
            // Frontend routes added with the create/unlock flow. An alias matching
            // one of these would be unreachable: the client-side router would claim
            // the path before the request ever left the browser.
            "create", "protected", "link-unavailable"
    );

    /** Custom aliases are restricted to characters that never need URL-escaping. */
    private static final String ALIAS_PATTERN = "^[A-Za-z0-9_-]{3,50}$";

    private final SecureRandom secureRandom = new SecureRandom();

    private final int codeLength;

    public ShortCodeGenerator(@Value("${app.shortcode.length:7}") int codeLength) {
        this.codeLength = codeLength;
    }

    /**
     * @return a cryptographically random base62 code of the configured length.
     *         At the default length of 7 the keyspace is 62^7 ≈ 3.5 x 10^12.
     */
    public String generate() {
        StringBuilder code = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** @return true when the alias is syntactically acceptable as a URL path segment. */
    public boolean isValidAlias(String alias) {
        return alias != null && alias.matches(ALIAS_PATTERN);
    }

    /** @return true when the alias would collide with an application route. */
    public boolean isReserved(String alias) {
        return alias != null && RESERVED.contains(alias.toLowerCase());
    }
}
