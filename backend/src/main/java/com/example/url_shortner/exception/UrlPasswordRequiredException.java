package com.example.url_shortner.exception;

/**
 * Raised when a redirect is attempted on a password-protected link without a
 * password.
 *
 * <p>Distinct from {@link InvalidLinkPasswordException}: this one says "you have not
 * been asked yet", and the redirect controller answers it by sending the visitor to
 * the unlock page rather than by returning an error.
 */
public class UrlPasswordRequiredException extends RuntimeException {

    public UrlPasswordRequiredException(String message) {
        super(message);
    }
}
