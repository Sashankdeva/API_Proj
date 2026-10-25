package com.squadup.service;

import com.squadup.aop.SkipLogging;
import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.UserDto;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
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
    /** Checked when the login does not exist, so unknown and known logins take equally long. */
    private final String unknownUserHash;

    /** Constructor injection: all dependencies are required, so they are final. */
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.unknownUserHash = passwordEncoder.encode("no-such-user");
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        if (users.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Username is already taken.");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already taken.");
        }

        User user = users.save(new User(username, email, passwordEncoder.encode(req.password())));
        log.info("Registered new player '{}'", user.getUsername());
        return UserDto.from(user);
    }

    /** Accepts either the username or the e-mail address, in any letter case. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        String login = req.username().trim();

        // One message for "no such account" and "wrong password", so logins cannot be enumerated.
        User user = users.findByLogin(login).orElse(null);
        String hash = user == null ? unknownUserHash : user.getPassword();
        if (!passwordEncoder.matches(req.password(), hash) || user == null) {
            throw ApiException.unauthorized("Incorrect username or password.");
        }

        String token = tokenService.issue(user);
        log.info("Player '{}' logged in", user.getUsername());
        return new LoginResponse(true, token, UserDto.from(user));
    }

    @SkipLogging   // the argument is a bearer token
    public void logout(String token) {
        if (token != null) {
            tokenService.revoke(token);
        }
    }
}
