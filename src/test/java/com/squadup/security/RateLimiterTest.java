package com.squadup.security;

import com.squadup.exception.ApiException;
import com.squadup.security.RateLimiter.Rule;
import com.squadup.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final MockEnvironment env = new MockEnvironment().withProperty("squadup.rate-limit.chat-message", "3");

    @Test
    void rejectsTheCallAfterTheLimitIsUsedUp() {
        RateLimiter limiter = new RateLimiter(env, clock);
        for (int i = 0; i < 3; i++) {
            limiter.check(Rule.CHAT_MESSAGE, 1L);
        }

        assertThatThrownBy(() -> limiter.check(Rule.CHAT_MESSAGE, 1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus().value()).isEqualTo(429);
    }

    @Test
    void allowAnswersWithABooleanInsteadOfThrowing() {
        RateLimiter limiter = new RateLimiter(env, clock);

        assertThat(limiter.allow(Rule.CHAT_MESSAGE, 1L)).isTrue();
        assertThat(limiter.allow(Rule.CHAT_MESSAGE, 1L)).isTrue();
        assertThat(limiter.allow(Rule.CHAT_MESSAGE, 1L)).isTrue();
        assertThat(limiter.allow(Rule.CHAT_MESSAGE, 1L)).isFalse();
    }

    @Test
    void callersAndRulesAreCountedSeparately() {
        RateLimiter limiter = new RateLimiter(env, clock);
        for (int i = 0; i < 3; i++) {
            limiter.check(Rule.CHAT_MESSAGE, 1L);
        }

        assertThatCode(() -> limiter.check(Rule.CHAT_MESSAGE, 2L)).doesNotThrowAnyException();
        assertThatCode(() -> limiter.check(Rule.SQUAD_CREATE, 1L)).doesNotThrowAnyException();
    }

    @Test
    void theAllowanceComesBackWhenTheWindowEnds() {
        RateLimiter limiter = new RateLimiter(env, clock);
        for (int i = 0; i < 3; i++) {
            limiter.check(Rule.CHAT_MESSAGE, 1L);
        }
        clock.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertThatCode(() -> limiter.check(Rule.CHAT_MESSAGE, 1L)).doesNotThrowAnyException();
    }

    @Test
    void usesTheDefaultWhenNothingIsConfigured() {
        RateLimiter limiter = new RateLimiter(new MockEnvironment(), clock);
        for (int i = 0; i < 10; i++) {
            limiter.check(Rule.SQUAD_CREATE, 1L);
        }

        assertThatThrownBy(() -> limiter.check(Rule.SQUAD_CREATE, 1L)).isInstanceOf(ApiException.class);
    }

    @Test
    void refusesALimitThatIsNotPositive() {
        env.setProperty("squadup.rate-limit.register", "0");

        assertThatThrownBy(() -> new RateLimiter(env, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("squadup.rate-limit.register");
    }
}
