package com.squadup.dto;

import com.squadup.entity.User;

/** Public, lightweight view of a player (squad member lists, search results). */
public record UserSummary(Long id, String username, String displayName, String avatarUrl) {

    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getDisplayNameOrUsername(), u.getAvatarUrl());
    }
}
