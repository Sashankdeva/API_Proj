package com.squadup.controller;

import com.squadup.dto.PageResponse;
import com.squadup.dto.SquadDtos.SquadSummaryV2;
import com.squadup.dto.SquadSearchCriteria;
import com.squadup.service.SquadService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versioning a Spring REST endpoint. Version 2 returns a compact squad summary
 * (member count and open slots instead of the full member list). Clients choose it with:
 * <ul>
 *   <li>URI versioning:    {@code GET /api/v2/squads}</li>
 *   <li>header versioning: {@code GET /api/squads} with {@code X-API-Version: 2}</li>
 * </ul>
 * Version 1 stays unchanged in {@link SquadController}, so existing clients keep working.
 */
@RestController
public class SquadV2Controller {

    private final SquadService squadService;

    public SquadV2Controller(SquadService squadService) {
        this.squadService = squadService;
    }

    @GetMapping("/api/v2/squads")
    public PageResponse<SquadSummaryV2> listByUri(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var criteria = new SquadSearchCriteria(q, game, language, openOnly, null);
        return PageResponse.of(
                squadService.search(criteria, SquadController.safe(pageable), SquadSummaryV2::from), s -> s);
    }

    @GetMapping(value = "/api/squads", headers = "X-API-Version=2")
    public PageResponse<SquadSummaryV2> listByHeader(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return listByUri(q, game, language, openOnly, pageable);
    }

    @GetMapping("/api/v2/squads/{squadId}")
    public SquadSummaryV2 get(@PathVariable Long squadId) {
        return squadService.get(squadId, SquadSummaryV2::from);
    }
}
