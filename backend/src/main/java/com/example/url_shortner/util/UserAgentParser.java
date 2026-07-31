package com.example.url_shortner.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives browser, operating system and device class from a raw {@code User-Agent}
 * header.
 *
 * <p><b>Why this exists:</b> click analytics previously stored the entire raw
 * User-Agent string in the {@code browser} column and literal {@code "Unknown"}
 * for OS and device, which made the analytics summary meaningless (and risked
 * overflowing the varchar(255) column).
 *
 * <p><b>Why a hand-rolled parser:</b> the alternative libraries either ship a
 * signature database that goes stale (UserAgentUtils' was last refreshed around
 * 2015 and reports modern Chrome as "Chrome 4x") or pull in a multi-megabyte
 * YAML ruleset. Detection here covers the browser/OS families that make up the
 * overwhelming majority of real traffic and degrades gracefully to "Unknown".
 *
 * <p><b>Registered as a {@code @Component}</b> rather than a static utility so it
 * can be injected and stubbed in tests (Dependency Inversion), even though the
 * implementation itself is stateless and therefore thread-safe.
 */
@Component
public class UserAgentParser {

    /** Matches the varchar(255) width of the analytics columns. */
    private static final int MAX_FIELD_LENGTH = 255;

    // --- Browser signatures. Evaluation order matters: most browsers embed the
    // --- tokens of the engines they are built on (Edge and Opera both contain
    // --- "Chrome", Chrome contains "Safari"), so the most specific wins first.
    private static final Pattern EDGE = Pattern.compile("Edge?[A-Za-z]*/([\\d.]+)");
    private static final Pattern OPERA = Pattern.compile("(?:OPR|OPiOS|Opera)[/ ]([\\d.]+)");
    private static final Pattern VIVALDI = Pattern.compile("Vivaldi/([\\d.]+)");
    private static final Pattern SAMSUNG = Pattern.compile("SamsungBrowser/([\\d.]+)");
    private static final Pattern UC_BROWSER = Pattern.compile("UCBrowser/([\\d.]+)");
    private static final Pattern FIREFOX = Pattern.compile("(?:Firefox|FxiOS)/([\\d.]+)");
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|CriOS)/([\\d.]+)");
    private static final Pattern SAFARI = Pattern.compile("Version/([\\d.]+).*Safari");
    private static final Pattern IE_MODERN = Pattern.compile("Trident/.*rv:([\\d.]+)");
    private static final Pattern IE_LEGACY = Pattern.compile("MSIE ([\\d.]+)");

    // --- Operating system signatures.
    private static final Pattern WINDOWS_NT = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern ANDROID = Pattern.compile("Android ([\\d.]+)");
    private static final Pattern IOS = Pattern.compile("(?:iPhone )?OS ([\\d_]+) like Mac OS X");
    private static final Pattern MAC_OS = Pattern.compile("Mac OS X ([\\d_.]+)");

    // --- Crawlers are excluded from device stats so they do not inflate them.
    private static final Pattern BOT = Pattern.compile(
            "(?i)(bot|crawler|spider|crawling|slurp|facebookexternalhit|"
                    + "whatsapp|telegrambot|discordbot|preview|monitor|curl|wget|"
                    + "python-requests|okhttp|postman|headless)");

    /**
     * Parses the header into its three analytics dimensions.
     *
     * @param userAgent raw header value; may be {@code null} or blank
     * @return never {@code null} — falls back to {@link UserAgentInfo#unknown()}
     */
    public UserAgentInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UserAgentInfo.unknown();
        }
        return new UserAgentInfo(
                truncate(detectBrowser(userAgent)),
                truncate(detectOperatingSystem(userAgent)),
                truncate(detectDevice(userAgent)));
    }

    // ------------------------------------------------------------------
    // Browser
    // ------------------------------------------------------------------

    private String detectBrowser(String ua) {
        String version;

        // Chromium derivatives first — they all also advertise "Chrome".
        if ((version = match(EDGE, ua)) != null) return named("Edge", version);
        if ((version = match(OPERA, ua)) != null) return named("Opera", version);
        if ((version = match(VIVALDI, ua)) != null) return named("Vivaldi", version);
        if ((version = match(SAMSUNG, ua)) != null) return named("Samsung Internet", version);
        if ((version = match(UC_BROWSER, ua)) != null) return named("UC Browser", version);

        if ((version = match(FIREFOX, ua)) != null) return named("Firefox", version);
        if ((version = match(CHROME, ua)) != null) return named("Chrome", version);

        // Safari has no "Safari/<product version>" token — the user-visible
        // version lives in "Version/", and Chrome also carries a Safari token,
        // so this must run after the Chromium checks.
        if ((version = match(SAFARI, ua)) != null) return named("Safari", version);

        if ((version = match(IE_MODERN, ua)) != null) return named("Internet Explorer", version);
        if ((version = match(IE_LEGACY, ua)) != null) return named("Internet Explorer", version);

        // Non-browser clients still deserve a useful label rather than "Unknown".
        if (BOT.matcher(ua).find()) return "Bot / Crawler";

        return UserAgentInfo.UNKNOWN;
    }

    /** Keeps only the major version so cardinality stays low for grouping. */
    private String named(String browser, String version) {
        int dot = version.indexOf('.');
        String major = dot > 0 ? version.substring(0, dot) : version;
        return major.isBlank() ? browser : browser + " " + major;
    }

    // ------------------------------------------------------------------
    // Operating system
    // ------------------------------------------------------------------

    private String detectOperatingSystem(String ua) {
        String version;

        // Android must precede Linux: every Android UA also contains "Linux".
        if ((version = match(ANDROID, ua)) != null) {
            return "Android " + majorOf(version);
        }

        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            version = match(IOS, ua);
            return version != null ? "iOS " + majorOf(version.replace('_', '.')) : "iOS";
        }

        if ((version = match(WINDOWS_NT, ua)) != null) {
            return windowsName(version);
        }

        if ((version = match(MAC_OS, ua)) != null) {
            return "macOS " + majorOf(version.replace('_', '.'));
        }

        if (ua.contains("CrOS")) return "Chrome OS";
        if (ua.contains("Ubuntu")) return "Ubuntu";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("FreeBSD")) return "FreeBSD";

        return UserAgentInfo.UNKNOWN;
    }

    /**
     * Maps the {@code Windows NT <x.y>} kernel version to its marketing name.
     *
     * <p>Windows 11 reports {@code NT 10.0} exactly like Windows 10 — the two are
     * only distinguishable via Client Hints, which are not available here, so they
     * are reported together rather than guessed at.
     */
    private String windowsName(String ntVersion) {
        return switch (ntVersion) {
            case "10.0" -> "Windows 10/11";
            case "6.3" -> "Windows 8.1";
            case "6.2" -> "Windows 8";
            case "6.1" -> "Windows 7";
            case "6.0" -> "Windows Vista";
            case "5.1", "5.2" -> "Windows XP";
            default -> "Windows";
        };
    }

    // ------------------------------------------------------------------
    // Device class
    // ------------------------------------------------------------------

    private String detectDevice(String ua) {
        // Checked first so crawlers never land in the Desktop bucket.
        if (BOT.matcher(ua).find()) {
            return UserAgentInfo.DEVICE_BOT;
        }

        if (ua.contains("iPad")) {
            return UserAgentInfo.DEVICE_TABLET;
        }

        if (ua.contains("Tablet") || ua.contains("PlayBook") || ua.contains("Kindle")
                || ua.contains("Silk")) {
            return UserAgentInfo.DEVICE_TABLET;
        }

        // Android phones carry "Mobile"; Android tablets deliberately omit it.
        if (ua.contains("Android")) {
            return ua.contains("Mobile")
                    ? UserAgentInfo.DEVICE_MOBILE
                    : UserAgentInfo.DEVICE_TABLET;
        }

        if (ua.contains("iPhone") || ua.contains("iPod") || ua.contains("Windows Phone")
                || ua.contains("Mobi") || ua.contains("BlackBerry")) {
            return UserAgentInfo.DEVICE_MOBILE;
        }

        // A recognisable desktop OS is stronger evidence than the absence of a
        // mobile token, which would also match an empty or spoofed header.
        if (ua.contains("Windows") || ua.contains("Macintosh") || ua.contains("Linux")
                || ua.contains("CrOS") || ua.contains("X11")) {
            return UserAgentInfo.DEVICE_DESKTOP;
        }

        return UserAgentInfo.UNKNOWN;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String match(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String majorOf(String version) {
        int dot = version.indexOf('.');
        return dot > 0 ? version.substring(0, dot) : version;
    }

    private String truncate(String value) {
        return value.length() > MAX_FIELD_LENGTH
                ? value.substring(0, MAX_FIELD_LENGTH)
                : value;
    }
}
