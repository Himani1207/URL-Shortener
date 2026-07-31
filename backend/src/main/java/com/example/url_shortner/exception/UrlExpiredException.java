package com.example.url_shortner.exception;

public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String message) {
        super(message);
    }
}