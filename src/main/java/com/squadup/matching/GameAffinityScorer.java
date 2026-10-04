package com.squadup.matching;

import com.squadup.entity.Squad;
import com.squadup.entity.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Strongest signal: the squad plays the player's favourite game or one of their game tags. */
@Component
@Order(1)
public class GameAffinityScorer implements MatchScorer {

    @Override
    public Score score(User player, Squad squad) {
        String game = squad.getGame();
        if (game.equalsIgnoreCase(player.getFavoriteGame())) {
            return new Score(50, "Plays your favorite game");
        }
        boolean tagged = player.getGameTags().stream().anyMatch(game::equalsIgnoreCase);
        return tagged ? new Score(30, "Plays one of your games") : Score.NONE;
    }
}
