package com.example.url_shortner.exception;

/**
 * Raised when a login attempt fails.
 *
 * <p>Deliberately thrown for both "no such user" and "wrong password", and carries
 * the same message in either case. Distinguishing them would turn the login
 * endpoint into a user-enumeration oracle, letting an attacker confirm which email
 * addresses hold accounts. Maps to 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
