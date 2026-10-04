package com.squadup.client;

import com.squadup.dto.ApiResponses.SquadCreated;
import com.squadup.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit V: consuming the running REST API from Java through {@link SquadUpApiClient}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SquadUpApiClientTest {

    @LocalServerPort
    private int port;

    private SquadUpApiClient client() {
        return new SquadUpApiClient(new RestTemplateBuilder(), "http://localhost:" + port);
    }

    @Test
    void registerLoginAndManageSquadsThroughTheClient() {
        SquadUpApiClient client = client();

        assertThat(client.register("client_user", "client_user@test.com", "secret123").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        UserDto me = client.login("client_user", "secret123");
        assertThat(me.username()).isEqualTo("client_user");

        SquadCreated created = client.createSquad("Client Crew", "CS2", "English", 4);
        assertThat(created.success()).isTrue();
        assertThat(client.mySquads().squads()).extracting(s -> s.name()).containsExactly("Client Crew");

        assertThat(client.stats().totalSquads()).isGreaterThanOrEqualTo(1);

        HttpHeaders v2 = new HttpHeaders();
        v2.set("X-API-Version", "2");
        assertThat(client.get("/api/squads?game=CS2", v2).getBody()).contains("\"openSlots\":3");

        assertThat(client.leaveSquad(created.squad().id()).message()).isEqualTo("Squad deleted.");
        client.logout();
    }

    @Test
    void errorsSurfaceAsHttpClientExceptions() {
        SquadUpApiClient client = client();
        assertThatThrownBy(() -> client.login("no_such_player", "whatever"))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("Account not found");
        assertThatThrownBy(client::stats)
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }
}
