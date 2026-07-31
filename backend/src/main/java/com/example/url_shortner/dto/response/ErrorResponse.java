package com.example.url_shortner.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error payload for every failed request.
 *
 * <p><b>Why this replaces the ad-hoc {@code Map<String, Object>}:</b> a hand-built
 * map has no schema, so springdoc cannot document it, the frontend cannot type it,
 * and nothing stops two handlers from disagreeing on key names. A DTO gives one
 * contract that the whole API — and the React client — can rely on.
 *
 * <p>{@code fieldErrors} is only populated for validation failures and is omitted
 * from the JSON entirely otherwise.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;

    /** HTTP status code, repeated in the body so logs and clients agree. */
    private int status;

    /** Reason phrase, e.g. "Not Found". */
    private String error;

    /** Human-readable, safe to surface directly in the UI. */
    private String message;

    /** Request path that produced the error. */
    private String path;

    /** Field name -> validation message. Present only on 400 validation failures. */
    private Map<String, String> fieldErrors;
}
