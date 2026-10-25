package com.squadup.security;

import com.squadup.exception.ApiException;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops one caller from flooding the application with sign-ups, friend requests, new squads
 * or chat messages. Each {@link Rule} allows a number of calls per key (a player id or an IP
 * address) in a fixed time window; the call after that is answered with 429.
 *
 * <p>Limits can be changed in {@code application.properties} as
 * {@code squadup.rate-limit.<rule-key>=<calls per window>}. State is kept in memory, which is
 * enough for a single instance.
 */
@Component
public class RateLimiter {

    public enum Rule {
        REGISTER("register", 20, Duration.ofHours(1)),
        FRIEND_REQUEST("friend-request", 30, Duration.ofHours(1)),
        SQUAD_CREATE("squad-create", 10, Duration.ofHours(1)),
        CHAT_MESSAGE("chat-message", 30, Duration.ofMinutes(1)),
        TYPING("typing", 30, Duration.ofSeconds(10)),
        SOAP_CALL("soap-call", 60, Duration.ofMinutes(1)),
        LOGIN_ATTEMPT("login-attempt", 100, Duration.ofMinutes(5));

        private final String key;
        private final int defaultLimit;
        private final Duration window;

        Rule(String key, int defaultLimit, Duration window) {
            this.key = key;
            this.defaultLimit = defaultLimit;
            this.window = window;
        }
    }

    private record Window(int calls, Instant end) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<Rule, Integer> limits = new EnumMap<>(Rule.class);
    private final Clock clock;

    public RateLimiter(Environment environment, Clock clock) {
        this.clock = clock;
        for (Rule rule : Rule.values()) {
            int limit = environment.getProperty("squadup.rate-limit." + rule.key, Integer.class, rule.defaultLimit);
            Assert.isTrue(limit > 0, "squadup.rate-limit." + rule.key + " must be greater than 0");
            limits.put(rule, limit);
        }
    }

    /**
     * Counts one call of {@code rule} by {@code caller}.
     *
     * @throws ApiException 429 when the caller has used up the rule's calls for the current window
     */
    public void check(Rule rule, Object caller) {
        if (!allow(rule, caller)) {
            throw ApiException.tooManyRequests("Too many requests. Please slow down and try again later.");
        }
    }

    /** Counts one call like {@link #check} but answers with a boolean, for events that are simply dropped. */
    public boolean allow(Rule rule, Object caller) {
        Instant now = Instant.now(clock);
        Window window = windows.merge(rule.key + ":" + caller, new Window(1, now.plus(rule.window)),
                (current, fresh) -> now.isBefore(current.end()) ? new Window(current.calls() + 1, current.end()) : fresh);
        return window.calls() <= limits.get(rule);
    }

    /** Housekeeping: forget windows that have ended, so the map cannot grow without bound. */
    @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1H")
    void purgeExpired() {
        Instant now = Instant.now(clock);
        windows.values().removeIf(w -> !now.isBefore(w.end()));
    }
}
