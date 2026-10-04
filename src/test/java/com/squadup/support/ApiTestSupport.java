package com.squadup.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for MockMvc API tests: unique players, register + login in one call. */
public abstract class ApiTestSupport {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** A player that exists and is logged in. */
    public record Player(long id, String username, String token) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    protected static String uniqueName(String prefix) {
        return prefix + "_" + COUNTER.incrementAndGet() + "_" + (System.nanoTime() % 10000);
    }

    protected Player newPlayer(String prefix) throws Exception {
        String username = uniqueName(prefix);
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", username, "email", username + "@test.com", "password", "secret123"))))
                .andExpect(status().isCreated());
        return login(username, "secret123");
    }

    protected Player login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = read(result);
        return new Player(body.at("/user/id").asLong(), username, body.get("token").asText());
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
