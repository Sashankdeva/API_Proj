package com.squadup.service;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.UserDto;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /** Constructor injection: all three dependencies are required, so they are final. */
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        if (users.existsByUsername(username)) {
            throw ApiException.conflict("Username is already taken.");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already taken.");
        }

        User user = users.save(new User(username, email, passwordEncoder.encode(req.password())));
        log.info("Registered new player '{}'", user.getUsername());
        return UserDto.from(user);
    }

    /** Accepts either the username (exact case) or the e-mail address. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = users.findByLogin(req.username().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found. Please sign up first."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw ApiException.unauthorized("Incorrect password. Try again.");
        }

        String token = tokenService.issue(user);
        log.info("Player '{}' logged in", user.getUsername());
        return new LoginResponse(true, token, UserDto.from(user));
    }

    public void logout(String token) {
        if (token != null) {
            tokenService.revoke(token);
        }
    }
}
