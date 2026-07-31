package com.example.url_shortner.exception;

/**
 * Raised when a redirect targets a link its owner has deactivated.
 *
 * <p>Distinct from {@link UrlExpiredException}: expiry is automatic and time-based,
 * deactivation is a deliberate act by the owner. Both map to 410 Gone, but keeping
 * them separate lets the client explain which happened. Replaces the bare
 * {@code RuntimeException("URL is inactive")} in the old redirect path.
 */
public class UrlInactiveException extends RuntimeException {

    public UrlInactiveException(String message) {
        super(message);
    }
}
