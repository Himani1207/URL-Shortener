package com.example.url_shortner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI document and Swagger UI configuration.
 *
 * <p><b>Why this file is new:</b> the project was described as having Swagger with
 * JWT authorisation, but neither the {@code springdoc-openapi} dependency nor any
 * configuration existed — {@code /swagger-ui.html} returned 404. The dependency is
 * now in {@code pom.xml} and this class supplies the document metadata plus the
 * bearer-token security scheme.
 *
 * <p><b>What the security scheme buys you:</b> it puts an "Authorize" button in
 * Swagger UI. Paste a JWT once and every subsequent "Try it out" carries the
 * {@code Authorization: Bearer ...} header automatically, instead of having to be
 * pasted per request. Endpoints opt in through {@code @SecurityRequirement} on the
 * controller, which is why the public redirect correctly shows as needing no auth.
 *
 * <p>Swagger UI is enabled here for local development. Whether to expose it beyond
 * that is a hosting decision — {@code springdoc.swagger-ui.enabled} switches it off
 * without a code change.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI urlShortenerOpenApi() {

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Paste the token returned by POST /api/auth/login or /api/auth/register.
                        Do not include the "Bearer " prefix — Swagger adds it.
                        """);

        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("v1")
                        .description("""
                                Production URL shortener with click analytics, QR codes and
                                Redis-backed short-code resolution.

                                Authentication is a stateless JWT bearer token. Register or log
                                in, then authorise with the returned token.
                                """)
                        .contact(new Contact().name("API Support"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current environment")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme));
        // Note: no global addSecurityItem here on purpose. Applying the requirement
        // per controller keeps the genuinely public endpoints — login, register and
        // the redirect — documented as public rather than falsely marked as
        // requiring a token.
    }
}
