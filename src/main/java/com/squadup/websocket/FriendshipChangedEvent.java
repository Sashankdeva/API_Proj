package com.squadup.websocket;

import java.util.Set;

/** Published when friend lists or pending requests of these players changed. */
public record FriendshipChangedEvent(Set<Long> userIds) {

    public FriendshipChangedEvent {
        userIds = Set.copyOf(userIds);
    }

    public static FriendshipChangedEvent of(Long... userIds) {
        return new FriendshipChangedEvent(Set.of(userIds));
    }
}
