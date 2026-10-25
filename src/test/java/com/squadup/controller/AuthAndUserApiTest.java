package com.squadup.controller;

import com.squadup.dto.AuthDtos;
import com.squadup.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit V: @RequestBody, ResponseEntity, validation, exception handling, security and CORS
 * for the authentication and profile endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndUserApiTest extends ApiTestSupport {

    @Test
    void registerReturns201WithLocationHeader() throws Exception {
        String name = uniqueName("newbie");
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", name, "email", name + "@test.com", "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/")))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void validationErrorsReturn400WithFieldDetails() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", "ab", "email", "not-an-email", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.username").value("Username must be at least 3 characters."))
                .andExpect(jsonPath("$.fieldErrors.email").value("Please enter a valid email address."))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be at least 6 characters."));
    }

    @Test
    void usernamesCannotContainMarkupOrQuotes() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", "x');alert(1);//", "email", "evil@test.com", "password", "secret123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").value(AuthDtos.USERNAME_MESSAGE));
    }

    @Test
    void avatarMustBeAnHttpUrl() throws Exception {
        Player p = newPlayer("avatar");
        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("avatarUrl", "javascript:alert(1)"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Avatar must be an http(s) image URL."));
        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("avatarUrl", "https://example.com/me.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").value("https://example.com/me.png"));
        // an empty value resets to the default avatar
        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("avatarUrl", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").value(""));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request body."));
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        Player p = newPlayer("dupe");
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", p.username(), "email", "other@test.com", "password", "secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username is already taken."));
    }

    @Test
    void loginErrorsUseProperStatusCodes() throws Exception {
        Player p = newPlayer("login");
        // an unknown account and a wrong password look exactly the same, so logins cannot be enumerated
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", "ghost_user_x", "password", "whatever"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", p.username(), "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Incorrect username or password."));
        // login by e-mail works too
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", p.username().toUpperCase() + "@TEST.COM", "password", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void apiRequiresAToken() throws Exception {
        mvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please log in to continue."));
        mvc.perform(get("/api/v1/users/1").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        Player p = newPlayer("bye");
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", p.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/" + p.id()).header("Authorization", p.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileCanBeReadAndUpdatedByItsOwner() throws Exception {
        Player p = newPlayer("profile");

        mvc.perform(get("/api/v1/users/" + p.id()).header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(p.username()))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "bio", "Rusher main",
                                "gameTags", List.of("Valorant", "CS2"),
                                "age", "21",
                                "platforms", List.of("Steam")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value("Rusher main"))
                .andExpect(jsonPath("$.user.age").value(21))
                .andExpect(jsonPath("$.user.gameTags", hasItem("CS2")));

        // an empty age clears it
        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("age", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.age").value(nullValue()));

        mvc.perform(put("/api/v1/users/" + p.id()).header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("age", "500"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Age must be between 1 and 120."));
    }

    @Test
    void playersCannotEditSomeoneElse() throws Exception {
        Player me = newPlayer("me");
        Player other = newPlayer("other");
        mvc.perform(put("/api/v1/users/" + other.id()).header("Authorization", me.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("bio", "hacked"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/" + other.id()).header("Authorization", me.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void usernameChangeRejectsTakenNames() throws Exception {
        Player a = newPlayer("rename_a");
        Player b = newPlayer("rename_b");
        mvc.perform(put("/api/v1/users/" + a.id()).header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("newUsername", b.username()))))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordChangeAndAccountDeletion() throws Exception {
        Player p = newPlayer("pw");
        mvc.perform(put("/api/v1/users/" + p.id() + "/password").header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("currentPassword", "wrong", "newPassword", "newsecret1"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/users/" + p.id() + "/password").header("Authorization", p.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("currentPassword", "secret123", "newPassword", "newsecret1"))))
                .andExpect(status().isOk());
        Player again = login(p.username(), "newsecret1");

        mvc.perform(delete("/api/v1/users/" + p.id()).header("Authorization", again.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", p.username(), "password", "newsecret1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherPlayersDoNotSeeAnEmailAddress() throws Exception {
        Player me = newPlayer("private_me");
        Player other = newPlayer("private_other");

        mvc.perform(get("/api/v1/users/" + me.id()).header("Authorization", me.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(me.username() + "@test.com"));
        mvc.perform(get("/api/v1/users/" + me.id()).header("Authorization", other.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(me.username()))
                .andExpect(jsonPath("$.user.email").value(nullValue()));
    }

    @Test
    void playersOfAGameAreListedWithoutEmailAndWithinTheLimit() throws Exception {
        Player me = newPlayer("lister");
        setFavoriteGame(me, "Roster Quest");
        setFavoriteGame(newPlayer("lister_extra"), "Roster Quest");
        setFavoriteGame(newPlayer("lister_extra"), "Roster Quest");

        mvc.perform(get("/api/v1/users/players?game=Roster Quest&limit=2").header("Authorization", me.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username", notNullValue()))
                .andExpect(jsonPath("$[0].email").value(nullValue()))
                .andExpect(jsonPath("$[1].email").value(nullValue()));
        mvc.perform(get("/api/v1/users/players?game=Roster Quest&limit=101").header("Authorization", me.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedLoginInputIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", "x".repeat(200), "password", "secret123"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", "someone", "password", "p".repeat(5000)))))
                .andExpect(status().isBadRequest());
    }

    private void setFavoriteGame(Player player, String game) throws Exception {
        mvc.perform(put("/api/v1/users/" + player.id()).header("Authorization", player.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("favoriteGame", game))))
                .andExpect(status().isOk());
    }

    @Test
    void usernamesAreUniqueRegardlessOfLetterCase() throws Exception {
        Player player = newPlayer("casey");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", player.username().toUpperCase(),
                                "email", "someone.else@test.com", "password", "secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username is already taken."));
    }

    @Test
    void unknownPathsAreDenied() throws Exception {
        Player player = newPlayer("nosy");

        mvc.perform(get("/some/hidden/file.json")).andExpect(status().isUnauthorized());
        mvc.perform(get("/some/hidden/file.json").header("Authorization", player.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordChangeSignsOutOtherSessionsButKeepsTheCurrentOne() throws Exception {
        Player phone = newPlayer("sessions");
        Player laptop = login(phone.username(), "secret123");

        mvc.perform(put("/api/v1/users/" + phone.id() + "/password").header("Authorization", laptop.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("currentPassword", "secret123", "newPassword", "newsecret1"))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/users/" + phone.id()).header("Authorization", phone.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/" + phone.id()).header("Authorization", laptop.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void repeatedFailedLoginsAreThrottledPerLogin() throws Exception {
        Player victim = newPlayer("throttle");
        Player bystander = newPlayer("bystander");

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(Map.of("username", victim.username(), "password", "guess-" + i))))
                    .andExpect(status().isUnauthorized());
        }
        // locked out, even with the right password; the answer says when to retry
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", victim.username().toUpperCase(), "password", "secret123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error", containsString("Try again in")));
        // other logins are unaffected
        login(bystander.username(), "secret123");
        // and a lockout belongs to the address that caused it: from elsewhere the victim can still log in
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr("10.20.30.40");
                            return request;
                        })
                        .content(toJson(Map.of("username", victim.username(), "password", "secret123"))))
                .andExpect(status().isOk());
    }

    @Test
    void searchAndDirectoryArePaginated() throws Exception {
        Player p = newPlayer("finder");
        mvc.perform(get("/api/v1/users/search").param("username", "finder").param("size", "5")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username", containsString("finder")))
                .andExpect(jsonPath("$.size").value(5));
        mvc.perform(get("/api/v1/users/search").param("username", "x").header("Authorization", p.bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/search").param("username", "finder").param("size", "500")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users").param("sort", "username,asc").param("size", "2")
                        .header("Authorization", p.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.sort").value("username: ASC"));
    }

    @Test
    void corsPreflightIsAllowedForTheApi() throws Exception {
        mvc.perform(options("/api/v1/squads")
                        .header("Origin", "http://127.0.0.1:5501")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5501"));
    }

    @Test
    void responsesCarryARequestId() throws Exception {
        Player p = newPlayer("trace");
        mvc.perform(get("/api/v1/users/" + p.id()).header("Authorization", p.bearer()))
                .andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/api/v1/users/999999").header("Authorization", p.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.error").value("User not found."));
    }
}
