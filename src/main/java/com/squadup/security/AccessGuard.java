package com.squadup.security;

import com.squadup.exception.ApiException;

/** Ownership checks for endpoints that take a user id in the path. */
public final class AccessGuard {

    private AccessGuard() {
    }

    /** Players may only read or change their own private data (friends, squads, profile edits). */
    public static void requireSelf(AuthUser me, Long userId) {
        if (me == null || !me.id().equals(userId)) {
            throw ApiException.forbidden("You can only access your own account data.");
        }
    }
}
