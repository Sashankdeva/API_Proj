package com.squadup.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@code @Around} advice: wraps methods annotated with {@link TrackExecutionTime}, measures
 * how long they take and warns when one is slower than the configured threshold.
 */
@Aspect
@Component
@Order(1)
public class PerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);

    private final long slowThresholdMs;

    public PerformanceAspect(@Value("${squadup.aop.slow-threshold-ms:500}") long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    @Around("@annotation(com.squadup.aop.TrackExecutionTime)")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            String method = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();
            if (ms > slowThresholdMs) {
                log.warn("SLOW {} took {} ms (threshold {} ms)", method, ms, slowThresholdMs);
            } else {
                log.info("{} took {} ms", method, ms);
            }
        }
    }
}
