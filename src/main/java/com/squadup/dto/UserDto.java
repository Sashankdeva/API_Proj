package com.squadup.dto;

import com.squadup.entity.User;

import java.time.Instant;
import java.util.List;

/** Full profile of a player as returned by the REST API (never includes the password). */
public record UserDto(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        List<String> gameTags,
        String region,
        String language,
        String avatarUrl,
        String firstName,
        String lastName,
        Integer age,
        String gender,
        String location,
        String favoriteGame,
        String skillLevel,
        List<String> platforms,
        String playStyle,
        int gamesPlayed,
        int totalHours,
        Instant createdAt) {

    /** The profile other players may see: the same data without the e-mail address. */
    public UserDto withoutEmail() {
        return new UserDto(id, username, null, displayName, bio, gameTags, region, language, avatarUrl,
                firstName, lastName, age, gender, location, favoriteGame, skillLevel, platforms, playStyle,
                gamesPlayed, totalHours, createdAt);
    }

    public static UserDto from(User u) {
        return new UserDto(
                u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(), u.getBio(),
                List.copyOf(u.getGameTags()), u.getRegion(), u.getLanguage(), u.getAvatarUrl(),
                u.getFirstName(), u.getLastName(), u.getAge(), u.getGender(), u.getLocation(),
                u.getFavoriteGame(), u.getSkillLevel(), List.copyOf(u.getPlatforms()), u.getPlayStyle(),
                u.getGamesPlayed(), u.getTotalHours(), u.getCreatedAt());
    }
}
