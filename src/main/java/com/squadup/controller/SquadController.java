package com.squadup.controller;

import com.squadup.aop.TrackExecutionTime;
import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.ApiResponses.SquadCreated;
import com.squadup.dto.ApiResponses.SquadList;
import com.squadup.dto.PageResponse;
import com.squadup.dto.SquadDtos.CreateSquadRequest;
import com.squadup.dto.SquadDtos.SquadDto;
import com.squadup.dto.SquadDtos.SquadMatch;
import com.squadup.dto.SquadSearchCriteria;
import com.squadup.security.AccessGuard;
import com.squadup.security.AuthUser;
import com.squadup.service.SquadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

/** Version 1 of the squad API (the one the web UI uses). */
@RestController
@RequestMapping("/api/v1/squads")
public class SquadController {

    private static final Set<String> SORTABLE = Set.of("createdAt", "name", "game", "language", "maxMembers");

    private final SquadService squadService;

    public SquadController(SquadService squadService) {
        this.squadService = squadService;
    }

    /**
     * Search with optional filters, pagination and sorting, e.g.
     * {@code GET /api/v1/squads?q=night&game=Valorant&openOnly=true&page=0&size=5&sort=name,asc}.
     * Backed by the custom Criteria API repository method.
     */
    @GetMapping
    @TrackExecutionTime
    public PageResponse<SquadDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var criteria = new SquadSearchCriteria(q, game, language, openOnly, null);
        return PageResponse.of(squadService.search(criteria, safe(pageable), SquadDto::from), s -> s);
    }

    /**
     * Matrix variables: filters carried inside a path segment, e.g.
     * {@code GET /api/v1/squads/browse/filter;game=Valorant;language=English;openOnly=true}.
     */
    @GetMapping("/browse/{segment}")
    public PageResponse<SquadDto> browse(
            @PathVariable String segment,
            @MatrixVariable(pathVar = "segment", required = false) String game,
            @MatrixVariable(pathVar = "segment", required = false) String language,
            @MatrixVariable(pathVar = "segment", defaultValue = "false") boolean openOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        var criteria = new SquadSearchCriteria(null, game, language, openOnly, null);
        return PageResponse.of(squadService.search(criteria, PageRequest.of(page, size), SquadDto::from), s -> s);
    }

    /** Most popular squads, ordered by member count through a custom {@code @Query}. */
    @GetMapping("/popular")
    public PageResponse<SquadDto> popular(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return PageResponse.of(squadService.mostPopular(PageRequest.of(page, size), SquadDto::from), s -> s);
    }

    /** Squads suggested for the logged-in player by the matching engine. */
    @GetMapping("/recommended")
    @TrackExecutionTime
    public List<SquadMatch> recommended(@RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
                                        @AuthenticationPrincipal AuthUser me) {
        return squadService.recommend(me.id(), limit);
    }

    @GetMapping("/{squadId}")
    public SquadDto get(@PathVariable Long squadId) {
        return squadService.get(squadId, SquadDto::from);
    }

    /** Squads the player belongs to. */
    @GetMapping("/user/{userId}")
    public SquadList squadsOf(@PathVariable Long userId, @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, userId);
        return new SquadList(squadService.squadsOf(userId));
    }

    /** Squads the player can still join (home page). */
    @GetMapping("/public/{userId}")
    public SquadList discover(@PathVariable Long userId, @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, userId);
        return new SquadList(squadService.notJoinedBy(userId));
    }

    @PostMapping
    public ResponseEntity<SquadCreated> create(@Valid @RequestBody CreateSquadRequest request,
                                               @AuthenticationPrincipal AuthUser me) {
        SquadDto squad = squadService.create(me.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(squad.id()).toUri();
        return ResponseEntity.created(location).body(new SquadCreated(true, squad));
    }

    @PostMapping("/{squadId}/join")
    public SquadCreated join(@PathVariable Long squadId, @AuthenticationPrincipal AuthUser me) {
        return new SquadCreated(true, squadService.join(squadId, me.id()));
    }

    /** The owner deletes the squad; anyone else leaves it. */
    @DeleteMapping("/{squadId}")
    public Ack leaveOrDelete(@PathVariable Long squadId, @AuthenticationPrincipal AuthUser me) {
        return Ack.of(squadService.leaveOrDelete(squadId, me.id()));
    }

    static Pageable safe(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> SORTABLE.contains(order.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50), sort);
    }
}
