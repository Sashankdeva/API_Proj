package com.squadup.security;

import com.squadup.exception.ApiException;
import com.squadup.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: the clock is a field we move by hand, so no sleeping is needed. */
class LoginAttemptLimiterTest {

    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, LOCKOUT, clock);

    @Test
    void locksAfterTheMaximumNumberOfFailures() {
        failTimes("alice", 2);
        assertThatCode(() -> limiter.checkAllowed("alice")).doesNotThrowAnyException();

        limiter.recordFailure("alice");

        assertThatThrownBy(() -> limiter.checkAllowed("alice"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Try again in")
                .extracting(e -> ((ApiException) e).getStatus().value()).isEqualTo(429);
    }

    @Test
    void lockEndsAfterTheLockoutPeriod() {
        failTimes("alice", 3);
        clock.advance(LOCKOUT.plusSeconds(1));

        assertThatCode(() -> limiter.checkAllowed("alice")).doesNotThrowAnyException();
        limiter.recordFailure("alice");   // a fresh count starts instead of locking again at once
        assertThatCode(() -> limiter.checkAllowed("alice")).doesNotThrowAnyException();
    }

    @Test
    void successForgetsEarlierFailures() {
        failTimes("alice", 2);
        limiter.recordSuccess("alice");
        failTimes("alice", 2);

        assertThatCode(() -> limiter.checkAllowed("alice")).doesNotThrowAnyException();
    }

    @Test
    void loginsAreCountedSeparately() {
        failTimes("alice", 3);

        assertThatCode(() -> limiter.checkAllowed("bob")).doesNotThrowAnyException();
    }

    @Test
    void failuresOutsideTheWindowDoNotAddUp() {
        failTimes("alice", 2);
        clock.advance(LOCKOUT.plusSeconds(1));
        failTimes("alice", 2);

        assertThatCode(() -> limiter.checkAllowed("alice")).doesNotThrowAnyException();
    }

    private void failTimes(String login, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(login);
        }
    }
}
