package com.squadup.dto;

/** Optional filters for the squad search; null / blank values are ignored. */
public record SquadSearchCriteria(
        String text,
        String game,
        String language,
        boolean openOnly,
        Long excludeMemberId) {
}
