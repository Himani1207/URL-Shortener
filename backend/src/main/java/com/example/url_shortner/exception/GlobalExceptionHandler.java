package com.example.url_shortner.exception;

import com.example.url_shortner.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into the uniform {@link ErrorResponse} contract.
 *
 * <p><b>What changed:</b> this advice previously handled only
 * {@link UrlNotFoundException}. Everything else — a duplicate email, bad
 * credentials, a taken alias, a validation failure — escaped as an untyped
 * {@code RuntimeException} and reached the client as a bare HTTP 500 with a Spring
 * stack trace. Each failure mode now has an explicit handler and a correct status.
 *
 * <p><b>Logging policy:</b> client-caused failures (4xx) are logged at WARN with no
 * stack trace, because they are expected traffic and stack traces would drown the
 * logs. Only genuinely unexpected faults (5xx) log at ERROR with the full trace.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // 400 - Bad request
    // ------------------------------------------------------------------

    /** Bean-validation failures on {@code @Valid @RequestBody} arguments. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // merge keeps the first message when a field has several violations,
            // so the client shows one actionable message per field.
            fieldErrors.merge(fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    (first, second) -> first);
        }

        log.warn("Validation failed [path={}, fields={}]", request.getRequestURI(), fieldErrors.keySet());

        return build(HttpStatus.BAD_REQUEST, "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(InvalidAliasException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAlias(
            InvalidAliasException ex, HttpServletRequest request) {

        log.warn("Invalid alias [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            Exception ex, HttpServletRequest request) {

        log.warn("Malformed request [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request data", request, null);
    }

    // ------------------------------------------------------------------
    // 401 / 403 - Authentication and authorisation
    // ------------------------------------------------------------------

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {

        // Email is intentionally not logged here to keep credentials-adjacent data
        // out of the log stream.
        log.warn("Failed login attempt [path={}]", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /**
     * A wrong visitor password on a protected link.
     *
     * <p>401 rather than 403: the caller may retry with different credentials, which
     * is precisely what 401 means and 403 does not. The message is whatever the
     * service chose — uniform across "wrong password", "no such link" and "paused",
     * so this endpoint cannot be used to enumerate short codes.
     */
    @ExceptionHandler(InvalidLinkPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLinkPassword(
            InvalidLinkPasswordException ex, HttpServletRequest request) {

        log.warn("Failed link unlock attempt [path={}]", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /**
     * A protected link resolved without a password.
     *
     * <p>Reached only by API clients: the browser-facing redirect catches this
     * itself and sends the visitor to the unlock page instead.
     */
    @ExceptionHandler(UrlPasswordRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePasswordRequired(
            UrlPasswordRequiredException ex, HttpServletRequest request) {

        log.warn("Password required [path={}]", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler({AccessDeniedException.class, UnauthorizedResourceAccessException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            Exception ex, HttpServletRequest request) {

        log.warn("Access denied [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource", request, null);
    }

    // ------------------------------------------------------------------
    // 404 - Not found
    // ------------------------------------------------------------------

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFound(
            UrlNotFoundException ex, HttpServletRequest request) {

        log.warn("Short URL not found [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler({UserNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            Exception ex, HttpServletRequest request) {

        log.warn("User not found [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------
    // 409 - Conflict
    // ------------------------------------------------------------------

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAliasTaken(
            AliasAlreadyExistsException ex, HttpServletRequest request) {

        log.warn("Alias conflict [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailTaken(
            EmailAlreadyExistsException ex, HttpServletRequest request) {

        log.warn("Registration rejected, email already registered [path={}]", request.getRequestURI());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------
    // 410 - Gone
    // ------------------------------------------------------------------

    /**
     * 410 rather than 404: the link genuinely existed, it is simply no longer
     * served. Crawlers treat the two very differently.
     */
    @ExceptionHandler({UrlExpiredException.class, UrlInactiveException.class})
    public ResponseEntity<ErrorResponse> handleGone(
            RuntimeException ex, HttpServletRequest request) {

        log.warn("Link no longer available [path={}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.GONE, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------
    // 500 - Everything unanticipated
    // ------------------------------------------------------------------

    @ExceptionHandler(QrCodeGenerationException.class)
    public ResponseEntity<ErrorResponse> handleQrFailure(
            QrCodeGenerationException ex, HttpServletRequest request) {

        log.error("QR generation failed [path={}]", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate the QR code", request, null);
    }

    /**
     * Catch-all safety net.
     *
     * <p>The exception message is never echoed back to the caller: internal messages
     * routinely leak class names, SQL fragments and file paths. The full detail goes
     * to the log, the client gets a generic sentence.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception [path={}]", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request, null);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
