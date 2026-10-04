package com.squadup.exception;

import com.squadup.config.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handling for all REST controllers: every failure becomes an
 * {@link ErrorResponse} with a proper HTTP status instead of a stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final RequestTrace requestTrace;
    private final Clock clock;

    /** {@code requestTrace} is request-scoped: Spring injects a proxy that resolves per request. */
    public GlobalExceptionHandler(RequestTrace requestTrace, Clock clock) {
        this.requestTrace = requestTrace;
        this.clock = clock;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, null);
    }

    /** {@code @Valid @RequestBody} failures. The first message goes in "error" for the UI. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        String first = ex.getBindingResult().getAllErrors().isEmpty()
                ? "Validation failed."
                : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, first, req, fields);
    }

    /** Constraint annotations on @RequestParam / @PathVariable (e.g. {@code @Max(100) int size}). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        String message = ex.getAllErrors().isEmpty()
                ? "Validation failed."
                : ex.getAllErrors().get(0).getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        String message = ex.getConstraintViolations().stream()
                .findFirst().map(v -> v.getMessage()).orElse("Validation failed.");
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body.", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'.", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing parameter '" + ex.getParameterName() + "'.", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "That change conflicts with existing data.", req, null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLocking(PessimisticLockingFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "The resource is busy. Please try again.", req, null);
    }

    /**
     * Anything else. Spring MVC's own exceptions (405, 415, 404 for unknown paths ...) carry
     * their status via {@link org.springframework.web.ErrorResponse}; the rest are real 500s.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            String detail = springError.getBody().getDetail();
            return build(HttpStatus.valueOf(status.value()), detail != null ? detail : "Request failed.", req, null);
        }
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Server error. Please try again.", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req,
                                                Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(message, status.value(), req.getRequestURI(),
                requestTrace.getId(), Instant.now(clock), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
