package com.example.url_shortner.exception;

/**
 * Raised when the password supplied to unlock a link does not match.
 *
 * <p>The message is deliberately identical whether the link is missing, paused or
 * simply has a different password — an unauthenticated caller must not be able to
 * probe which short codes exist by reading the difference.
 */
public class InvalidLinkPasswordException extends RuntimeException {

    public InvalidLinkPasswordException(String message) {
        super(message);
    }
}
