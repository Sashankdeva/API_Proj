package com.squadup.security;

import com.squadup.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after {@code max-failed-logins} wrong passwords for the same
 * login within the lockout period, further attempts are rejected with 429 until it ends. The caller
 * builds the key (client address and login name), so one address cannot lock out another.
 *
 * <p>State is kept in memory, which is enough for a single-instance deployment. A successful
 * login clears the counter.
 */
@Component
public class LoginAttemptLimiter {

    private record Attempts(int failures, Instant windowEnd, boolean locked) {
    }

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;

    public LoginAttemptLimiter(@Value("${squadup.auth.max-failed-logins:5}") int maxFailures,
                               @Value("${squadup.auth.lockout:15m}") Duration lockout,
                               Clock clock) {
        Assert.isTrue(maxFailures > 0, "squadup.auth.max-failed-logins must be greater than 0");
        Assert.isTrue(!lockout.isNegative() && !lockout.isZero(), "squadup.auth.lockout must be positive");
        this.maxFailures = maxFailures;
        this.lockout = lockout;
        this.clock = clock;
    }

    /** @throws ApiException 429 while the login is locked out */
    public void checkAllowed(String login) {
        Attempts current = attempts.get(login);
        Instant now = Instant.now(clock);
        if (current != null && current.locked() && now.isBefore(current.windowEnd())) {
            long minutes = Math.max(1, Duration.between(now, current.windowEnd()).toMinutes() + 1);
            throw ApiException.tooManyRequests("Too many failed attempts. Try again in " + minutes + " minute(s).");
        }
    }

    public void recordFailure(String login) {
        Instant now = Instant.now(clock);
        attempts.compute(login, (key, current) -> {
            boolean fresh = current == null || !now.isBefore(current.windowEnd());
            int failures = fresh ? 1 : current.failures() + 1;
            Instant windowEnd = fresh ? now.plus(lockout) : current.windowEnd();
            if (failures >= maxFailures) {
                if (failures == maxFailures) {
                    audit.warn("LOGIN_LOCKED key={} for={}", key, lockout);
                }
                return new Attempts(failures, now.plus(lockout), true);
            }
            return new Attempts(failures, windowEnd, false);
        });
    }

    public void recordSuccess(String login) {
        attempts.remove(login);
    }

    /** Housekeeping: forget logins whose window has ended, so the map cannot grow without bound. */
    @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1H")
    void purgeExpired() {
        Instant now = Instant.now(clock);
        attempts.values().removeIf(a -> !now.isBefore(a.windowEnd()));
    }
}
