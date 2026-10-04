package com.squadup.controller;

import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.UserDto;
import com.squadup.service.AuthService;
import com.squadup.service.TokenService;
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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 201 Created, with a Location header pointing at the new user resource. */
    @PostMapping("/register")
    public ResponseEntity<Ack> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(Ack.of("Account created! You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Ack> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(TokenService.extractBearer(authorization));
        return ResponseEntity.status(HttpStatus.OK).body(Ack.of("Logged out."));
    }
}
