package com.example.url_shortner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context starts and every bean wires up.
 *
 * <p>Now pinned to the {@code test} profile. Without it this test tried to reach the
 * developer's local PostgreSQL and Redis, so it failed on any machine that did not
 * happen to have both running — including CI.
 *
 * <p>Cheap but genuinely useful: it catches missing beans, circular dependencies and
 * broken configuration before any behavioural test runs.
 */
@SpringBootTest
@ActiveProfiles("test")
class UrlShortnerApplicationTests {

    @Test
    @DisplayName("application context loads")
    void contextLoads() {
        // Assertion-free by design: a failure to start throws.
    }
}
