package com.squadup.dto;

import java.util.List;

/** Platform-wide numbers, computed with plain SQL through JdbcTemplate. */
public record PlatformStats(
        long totalUsers,
        long totalSquads,
        long openSquads,
        long totalMessages,
        List<GameStat> topGames) {

    public record GameStat(String game, long squadCount, long playerCount) {
    }
}
