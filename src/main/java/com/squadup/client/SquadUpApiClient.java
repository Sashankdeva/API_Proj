package com.squadup.client;

import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.ApiResponses.SquadCreated;
import com.squadup.dto.ApiResponses.SquadList;
import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.PlatformStats;
import com.squadup.dto.SquadDtos.CreateSquadRequest;
import com.squadup.dto.UserDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Creating a REST client: a typed Java client for the SquadUp REST API built on
 * {@link RestTemplate}. Other Java programs (or services) can talk to SquadUp with it
 * without building HTTP requests by hand. See {@code SquadUpApiClientTest} for a full example.
 *
 * <pre>{@code
 * SquadUpApiClient client = new SquadUpApiClient(new RestTemplateBuilder(), "http://localhost:5500");
 * client.login("sniperking", "sniperking");
 * SquadList mine = client.mySquads();
 * }</pre>
 *
 * Non-2xx responses surface as {@code HttpClientErrorException} / {@code HttpServerErrorException}.
 */
public class SquadUpApiClient {

    private final RestTemplate rest;
    private String token;
    private UserDto currentUser;

    public SquadUpApiClient(RestTemplateBuilder builder, String baseUrl) {
        this.rest = builder
                .rootUri(baseUrl)
                // Adds the bearer token to every request once we are logged in.
                .additionalInterceptors((request, body, execution) -> {
                    if (token != null) {
                        request.getHeaders().setBearerAuth(token);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    public ResponseEntity<Ack> register(String username, String email, String password) {
        return rest.postForEntity("/api/v1/auth/register", new RegisterRequest(username, email, password), Ack.class);
    }

    public UserDto login(String username, String password) {
        LoginResponse response = rest.postForObject("/api/v1/auth/login", new LoginRequest(username, password),
                LoginResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty login response");
        }
        this.token = response.token();
        this.currentUser = response.user();
        return currentUser;
    }

    public void logout() {
        rest.postForObject("/api/v1/auth/logout", null, Ack.class);
        token = null;
        currentUser = null;
    }

    public UserDto currentUser() {
        return currentUser;
    }

    public SquadCreated createSquad(String name, String game, String language, int maxMembers) {
        return rest.postForObject("/api/v1/squads", new CreateSquadRequest(name, game, language, maxMembers),
                SquadCreated.class);
    }

    public SquadList mySquads() {
        return rest.getForObject("/api/v1/squads/user/{userId}", SquadList.class, requireLogin().id());
    }

    public SquadCreated joinSquad(long squadId) {
        return rest.postForObject("/api/v1/squads/{id}/join", null, SquadCreated.class, squadId);
    }

    public Ack leaveSquad(long squadId) {
        return rest.exchange("/api/v1/squads/{id}", HttpMethod.DELETE, HttpEntity.EMPTY, Ack.class, squadId).getBody();
    }

    public PlatformStats stats() {
        return rest.getForObject("/api/v1/stats", PlatformStats.class);
    }

    /** Raw access for endpoints without a typed method (e.g. v2 or matrix-variable URLs). */
    public ResponseEntity<String> get(String path, HttpHeaders headers) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private UserDto requireLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("Call login() first");
        }
        return currentUser;
    }
}
