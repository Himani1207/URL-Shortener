package com.example.url_shortner.exception;

/**
 * Raised when an authenticated user targets a resource belonging to someone else.
 *
 * <p><b>Why this was needed:</b> the analytics endpoints previously looked a URL up
 * by short code with no ownership check at all, so any logged-in user could read
 * any other user's click history, referrer data and IP addresses simply by guessing
 * or observing a short code. Every per-URL operation now asserts ownership and
 * raises this exception on mismatch. Maps to 403 Forbidden.
 */
public class UnauthorizedResourceAccessException extends RuntimeException {

    public UnauthorizedResourceAccessException(String message) {
        super(message);
    }
}
