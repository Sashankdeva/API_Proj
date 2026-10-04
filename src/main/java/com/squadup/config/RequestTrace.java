package com.squadup.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Bean scope example: a new {@code RequestTrace} is created for every HTTP request
 * (the default scope, singleton, would share one instance across the whole app).
 *
 * Singletons such as {@code GlobalExceptionHandler} receive a scoped proxy, which forwards
 * each call to the instance belonging to the current request.
 */
@Component
@RequestScope
public class RequestTrace {

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final long startNanos = System.nanoTime();

    public String getId() {
        return id;
    }

    public long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
