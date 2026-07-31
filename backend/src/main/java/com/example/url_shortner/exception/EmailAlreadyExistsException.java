package com.example.url_shortner.exception;

/**
 * Raised on registration when the email address is already taken.
 *
 * <p>Replaces the bare {@code RuntimeException} previously thrown by
 * {@code AuthServiceImpl}, which the global handler could not distinguish from an
 * internal fault and therefore reported as HTTP 500. Maps to 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
