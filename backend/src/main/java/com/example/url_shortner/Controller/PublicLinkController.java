package com.example.url_shortner.Controller;

import com.example.url_shortner.dto.request.UnlockRequest;
import com.example.url_shortner.dto.response.ErrorResponse;
import com.example.url_shortner.dto.response.UnlockResponse;
import com.example.url_shortner.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one API surface a visitor can reach without an account.
 *
 * <p><b>Why a separate controller.</b> Everything under {@code /api/urls} is
 * owner-scoped and sits behind {@code anyRequest().authenticated()}. Unlocking a
 * protected link is the opposite: the caller is the recipient of a shared link and
 * has no token. Putting it on its own {@code /api/public} prefix means the security
 * rule that permits it is a single unambiguous path, rather than a wildcard under
 * {@code /api/urls} that would also expose {@code /api/urls/stats}.
 *
 * <p><b>No matching GET.</b> There is deliberately no "is this link protected?"
 * endpoint. The unlock page does not need one — it is only ever reached because
 * {@link RedirectController} sent the visitor there — and adding one would hand an
 * anonymous caller a way to test short codes for existence.
 */
@Slf4j
@RestController
@RequestMapping("/api/public/links")
@RequiredArgsConstructor
@Tag(name = "Public links", description = "Unauthenticated access to shared links")
public class PublicLinkController {

    private final UrlService urlService;

    @PostMapping("/{shortCode}/unlock")
    @Operation(
            summary = "Unlock a password-protected link",
            description = "Verifies the visitor password and returns the destination. "
                    + "A successful unlock records the click; a failed attempt does not.")
    @ApiResponse(responseCode = "200", description = "Password accepted")
    @ApiResponse(responseCode = "401", description = "Incorrect password, or the link is unavailable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UnlockResponse> unlock(
            @PathVariable String shortCode,
            @Valid @RequestBody UnlockRequest body,
            HttpServletRequest request) {

        String destination = urlService.unlock(shortCode, body.getPassword(), request);

        return ResponseEntity.ok()
                // The body carries a destination that was gated behind a password;
                // no shared cache should be allowed to keep a copy of it.
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(UnlockResponse.builder().originalUrl(destination).build());
    }
}
