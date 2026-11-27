package com.yourname.got;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.system.Tutorial;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

/** The guide reads the real kingdom: a fresh kingdom has a next step, optional steps stay hidden while their feature is off. */
class TutorialTest {
    @Test
    void freshKingdomStartsAtTheBeginning() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("guide", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom k = u1.getKingdom();
        Check.that(Tutorial.steps().size() == 7, "seven basic steps while research and diplomacy are off");
        Check.that(Tutorial.next(k) != null, "there is a next step");
        Check.that(Tutorial.doneCount(k) < Tutorial.steps().size(), "not finished at the start");
        Check.that(!Tutorial.steps().get(2).isDone(k), "castle is not level 3 yet");
        Check.that(Tutorial.next(null) != null, "a missing kingdom does not crash");
        Check.assertAll();
    }
}
