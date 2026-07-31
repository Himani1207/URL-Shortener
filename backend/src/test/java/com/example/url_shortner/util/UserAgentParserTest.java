package com.example.url_shortner.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the User-Agent detection that replaced the hardcoded "Unknown" values.
 *
 * <p>The strings below are real headers. Most of the risk in this parser is
 * ordering: nearly every browser embeds the tokens of the engine it is built on,
 * so a naive check reports Edge and Opera as Chrome, and Chrome as Safari.
 */
class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Nested
    @DisplayName("Browser detection")
    class BrowserDetection {

        @ParameterizedTest(name = "{1} -> {0}")
        @CsvSource(delimiter = '|', value = {
                "Chrome 120   | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Firefox 121  | Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
                "Safari 17    | Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15",
                "Edge 120     | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2210.91",
                "Opera 106    | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0",
                "Samsung Internet 23 | Mozilla/5.0 (Linux; Android 13; SAMSUNG SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36",
                "Vivaldi 6    | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Vivaldi/6.5.3206.39",
                "Internet Explorer 11 | Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko"
        })
        void identifiesBrowserAndMajorVersion(String expected, String userAgent) {
            assertThat(parser.parse(userAgent).browser()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Chromium derivatives are not misreported as Chrome")
        void chromiumDerivativesWinOverChrome() {
            // Every one of these also contains "Chrome/..."; if the ordering in the
            // parser regressed, all three would come back as Chrome.
            String edge = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2210.91";
            String opera = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0";

            assertThat(parser.parse(edge).browser()).startsWith("Edge");
            assertThat(parser.parse(opera).browser()).startsWith("Opera");
        }

        @Test
        @DisplayName("Chrome is not misreported as Safari")
        void chromeWinsOverSafariToken() {
            String chrome = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

            assertThat(parser.parse(chrome).browser()).startsWith("Chrome");
        }
    }

    @Nested
    @DisplayName("Operating system detection")
    class OperatingSystemDetection {

        @ParameterizedTest(name = "{1} -> {0}")
        @CsvSource(delimiter = '|', value = {
                "Windows 10/11 | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Windows 7     | Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36",
                "macOS 10      | Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15",
                "Android 13    | Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "iOS 17        | Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
                "Chrome OS     | Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Ubuntu        | Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0"
        })
        void identifiesOperatingSystem(String expected, String userAgent) {
            assertThat(parser.parse(userAgent).operatingSystem()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Android is not reported as Linux")
        void androidWinsOverLinuxToken() {
            // Android headers contain "Linux"; ordering has to put Android first.
            String android = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

            assertThat(parser.parse(android).operatingSystem()).startsWith("Android");
        }
    }

    @Nested
    @DisplayName("Device classification")
    class DeviceDetection {

        @ParameterizedTest(name = "{1} -> {0}")
        @CsvSource(delimiter = '|', value = {
                "Desktop | Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Desktop | Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15",
                "Mobile  | Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
                "Mobile  | Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "Tablet  | Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
                "Tablet  | Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        })
        void classifiesDevice(String expected, String userAgent) {
            assertThat(parser.parse(userAgent).device()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Android tablets are told apart from phones by the absence of 'Mobile'")
        void androidTabletVersusPhone() {
            String phone = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
            String tablet = "Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

            assertThat(parser.parse(phone).device()).isEqualTo(UserAgentInfo.DEVICE_MOBILE);
            assertThat(parser.parse(tablet).device()).isEqualTo(UserAgentInfo.DEVICE_TABLET);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
                "curl/8.4.0",
                "python-requests/2.31.0"
        })
        @DisplayName("Crawlers are bucketed as Bot so they do not inflate desktop stats")
        void crawlersAreNotCountedAsDesktop(String userAgent) {
            assertThat(parser.parse(userAgent).device()).isEqualTo(UserAgentInfo.DEVICE_BOT);
        }
    }

    @Nested
    @DisplayName("Degradation")
    class Degradation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("A missing header yields Unknown rather than throwing")
        void missingHeaderIsHandled(String userAgent) {
            UserAgentInfo info = parser.parse(userAgent);

            assertThat(info.browser()).isEqualTo(UserAgentInfo.UNKNOWN);
            assertThat(info.operatingSystem()).isEqualTo(UserAgentInfo.UNKNOWN);
            assertThat(info.device()).isEqualTo(UserAgentInfo.UNKNOWN);
        }

        @Test
        @DisplayName("Output always fits the varchar(255) analytics columns")
        void outputIsTruncatedToColumnWidth() {
            // The old code stored the raw header in `browser`; a header this long
            // would have failed on insert.
            String absurd = "Mozilla/5.0 " + "x".repeat(5_000);

            UserAgentInfo info = parser.parse(absurd);

            assertThat(info.browser().length()).isLessThanOrEqualTo(255);
            assertThat(info.operatingSystem().length()).isLessThanOrEqualTo(255);
            assertThat(info.device().length()).isLessThanOrEqualTo(255);
        }
    }
}
