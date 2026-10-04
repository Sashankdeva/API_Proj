package com.squadup.matching;

import com.squadup.entity.Squad;
import com.squadup.entity.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Communication matters: prefer squads that speak the player's language. */
@Component
@Order(2)
public class LanguageScorer implements MatchScorer {

    @Override
    public Score score(User player, Squad squad) {
        return squad.getLanguage().equalsIgnoreCase(player.getLanguage())
                ? new Score(25, "Speaks " + squad.getLanguage())
                : Score.NONE;
    }
}
