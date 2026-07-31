package com.example.url_shortner.integration;

import com.example.url_shortner.dto.request.RegisterRequest;
import com.example.url_shortner.dto.request.UrlRequest;
import com.example.url_shortner.dto.request.UrlUpdateRequest;
import com.example.url_shortner.repository.ClickAnalyticsRepository;
import com.example.url_shortner.repository.UrlRepository;
import com.example.url_shortner.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for link creation, redirection, analytics and QR generation.
 *
 * <p>Deliberately <b>not</b> {@code @Transactional}. The redirect path writes through
 * a bulk {@code UPDATE} and then reads the counter back, and a rolled-back test
 * transaction would hide whether that write actually reached the database. Each test
 * cleans up explicitly instead.
 *
 * <p>The most important case here is {@link #redirectIsPublic()}: the shared link has
 * to work for a recipient who has never signed in. Before the root
 * {@code RedirectController} existed, the only redirect route sat behind
 * {@code anyRequest().authenticated()}, so every shared link demanded a JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Links API")
class UrlControllerIT {

    private static final String DESTINATION = "https://www.example.com/a/long/destination/path";
    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UrlRepository urlRepository;
    @Autowired private ClickAnalyticsRepository clickAnalyticsRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        clickAnalyticsRepository.deleteAll();
        urlRepository.deleteAll();
        userRepository.deleteAll();
        token = registerAndGetToken("owner@example.com");
    }

    // ==================================================================
    // Creation
    // ==================================================================

    @Test
    @DisplayName("POST /api/urls creates a link and returns the fully qualified short URL")
    void createsLink() throws Exception {
        mockMvc.perform(authed(post("/api/urls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder().originalUrl(DESTINATION).build())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.originalUrl").value(DESTINATION))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("a custom alias is honoured")
    void createsLinkWithCustomAlias() throws Exception {
        mockMvc.perform(authed(post("/api/urls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder()
                                .originalUrl(DESTINATION)
                                .customAlias("my-campaign")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("my-campaign"));
    }

    @Test
    @DisplayName("a duplicate alias returns 409")
    void duplicateAliasReturnsConflict() throws Exception {
        String body = json(UrlRequest.builder()
                .originalUrl(DESTINATION).customAlias("taken-alias").build());

        mockMvc.perform(authed(post("/api/urls"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(post("/api/urls"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a reserved alias returns 400")
    void reservedAliasIsRejected() throws Exception {
        // "api" would shadow the API itself, since redirects live at the root.
        mockMvc.perform(authed(post("/api/urls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder()
                                .originalUrl(DESTINATION).customAlias("api").build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a missing destination returns 400 rather than persisting a null")
    void missingDestinationIsRejected() throws Exception {
        // @Pattern alone skips nulls; @NotBlank is what closes this.
        mockMvc.perform(authed(post("/api/urls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.originalUrl").isNotEmpty());
    }

    @Test
    @DisplayName("creating a link without a token returns 401")
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder().originalUrl(DESTINATION).build())))
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    // Redirect
    // ==================================================================

    @Test
    @DisplayName("GET /{shortCode} redirects anonymously - a shared link works for the recipient")
    void redirectIsPublic() throws Exception {
        String shortCode = createLink(DESTINATION, null);

        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", DESTINATION))
                // A cached 302 would let the browser skip the server, so later
                // clicks would go uncounted and an edited destination never apply.
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    @DisplayName("a redirect increments the click counter")
    void redirectCountsClick() throws Exception {
        String shortCode = createLink(DESTINATION, null);

        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA))
                .andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA))
                .andExpect(status().isFound());

        assertThat(urlRepository.findByShortCode(shortCode).orElseThrow().getClickCount())
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("a redirect records real browser, OS and device values")
    void redirectRecordsRealAnalytics() throws Exception {
        String shortCode = createLink(DESTINATION, null);

        mockMvc.perform(get("/" + shortCode)
                        .header("User-Agent", CHROME_UA)
                        .header("X-Forwarded-For", "198.51.100.42"))
                .andExpect(status().isFound());

        var click = clickAnalyticsRepository.findAll().get(0);

        // Previously these were the literal string "Unknown".
        assertThat(click.getBrowser()).isEqualTo("Chrome 120");
        assertThat(click.getOperatingSystem()).isEqualTo("Windows 10/11");
        assertThat(click.getDevice()).isEqualTo("Desktop");
        assertThat(click.getIpAddress()).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("an unknown code redirects to the frontend explanation page")
    void unknownCodeGoesToErrorPage() throws Exception {
        // A browser visitor gets a readable page, not a JSON error body.
        mockMvc.perform(get("/doesnotexist"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/link-unavailable")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("reason=not-found")));
    }

    @Test
    @DisplayName("an expired link is not served")
    void expiredLinkIsNotServed() throws Exception {
        String shortCode = createLink(DESTINATION, LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("reason=expired")));

        assertThat(clickAnalyticsRepository.count()).isZero();
    }

    @Test
    @DisplayName("a deactivated link is not served")
    void deactivatedLinkIsNotServed() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        Long id = urlRepository.findByShortCode(shortCode).orElseThrow().getId();

        mockMvc.perform(authed(patch("/api/urls/" + id + "/toggle")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("reason=inactive")));
    }

    // ==================================================================
    // Listing and analytics
    // ==================================================================

    @Test
    @DisplayName("GET /api/urls lists only the caller's links")
    void listReturnsOnlyOwnLinks() throws Exception {
        createLink(DESTINATION, null);

        String otherToken = registerAndGetToken("someone.else@example.com");
        mockMvc.perform(post("/api/urls")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder()
                                .originalUrl("https://other.example.com").build())))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/api/urls")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].originalUrl").value(DESTINATION));
    }

    @Test
    @DisplayName("GET /api/urls/{code}/analytics returns the click history")
    void analyticsReturnsHistory() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA));

        mockMvc.perform(authed(get("/api/urls/" + shortCode + "/analytics")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].browser").value("Chrome 120"))
                .andExpect(jsonPath("$[0].device").value("Desktop"));
    }

    @Test
    @DisplayName("GET /api/urls/{code}/summary aggregates correctly and zero-fills the series")
    void summaryAggregates() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA));

        mockMvc.perform(authed(get("/api/urls/" + shortCode + "/summary?days=7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1))
                .andExpect(jsonPath("$.desktopUsers").value(1))
                .andExpect(jsonPath("$.mobileUsers").value(0))
                .andExpect(jsonPath("$.uniqueVisitors").value(1))
                .andExpect(jsonPath("$.browsers[0].label").value("Chrome 120"))
                .andExpect(jsonPath("$.clicksPerDay", org.hamcrest.Matchers.hasSize(7)));
    }

    @Test
    @DisplayName("another user cannot read someone else's analytics")
    void analyticsAreOwnerOnly() throws Exception {
        // The security hole this closes: any authenticated user could previously
        // read any link's click history, visitor IPs included.
        String shortCode = createLink(DESTINATION, null);
        String intruderToken = registerAndGetToken("intruder@example.com");

        mockMvc.perform(get("/api/urls/" + shortCode + "/analytics")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/urls/stats returns account totals")
    void dashboardStats() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA));

        mockMvc.perform(authed(get("/api/urls/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinks").value(1))
                .andExpect(jsonPath("$.activeLinks").value(1))
                .andExpect(jsonPath("$.totalClicks").value(1));
    }

    // ==================================================================
    // Update / delete / QR
    // ==================================================================

    @Test
    @DisplayName("PUT /api/urls/{id} updates the destination and the redirect follows it")
    void updateChangesDestination() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        Long id = urlRepository.findByShortCode(shortCode).orElseThrow().getId();

        mockMvc.perform(authed(put("/api/urls/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlUpdateRequest.builder()
                                .originalUrl("https://updated.example.com").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://updated.example.com"));

        // Proves the cache was refreshed rather than left serving the old target.
        mockMvc.perform(get("/" + shortCode))
                .andExpect(header().string("Location", "https://updated.example.com"));
    }

    @Test
    @DisplayName("DELETE /api/urls/{id} removes the link and its history")
    void deleteRemovesLinkAndHistory() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        mockMvc.perform(get("/" + shortCode).header("User-Agent", CHROME_UA));
        Long id = urlRepository.findByShortCode(shortCode).orElseThrow().getId();

        mockMvc.perform(authed(delete("/api/urls/" + id)))
                .andExpect(status().isNoContent());

        assertThat(urlRepository.findByShortCode(shortCode)).isEmpty();
        assertThat(clickAnalyticsRepository.count()).isZero();
    }

    @Test
    @DisplayName("another user cannot delete someone else's link")
    void deleteIsOwnerOnly() throws Exception {
        String shortCode = createLink(DESTINATION, null);
        Long id = urlRepository.findByShortCode(shortCode).orElseThrow().getId();
        String intruderToken = registerAndGetToken("intruder2@example.com");

        mockMvc.perform(delete("/api/urls/" + id)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());

        assertThat(urlRepository.findByShortCode(shortCode)).isPresent();
    }

    @Test
    @DisplayName("GET /api/urls/{code}/qr returns a real PNG")
    void qrEndpointReturnsPng() throws Exception {
        String shortCode = createLink(DESTINATION, null);

        byte[] png = mockMvc.perform(authed(get("/api/urls/" + shortCode + "/qr")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(png).isNotEmpty();
        // PNG magic number: 89 50 4E 47
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(new String(new byte[]{png[1], png[2], png[3]})).isEqualTo("PNG");
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authed(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private String createLink(String destination, LocalDateTime expiresAt) throws Exception {
        String response = mockMvc.perform(authed(post("/api/urls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UrlRequest.builder()
                                .originalUrl(destination)
                                .expiresAt(expiresAt)
                                .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("shortCode").asText();
    }

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword("S3curePassw0rd!");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
