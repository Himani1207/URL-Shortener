package com.example.url_shortner.exception;

public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String message) {
        super(message);
    }
}