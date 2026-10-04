package com.squadup.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squadup.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

/** Writes 401 / 403 responses as the same JSON shape as every other API error. */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Please log in to continue.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "You are not allowed to do that.");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(message, status.value(), request.getRequestURI(), null,
                Instant.now(clock), null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
