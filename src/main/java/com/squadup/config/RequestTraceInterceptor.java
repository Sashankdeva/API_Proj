package com.squadup.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Logs every API call with its status and duration, and returns the request id to the client. */
@Component
public class RequestTraceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceInterceptor.class);

    private final RequestTrace trace;

    public RequestTraceInterceptor(RequestTrace trace) {
        this.trace = trace;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("X-Request-Id", trace.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("[{}] {} {} -> {} ({} ms)", trace.getId(), request.getMethod(), request.getRequestURI(),
                response.getStatus(), trace.elapsedMillis());
    }
}
