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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign-ups from one address are limited; the limit is lowered here to keep the test short. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "squadup.rate-limit.register=3")
class RateLimitApiTest extends ApiTestSupport {

    @Test
    void theFourthSignUpFromOneAddressIsRefused() throws Exception {
        newPlayer("signup_a");
        newPlayer("signup_b");
        newPlayer("signup_c");

        String name = uniqueName("signup_d");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", name, "email", name + "@test.com", "password", "secret123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests. Please slow down and try again later."));
    }
}
