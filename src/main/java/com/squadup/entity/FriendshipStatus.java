package com.squadup.entity;

public enum FriendshipStatus {
    PENDING, ACCEPTED, REJECTED;

    /** Lower-case form used in the JSON API ("pending", "accepted", "rejected"). */
    public String json() {
        return name().toLowerCase();
    }
}
