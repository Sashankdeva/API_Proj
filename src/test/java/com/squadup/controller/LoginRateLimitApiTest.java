package com.squadup.controller;

import com.squadup.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** All login attempts from one address are limited, whichever login they are for. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "squadup.rate-limit.login-attempt=3")
class LoginRateLimitApiTest extends ApiTestSupport {

    @Test
    void theFourthLoginAttemptFromOneAddressIsRefused() throws Exception {
        Player player = newPlayer("busy_login");   // attempt 1
        login(player.username(), "secret123");     // attempt 2
        login(player.username(), "secret123");     // attempt 3

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", player.username(), "password", "secret123"))))
                .andExpect(status().isTooManyRequests());
        // another address has its own allowance
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("10.1.2.3");
                            return request;
                        })
                        .content(toJson(Map.of("username", player.username(), "password", "secret123"))))
                .andExpect(status().isOk());
    }
}
