package com.squadup.service;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.entity.AuthToken;
import com.squadup.repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "squadup.auth.max-sessions=3")
@ActiveProfiles("test")
class TokenServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Test
    void onlyAHashOfTheTokenIsStored() {
        String token = registerAndLogin("hash_player");

        assertThat(tokenRepository.findAll()).extracting(AuthToken::getToken)
                .doesNotContain(token)
                .allSatisfy(stored -> assertThat(stored).hasSize(64).matches("[0-9a-f]+"));
        assertThat(tokenService.authenticate(token)).isPresent();
    }

    @Test
    void aPlayerKeepsOnlyTheNewestSessions() {
        registerAndLogin("session_player");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tokens.add(authService.login(new LoginRequest("session_player", "secret123")).token());
        }

        assertThat(tokenService.authenticate(tokens.get(0))).isEmpty();
        assertThat(tokenService.authenticate(tokens.get(1))).isEmpty();
        assertThat(tokenService.authenticate(tokens.get(2))).isPresent();
        assertThat(tokenService.authenticate(tokens.get(4))).isPresent();
    }

    private String registerAndLogin(String name) {
        authService.register(new RegisterRequest(name, name + "@test.com", "secret123"));
        return authService.login(new LoginRequest(name, "secret123")).token();
    }
}
