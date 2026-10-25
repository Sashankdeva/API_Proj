package com.squadup.soap;

import com.squadup.dto.UserDto;
import com.squadup.dto.UserSummary;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.security.AuthUser;
import com.squadup.security.RateLimiter;
import com.squadup.service.UserService;
import com.squadup.soap.schema.GetPlayerRequest;
import com.squadup.soap.schema.GetPlayerResponse;
import com.squadup.soap.schema.SearchPlayersRequest;
import com.squadup.soap.schema.SearchPlayersResponse;
import com.squadup.soap.schema.SoapNamespace;
import com.squadup.soap.schema.SoapPlayer;
import com.squadup.soap.schema.SoapPlayerSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.Objects;

/**
 * SOAP-based web service. {@code @PayloadRoot} routes each incoming SOAP body to a method
 * by the root element's namespace and name; JAXB converts XML to and from Java objects.
 *
 * The endpoint reuses the same {@link UserService} as the REST API. Only the protocol
 * (SOAP/XML instead of REST/JSON) differs.
 */
@Endpoint
public class PlayerSoapEndpoint {

    private static final int DEFAULT_RESULTS = 10;
    private static final int MAX_RESULTS = 50;

    private final UserService userService;
    private final RateLimiter rateLimiter;

    public PlayerSoapEndpoint(UserService userService, RateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @PayloadRoot(namespace = SoapNamespace.URI, localPart = "getPlayerRequest")
    @ResponsePayload
    public GetPlayerResponse getPlayer(@RequestPayload GetPlayerRequest request) {
        throttle();
        try {
            UserDto user = userService.getByUsername(Objects.requireNonNullElse(request.getUsername(), "").trim());
            GetPlayerResponse response = new GetPlayerResponse();
            response.setPlayer(toSoap(user));
            return response;
        } catch (ResourceNotFoundException e) {
            throw new PlayerNotFoundException("No player named '" + request.getUsername() + "'");
        }
    }

    @PayloadRoot(namespace = SoapNamespace.URI, localPart = "searchPlayersRequest")
    @ResponsePayload
    public SearchPlayersResponse searchPlayers(@RequestPayload SearchPlayersRequest request) {
        throttle();
        int max = request.getMaxResults() == null ? DEFAULT_RESULTS
                : Math.max(1, Math.min(request.getMaxResults(), MAX_RESULTS));
        Page<UserSummary> page;
        try {
            page = userService.search(request.getQuery(), PageRequest.of(0, max, Sort.by("username")));
        } catch (ApiException e) {
            throw new InvalidSoapRequestException(e.getMessage());
        }

        SearchPlayersResponse response = new SearchPlayersResponse();
        response.setTotalMatches(page.getTotalElements());
        page.forEach(u -> {
            SoapPlayerSummary s = new SoapPlayerSummary();
            s.setId(u.id());
            s.setUsername(u.username());
            s.setDisplayName(u.displayName());
            response.getPlayer().add(s);
        });
        return response;
    }

    /** Each logged-in caller may make a limited number of SOAP calls per minute; the rest get a SOAP fault. */
    private void throttle() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object caller = auth != null && auth.getPrincipal() instanceof AuthUser user ? user.id() : "anonymous";
        try {
            rateLimiter.check(RateLimiter.Rule.SOAP_CALL, caller);
        } catch (ApiException e) {
            throw new InvalidSoapRequestException(e.getMessage());
        }
    }

    private static SoapPlayer toSoap(UserDto u) {
        SoapPlayer p = new SoapPlayer();
        p.setId(u.id());
        p.setUsername(u.username());
        p.setDisplayName(nz(u.displayName()));
        p.setBio(nz(u.bio()));
        p.setRegion(nz(u.region()));
        p.setLanguage(nz(u.language()));
        p.setFavoriteGame(nz(u.favoriteGame()));
        p.setSkillLevel(nz(u.skillLevel()));
        p.setPlayStyle(nz(u.playStyle()));
        p.getGameTag().addAll(u.gameTags());
        p.getPlatform().addAll(u.platforms());
        p.setMemberSince(u.createdAt() == null ? "" : u.createdAt().toString());
        return p;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
