package com.example.url_shortner.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JWT issuance and validation.
 *
 * <p>{@code secretKey} and {@code jwtExpiration} are {@code @Value}-injected fields.
 * Outside a Spring context nothing populates them, so they are set reflectively —
 * which also lets the expiry test use a negative lifetime to mint an
 * already-expired token without making the test sleep.
 */
class JWTServiceTest {

    /** 64 Base64 bytes; HS256 requires at least 256 bits of key material. */
    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11cmwtc2hvcnRlbmVyLWludGVncmF0aW9uLXRlc3Rz";

    private static final String EMAIL = "kunal@example.com";

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
    }

    @Test
    @DisplayName("A generated token round-trips its subject")
    void generatesTokenCarryingTheEmail() {
        String token = jwtService.generateToken(EMAIL);

        assertThat(token).isNotBlank();
        // Header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.extractUsername(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("A freshly issued token is valid for its subject")
    void freshTokenIsValid() {
        String token = jwtService.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(token, EMAIL)).isTrue();
    }

    @Test
    @DisplayName("A token is not valid for a different user")
    void tokenIsRejectedForAnotherSubject() {
        // Guards against a validation shortcut that checks only the signature: a
        // real token belonging to someone else must not authenticate this user.
        String token = jwtService.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(token, "someone.else@example.com")).isFalse();
    }

    @Test
    @DisplayName("An expired token is rejected")
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1_000L);
        String expired = jwtService.generateToken(EMAIL);

        // jjwt refuses to parse an expired token at all, so validation surfaces as
        // this exception rather than a false return. JWTAuthenticationFilter catches
        // exactly this and answers 401 instead of letting it escape as a 500.
        assertThatThrownBy(() -> jwtService.isTokenValid(expired, EMAIL))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("A token signed with a different key is rejected")
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreignToken = jwtService.generateToken(EMAIL);

        JWTService otherService = new JWTService();
        ReflectionTestUtils.setField(otherService, "secretKey",
                "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtY29tcGxldGVseS1kaWZmZXJlbnQhIQ==");
        ReflectionTestUtils.setField(otherService, "jwtExpiration", 3_600_000L);

        assertThatThrownBy(() -> otherService.extractUsername(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("A tampered payload invalidates the signature")
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(EMAIL);
        // Corrupt the final signature character.
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Two tokens for the same subject are both independently valid")
    void repeatedIssuanceStaysValid() {
        String first = jwtService.generateToken(EMAIL);
        String second = jwtService.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(first, EMAIL)).isTrue();
        assertThat(jwtService.isTokenValid(second, EMAIL)).isTrue();
    }
}
