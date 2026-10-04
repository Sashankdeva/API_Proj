package com.squadup.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * JSON body of every error. The web UI reads {@code error}; the other fields help when
 * testing with Postman or curl.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int status,
        String path,
        String requestId,
        Instant timestamp,
        Map<String, String> fieldErrors) {
}
