package com.squadup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Partial profile update: every field is optional and only non-null fields are applied.
 * {@code age} is a string so the form can clear it with "" (null means "leave unchanged").
 */
public record UpdateProfileRequest(
        @Size(max = 60, message = "Display name must be at most 60 characters.") String displayName,
        @Size(max = 300, message = "Bio must be at most 300 characters.") String bio,
        @Size(max = 10, message = "Max 10 game tags.") List<@Size(max = 30, message = "Game tags must be at most 30 characters.") String> gameTags,
        @Size(max = 30) String region,
        @Size(max = 30) String language,
        @Size(max = 500, message = "Avatar URL is too long.")
        @Pattern(regexp = "^(|https?://[^\\s\"'<>]+)$", message = "Avatar must be an http(s) image URL.")
        String avatarUrl,

        @Size(min = 3, max = 30, message = "Username must be 3-30 characters.")
        @Pattern(regexp = AuthDtos.USERNAME_PATTERN, message = AuthDtos.USERNAME_MESSAGE)
        String newUsername,

        @Size(max = 30, message = "First name must be at most 30 characters.") String firstName,
        @Size(max = 30, message = "Last name must be at most 30 characters.") String lastName,
        @Pattern(regexp = "^(|[1-9]\\d?|1[01]\\d|120)$", message = "Age must be between 1 and 120.") String age,
        @Size(max = 20) String gender,
        @Size(max = 60, message = "Location must be at most 60 characters.") String location,

        @Size(max = 60, message = "Favorite game must be at most 60 characters.") String favoriteGame,
        @Size(max = 30) String skillLevel,
        @Size(max = 10) List<@Size(max = 40) String> platforms,
        @Size(max = 30) String playStyle,
        @Min(value = 0, message = "Games played cannot be negative.") Integer gamesPlayed,
        @Min(value = 0, message = "Total hours cannot be negative.") Integer totalHours) {
}
