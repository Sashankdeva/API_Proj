package com.squadup.matching;

import com.squadup.entity.Squad;
import com.squadup.entity.User;

/**
 * One rule of the squad recommendation engine. Every implementation annotated with
 * {@code @Component} is found by component scanning and autowired into
 * {@link SquadMatcher} as a {@code List<MatchScorer>}; adding a rule needs no other change.
 */
public interface MatchScorer {

    /** @return points (0 = no match) and a short human-readable reason */
    Score score(User player, Squad squad);

    record Score(int points, String reason) {
        public static final Score NONE = new Score(0, null);
    }
}
