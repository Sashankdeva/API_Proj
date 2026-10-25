package com.squadup.service;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Tokens live for one second here, so expiry can be observed for real. */
@SpringBootTest(properties = "squadup.auth.token-ttl=PT1S")
@ActiveProfiles("test")
class TokenExpiryTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Test
    void anExpiredTokenIsRejectedAndThePurgeJobRemovesIt() throws Exception {
        long userId = authService.register(new RegisterRequest("expiry_player", "expiry@test.com", "secret123")).id();
        String token = authService.login(new LoginRequest("expiry_player", "secret123")).token();
        assertThat(tokenService.authenticate(token)).isPresent();

        Thread.sleep(1500);

        assertThat(tokenService.authenticate(token)).isEmpty();
        assertThat(tokenRepository.findByUser_IdOrderByIdAsc(userId)).hasSize(1);
        tokenService.purgeExpired();
        assertThat(tokenRepository.findByUser_IdOrderByIdAsc(userId)).isEmpty();
    }
}
