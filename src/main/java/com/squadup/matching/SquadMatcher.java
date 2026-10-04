package com.squadup.matching;

import com.squadup.dto.SquadDtos.SquadDto;
import com.squadup.dto.SquadDtos.SquadMatch;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks squads for one player.
 *
 * Bean scope example: this class keeps state (the player and the results collected so far),
 * so it must not be shared between requests. With {@code prototype} scope the container
 * creates a fresh instance each time one is requested (see {@code SquadService#recommend}).
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SquadMatcher {

    private final List<MatchScorer> scorers;
    private final List<SquadMatch> results = new ArrayList<>();
    private User player;

    /** Autowiring a collection: Spring injects every MatchScorer bean, in @Order order. */
    public SquadMatcher(List<MatchScorer> scorers) {
        this.scorers = scorers;
    }

    public SquadMatcher forPlayer(User player) {
        this.player = player;
        return this;
    }

    public SquadMatcher consider(Squad squad) {
        if (player == null) {
            throw new IllegalStateException("Call forPlayer() first");
        }
        int total = 0;
        List<String> reasons = new ArrayList<>();
        for (MatchScorer scorer : scorers) {
            MatchScorer.Score score = scorer.score(player, squad);
            if (score.points() > 0) {
                total += score.points();
                reasons.add(score.reason());
            }
        }
        if (total > 0) {
            results.add(new SquadMatch(SquadDto.from(squad), total, reasons));
        }
        return this;
    }

    public List<SquadMatch> top(int limit) {
        return results.stream()
                .sorted(Comparator.comparingInt(SquadMatch::score).reversed()
                        .thenComparing(m -> m.squad().createdAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    public List<MatchScorer> getScorers() {
        return scorers;
    }
}
