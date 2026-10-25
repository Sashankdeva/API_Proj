package com.squadup.controller;

import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.UserDto;
import com.squadup.exception.ApiException;
import com.squadup.security.LoginAttemptLimiter;
import com.squadup.security.RateLimiter;
import com.squadup.service.AuthService;
import com.squadup.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    private final RateLimiter rateLimiter;
    private final LoginAttemptLimiter loginLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter, LoginAttemptLimiter loginLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.loginLimiter = loginLimiter;
    }

    /** 201 Created, with a Location header pointing at the new user resource. */
    @PostMapping("/register")
    public ResponseEntity<Ack> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimiter.check(RateLimiter.Rule.REGISTER, http.getRemoteAddr());
        UserDto user = authService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(Ack.of("Account created! You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // Two limits, both per client address: all login attempts, and repeated failures for one login.
        // Counting by address means a stranger cannot lock a player out from somewhere else.
        String client = http.getRemoteAddr();
        rateLimiter.check(RateLimiter.Rule.LOGIN_ATTEMPT, client);
        String attemptKey = client + "|" + request.username().trim().toLowerCase(Locale.ROOT);
        loginLimiter.checkAllowed(attemptKey);
        try {
            LoginResponse response = authService.login(request);
            loginLimiter.recordSuccess(attemptKey);
            return ResponseEntity.ok(response);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.UNAUTHORIZED) {
                loginLimiter.recordFailure(attemptKey);
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Ack> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(TokenService.extractBearer(authorization));
        return ResponseEntity.status(HttpStatus.OK).body(Ack.of("Logged out."));
    }
}
