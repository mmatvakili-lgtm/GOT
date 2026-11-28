package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.system.GameUpdater;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/** Advance the game clock to day 102: the final wave runs and the game ends with a ranking by score. */
class LongNightTest {
    @Test
    void gameEndsAfterTheFinalWaveWithRanking() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("night2", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        for (int n = 0; n < 40; n++) u1.getKingdom().getArmyManager().recruit(new Infantry());
        g.getGameTime().setGameSpeed(1);
        g.getGameTime().setStartTime(System.currentTimeMillis() - 101L * 24 * 3600 * 1000);
        for (int tick = 1; tick <= 6 && !g.isFinished(); tick++) GameUpdater.updateGlobalState(g);
        // The dead now need time to march: the game must wait for them instead of ending at once.
        Check.that(!g.isFinished(), "the game waits for the final battles");
        com.yourname.got.logic.model.Mission[] pending = new com.yourname.got.logic.model.Mission[g.getMissions().size()];
        g.getMissions().fillArray(pending);
        for (com.yourname.got.logic.model.Mission m : pending) m.startAttack(); // the march is over
        g.checkMissions();
        for (int tick = 1; tick <= 6 && !g.isFinished(); tick++) GameUpdater.updateGlobalState(g);
        Check.that(g.isFinished(), "the game ended after the final battles");
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("users/user_1/Record_night2.ser"))) {
            GameRecord r = (GameRecord) in.readObject();
            int count = 0;
            boolean rank1 = false, rank2 = false;
            for (PlayerResult p : r.getPlayerResults()) {
                count++;
                if (p.getFinalRank() == 1) rank1 = true;
                if (p.getFinalRank() == 2) rank2 = true;
            }
            Check.that(count == 2 && rank1 && rank2, "both players are ranked 1 and 2");
        }
        Check.assertAll();
    }
}
