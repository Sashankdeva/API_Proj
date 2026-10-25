package com.squadup.controller;

import com.squadup.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Squad creation and friend requests are limited per player (limits lowered to 1 for the test). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "squadup.rate-limit.squad-create=1",
        "squadup.rate-limit.friend-request=1"
})
class ActionRateLimitApiTest extends ApiTestSupport {

    @Test
    void aPlayerCannotCreateMoreSquadsThanTheLimit() throws Exception {
        Player player = newPlayer("squad_spammer");

        createSquad(player, "First squad").andExpect(status().isCreated());
        createSquad(player, "Second squad").andExpect(status().isTooManyRequests());

        // the limit belongs to the player, not to everybody
        createSquad(newPlayer("squad_other"), "Other squad").andExpect(status().isCreated());
    }

    @Test
    void aPlayerCannotSendMoreFriendRequestsThanTheLimit() throws Exception {
        Player sender = newPlayer("friend_spammer");
        Player first = newPlayer("friend_target_a");
        Player second = newPlayer("friend_target_b");

        friendRequest(sender, first.username()).andExpect(status().isCreated());
        friendRequest(sender, second.username()).andExpect(status().isTooManyRequests());
    }

    private ResultActions createSquad(Player player, String name) throws Exception {
        return mvc.perform(post("/api/v1/squads").header("Authorization", player.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("name", name, "game", "Valorant", "language", "English", "maxMembers", 5))));
    }

    private ResultActions friendRequest(Player sender, String recipient) throws Exception {
        return mvc.perform(post("/api/v1/friends/request").header("Authorization", sender.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("recipientUsername", recipient))));
    }
}
