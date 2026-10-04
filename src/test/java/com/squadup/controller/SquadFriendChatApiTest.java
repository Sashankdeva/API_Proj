package com.squadup.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.squadup.support.ApiTestSupport;
import com.squadup.util.RoomIds;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit V: CRUD on squads, @PathVariable / @RequestParam / @MatrixVariable, versioning,
 * plus the friends and chat-history endpoints used by the web UI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SquadFriendChatApiTest extends ApiTestSupport {

    private long createSquad(Player owner, String name, String game, String language, int maxMembers) throws Exception {
        var result = mvc.perform(post("/api/v1/squads").header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", name, "game", game, "language", language, "maxMembers", maxMembers))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/squads/")))
                .andExpect(jsonPath("$.squad.createdBy.id").value(owner.id()))
                .andReturn();
        return read(result).at("/squad/id").asLong();
    }

    @Test
    void squadLifecycle() throws Exception {
        Player owner = newPlayer("owner");
        Player joiner = newPlayer("joiner");
        Player late = newPlayer("late");
        long squadId = createSquad(owner, "Duo Kings", "Valorant", "English", 2);

        // Shows up on the joiner's home page, not on the owner's
        mvc.perform(get("/api/v1/squads/public/" + joiner.id()).header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.squads[*].id", hasItem((int) squadId)));
        mvc.perform(get("/api/v1/squads/public/" + owner.id()).header("Authorization", owner.bearer()))
                .andExpect(jsonPath("$.squads[*].id", not(hasItem((int) squadId))));

        mvc.perform(post("/api/v1/squads/" + squadId + "/join").header("Authorization", joiner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.squad.members", hasSize(2)));
        mvc.perform(post("/api/v1/squads/" + squadId + "/join").header("Authorization", joiner.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Already a member."));
        mvc.perform(post("/api/v1/squads/" + squadId + "/join").header("Authorization", late.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Squad is full."));

        mvc.perform(get("/api/v1/squads/user/" + joiner.id()).header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.squads[0].name").value("Duo Kings"));

        mvc.perform(delete("/api/v1/squads/" + squadId).header("Authorization", joiner.bearer()))
                .andExpect(jsonPath("$.message").value("Left squad."));
        mvc.perform(delete("/api/v1/squads/" + squadId).header("Authorization", owner.bearer()))
                .andExpect(jsonPath("$.message").value("Squad deleted."));
        mvc.perform(get("/api/v1/squads/" + squadId).header("Authorization", owner.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void squadValidation() throws Exception {
        Player p = newPlayer("validator");
        mvc.perform(post("/api/v1/squads").header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "", "game", "CS2", "maxMembers", 50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Squad name and game are required."))
                .andExpect(jsonPath("$.fieldErrors.maxMembers").value("A squad can have at most 20 members."));
        mvc.perform(get("/api/v1/squads/abc").header("Authorization", p.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for 'squadId'."));
    }

    @Test
    void searchPaginationSortingAndMatrixVariables() throws Exception {
        Player p = newPlayer("searcher");
        createSquad(p, "Apex Alpha", "Apex Legends", "German", 3);
        createSquad(p, "Apex Beta", "Apex Legends", "English", 3);
        createSquad(p, "Mine Crafters", "Minecraft", "German", 3);

        mvc.perform(get("/api/v1/squads").param("game", "Apex Legends").param("sort", "name,desc").param("size", "1")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Apex Beta"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/v1/squads/browse/filter;game=Apex Legends;language=German")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Apex Alpha"));

        mvc.perform(get("/api/v1/squads/popular").header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    void versioningByUriAndHeader() throws Exception {
        Player p = newPlayer("versioned");
        long id = createSquad(p, "Versioned", "PUBG", "Hindi", 4);

        mvc.perform(get("/api/v2/squads/" + id).header("Authorization", p.bearer()))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.openSlots").value(3))
                .andExpect(jsonPath("$.owner").value(p.username()))
                .andExpect(jsonPath("$.members").doesNotExist());

        mvc.perform(get("/api/squads").param("game", "PUBG").header("X-API-Version", "2")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].openSlots").value(3));

        mvc.perform(get("/api/v1/squads/" + id).header("Authorization", p.bearer()))
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void recommendationsFavorMatchingGameAndLanguage() throws Exception {
        Player owner = newPlayer("reco_owner");
        Player me = newPlayer("reco_me");
        mvc.perform(put("/api/v1/users/" + me.id())
                        .header("Authorization", me.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("favoriteGame", "Rocket League", "language", "French"))))
                .andExpect(status().isOk());
        createSquad(owner, "Rocket Fr", "Rocket League", "French", 5);
        createSquad(owner, "Rocket En", "Rocket League", "English", 5);

        var result = mvc.perform(get("/api/v1/squads/recommended").param("limit", "20").header("Authorization", me.bearer()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode first = read(result).get(0);
        assertThat(first.at("/squad/name").asText()).isEqualTo("Rocket Fr");
        assertThat(first.get("reasons").toString()).contains("Plays your favorite game", "Speaks French");
    }

    @Test
    void friendRequestFlow() throws Exception {
        Player alice = newPlayer("alice");
        Player bob = newPlayer("bob");

        mvc.perform(post("/api/v1/friends/request").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("recipientUsername", alice.username()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You can't add yourself."));

        mvc.perform(post("/api/v1/friends/request").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("recipientUsername", bob.username()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/v1/friends/request").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("recipientUsername", bob.username()))))
                .andExpect(status().isConflict());

        var pending = mvc.perform(get("/api/v1/friends/pending/" + bob.id()).header("Authorization", bob.bearer()))
                .andExpect(jsonPath("$.pending[0].username").value(alice.username()))
                .andReturn();
        long friendshipId = read(pending).at("/pending/0/friendshipId").asLong();

        // Only the recipient may answer, and nobody may read someone else's list
        mvc.perform(post("/api/v1/friends/respond").header("Authorization", alice.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("friendshipId", friendshipId, "action", "accept"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/friends/" + bob.id()).header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/friends/respond").header("Authorization", bob.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("friendshipId", friendshipId, "action", "accept"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Both sides get the same DM room id
        String room = RoomIds.dm(alice.id(), bob.id());
        mvc.perform(get("/api/v1/friends/" + alice.id()).header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.friends[0].username").value(bob.username()))
                .andExpect(jsonPath("$.friends[0].roomId").value(room));
        mvc.perform(get("/api/v1/friends/" + bob.id()).header("Authorization", bob.bearer()))
                .andExpect(jsonPath("$.friends[0].roomId").value(room));
        mvc.perform(get("/api/v1/chat/history/" + room).header("Authorization", bob.bearer()))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/friends/" + friendshipId).header("Authorization", bob.bearer()))
                .andExpect(jsonPath("$.message").value("Friend removed."));
        mvc.perform(get("/api/v1/friends/" + alice.id()).header("Authorization", alice.bearer()))
                .andExpect(jsonPath("$.friends", hasSize(0)));
    }

    @Test
    void chatHistoryIsOnlyVisibleToRoomMembers() throws Exception {
        Player owner = newPlayer("chat_owner");
        Player outsider = newPlayer("chat_outsider");
        long squadId = createSquad(owner, "Chatters", "Fortnite", "English", 5);

        mvc.perform(get("/api/v1/chat/history/squad_" + squadId).header("Authorization", owner.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(0)));
        mvc.perform(get("/api/v1/chat/history/squad_" + squadId).header("Authorization", outsider.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/chat/history/dm_1_2").header("Authorization", outsider.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void statsComeFromJdbcTemplate() throws Exception {
        Player p = newPlayer("stats");
        createSquad(p, "Stat Squad", "Overwatch 2", "English", 5);
        mvc.perform(get("/api/v1/stats").header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalSquads", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.topGames[*].game", everyItem(not(""))));
    }
}
