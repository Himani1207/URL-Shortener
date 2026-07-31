package com.example.url_shortner.exception;

/**
 * Raised when a QR image cannot be rendered.
 *
 * <p>Wraps ZXing's checked {@code WriterException} and {@code IOException} so the
 * service layer exposes a single unchecked, domain-meaningful failure that
 * {@link GlobalExceptionHandler} can map to a 500 response.
 */
public class QrCodeGenerationException extends RuntimeException {

    public QrCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
