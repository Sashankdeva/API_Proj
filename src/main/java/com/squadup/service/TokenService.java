package com.squadup.service;

import com.squadup.entity.AuthToken;
import com.squadup.entity.User;
import com.squadup.repository.AuthTokenRepository;
import com.squadup.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/** Issues, validates and revokes the bearer tokens used by the web UI. */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String BEARER = "Bearer ";

    private final AuthTokenRepository tokens;
    private final SecureRandom random;
    private final Clock clock;
    private final Duration ttl;

    public TokenService(AuthTokenRepository tokens, SecureRandom random, Clock clock,
                        @Value("${squadup.auth.token-ttl:24h}") Duration ttl) {
        this.tokens = tokens;
        this.random = random;
        this.clock = clock;
        this.ttl = ttl;
    }

    public static String extractBearer(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        tokens.save(new AuthToken(token, user, Instant.now(clock).plus(ttl)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> authenticate(String token) {
        Instant now = Instant.now(clock);
        return tokens.findByToken(token)
                .filter(t -> !t.isExpired(now))
                .map(t -> new AuthUser(t.getUser().getId(), t.getUser().getUsername()));
    }

    @Transactional
    public void revoke(String token) {
        tokens.deleteByToken(token);
    }

    @Transactional
    public void revokeAll(Long userId) {
        tokens.deleteByUserId(userId);
    }

    /** Housekeeping: drop expired tokens once an hour. */
    @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1M")
    @Transactional
    public void purgeExpired() {
        int removed = tokens.deleteExpired(Instant.now(clock));
        if (removed > 0) {
            log.info("Purged {} expired auth tokens", removed);
        }
    }
}
