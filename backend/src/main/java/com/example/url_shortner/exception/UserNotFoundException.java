package com.example.url_shortner.exception;

/**
 * Raised when the authenticated principal has no matching row in the database.
 *
 * <p>In practice this means a still-valid JWT was presented for an account that has
 * since been deleted. Maps to 404 Not Found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
