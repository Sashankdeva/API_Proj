package com.squadup.matching;

import com.squadup.entity.Squad;
import com.squadup.entity.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Small bonus for squads with room to spare, so the player is not squeezed into the last slot. */
@Component
@Order(3)
public class OpenSlotsScorer implements MatchScorer {

    @Override
    public Score score(User player, Squad squad) {
        int open = squad.getMaxMembers() - squad.getMembers().size();
        if (open <= 0) {
            return Score.NONE;
        }
        return new Score(Math.min(open, 5) * 2, open + (open == 1 ? " slot" : " slots") + " open");
    }
}
