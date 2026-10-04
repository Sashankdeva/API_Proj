package com.squadup.jdbc;

import com.squadup.dto.PlatformStats;
import com.squadup.dto.PlatformStats.GameStat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring JDBC data access. Aggregate reporting queries are a natural fit for plain SQL:
 * {@link JdbcTemplate} handles connections, statements and exception translation, and a
 * {@link RowMapper} turns each row into an object. Compare this with the JPA repositories.
 */
@Repository
public class PlatformStatsDao {

    private static final RowMapper<GameStat> GAME_STAT_MAPPER = (rs, rowNum) -> new GameStat(
            rs.getString("game"),
            rs.getLong("squad_count"),
            rs.getLong("player_count"));

    private final JdbcTemplate jdbc;

    public PlatformStatsDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long countUsers() {
        return count("select count(*) from users");
    }

    public long countSquads() {
        return count("select count(*) from squads");
    }

    public long countMessages() {
        return count("select count(*) from messages");
    }

    public long countOpenSquads() {
        return count("""
                select count(*) from squads s
                where s.max_members > (select count(*) from squad_members m where m.squad_id = s.id)""");
    }

    /** Games ranked by how many players are in their squads, then by number of squads. */
    public List<GameStat> topGames(int limit) {
        String sql = """
                select s.game as game,
                       count(distinct s.id) as squad_count,
                       count(m.user_id) as player_count
                from squads s
                left join squad_members m on m.squad_id = s.id
                group by s.game
                order by player_count desc, squad_count desc, s.game
                limit ?""";
        return jdbc.query(sql, GAME_STAT_MAPPER, limit);
    }

    public PlatformStats snapshot(int topGames) {
        return new PlatformStats(countUsers(), countSquads(), countOpenSquads(), countMessages(), topGames(topGames));
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
