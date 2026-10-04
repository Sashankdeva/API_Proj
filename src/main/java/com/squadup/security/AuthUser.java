package com.squadup.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * The logged-in player, stored as the principal in the SecurityContext (REST) and on the
 * STOMP session (WebSocket). {@link #getName()} is the user id, which is what Spring uses
 * to route {@code /user/queue/...} messages to this player.
 */
public record AuthUser(Long id, String username) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
