package com.example.url_shortner.exception;

/**
 * Raised when a requested custom alias is syntactically invalid or reserved.
 *
 * <p>Reserved aliases matter because redirects are served from the root path: an
 * alias of "api" or "swagger-ui" would shadow a real application route.
 * See {@link com.example.url_shortner.util.ShortCodeGenerator}. Maps to 400.
 */
public class InvalidAliasException extends RuntimeException {

    public InvalidAliasException(String message) {
        super(message);
    }
}
