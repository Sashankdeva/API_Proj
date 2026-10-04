package com.squadup.aop;

import com.squadup.exception.ApiException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Cross-cutting logging for the whole service layer, without a single log statement in the
 * services for it.
 *
 * <ul>
 *   <li>{@code @Before}         - method entry with its arguments</li>
 *   <li>{@code @AfterReturning} - successful exit with a short description of the result</li>
 *   <li>{@code @AfterThrowing}  - failures: business errors as WARN, unexpected ones as ERROR</li>
 * </ul>
 */
@Aspect
@Component
@Order(2)
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private static final int MAX_ARG_LENGTH = 120;

    /** Every public method of every class in com.squadup.service and its sub-packages. */
    @Pointcut("within(com.squadup.service..*) && execution(public * *(..))")
    public void serviceLayer() {
    }

    /** Scheduled housekeeping would flood the log, so it is left out. */
    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledJob() {
    }

    @Before("serviceLayer() && !scheduledJob()")
    public void logEntry(JoinPoint jp) {
        if (log.isDebugEnabled()) {
            log.debug("-> {}({})", name(jp), Arrays.stream(jp.getArgs())
                    .map(LoggingAspect::describe)
                    .collect(Collectors.joining(", ")));
        }
    }

    @AfterReturning(pointcut = "serviceLayer() && !scheduledJob()", returning = "result")
    public void logExit(JoinPoint jp, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("<- {} returned {}", name(jp), describe(result));
        }
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logFailure(JoinPoint jp, Throwable ex) {
        if (ex instanceof ApiException api) {
            log.warn("x  {} rejected: {} ({})", name(jp), api.getMessage(), api.getStatus().value());
        } else {
            log.error("x  {} failed: {}", name(jp), ex.toString());
        }
    }

    private static String name(JoinPoint jp) {
        return jp.getSignature().getDeclaringType().getSimpleName() + "." + jp.getSignature().getName();
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection<?> c) {
            return value.getClass().getSimpleName() + "[size=" + c.size() + "]";
        }
        String text = value.toString();
        return text.length() > MAX_ARG_LENGTH ? text.substring(0, MAX_ARG_LENGTH) + "..." : text;
    }
}
