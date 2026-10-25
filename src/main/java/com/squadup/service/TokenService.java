package com.squadup.service;

import com.squadup.aop.SkipLogging;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Issues, validates and revokes the bearer tokens used by the web UI.
 *
 * <p>Only a SHA-256 hash of a token is stored, so a copy of the database cannot be used to log in.
 * The class is kept out of the debug trace ({@link SkipLogging}) because its arguments are tokens.
 */
@SkipLogging
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String BEARER = "Bearer ";

    private final AuthTokenRepository tokens;
    private final SecureRandom random;
    private final Clock clock;
    private final Duration ttl;
    private final int maxSessions;

    public TokenService(AuthTokenRepository tokens, SecureRandom random, Clock clock,
                        @Value("${squadup.auth.token-ttl:24h}") Duration ttl,
                        @Value("${squadup.auth.max-sessions:10}") int maxSessions) {
        this.tokens = tokens;
        this.random = random;
        this.clock = clock;
        this.ttl = ttl;
        this.maxSessions = maxSessions;
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
        tokens.save(new AuthToken(hash(token), user, Instant.now(clock).plus(ttl)));
        dropOldestSessions(user.getId());
        return token;
    }

    /** A player keeps at most {@code max-sessions} tokens; the oldest ones are removed first. */
    private void dropOldestSessions(Long userId) {
        List<AuthToken> sessions = tokens.findByUser_IdOrderByIdAsc(userId);
        int excess = sessions.size() - maxSessions;
        if (excess > 0) {
            tokens.deleteAll(sessions.subList(0, excess));
        }
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> authenticate(String token) {
        Instant now = Instant.now(clock);
        return tokens.findByToken(hash(token))
                .filter(t -> !t.isExpired(now))
                .map(t -> new AuthUser(t.getUser().getId(), t.getUser().getUsername()));
    }

    @Transactional
    public void revoke(String token) {
        tokens.deleteByToken(hash(token));
    }

    @Transactional
    public void revokeAll(Long userId) {
        tokens.deleteByUserId(userId);
    }

    /** Signs a player out everywhere except the session identified by {@code keepToken}. */
    @Transactional
    public void revokeAllExcept(Long userId, String keepToken) {
        tokens.deleteByUserIdExcept(userId, hash(keepToken));
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

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }
}
