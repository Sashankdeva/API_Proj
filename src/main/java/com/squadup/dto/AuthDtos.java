package com.squadup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies for /api/v1/auth. Passwords are masked in toString() so they never reach the logs. */
public final class AuthDtos {

    /** Letters, digits, '.', '_' and '-' only, so a username is always safe to show anywhere in the UI. */
    public static final String USERNAME_PATTERN = "^[A-Za-z0-9._-]*$";
    public static final String USERNAME_MESSAGE = "Username can only contain letters, numbers, '.', '_' and '-'.";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "All fields are required.")
            @Size(min = 3, message = "Username must be at least 3 characters.")
            @Size(max = 30, message = "Username must be at most 30 characters.")
            @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE)
            String username,

            @NotBlank(message = "All fields are required.")
            @Email(message = "Please enter a valid email address.")
            @Size(max = 120, message = "Email is too long.")
            String email,

            @NotBlank(message = "All fields are required.")
            @Size(min = 6, message = "Password must be at least 6 characters.")
            @Size(max = 72, message = "Password must be at most 72 characters.")
            String password) {

        @Override
        public String toString() {
            return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
        }
    }

    /** {@code username} may also be the account e-mail. */
    public record LoginRequest(
            @NotBlank(message = "Username and password are required.")
            @Size(max = 120, message = "Username or password is too long.") String username,
            @NotBlank(message = "Username and password are required.")
            @Size(max = 72, message = "Username or password is too long.") String password) {

        @Override
        public String toString() {
            return "LoginRequest[username=" + username + ", password=***]";
        }
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Both current and new passwords are required.") String currentPassword,

            @NotBlank(message = "Both current and new passwords are required.")
            @Size(min = 6, message = "New password must be at least 6 characters.")
            @Size(max = 72, message = "New password must be at most 72 characters.")
            String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }

    public record LoginResponse(boolean success, String token, UserDto user) {
        @Override
        public String toString() {
            return "LoginResponse[success=" + success + ", token=***, user=" + (user == null ? null : user.username()) + "]";
        }
    }
}
