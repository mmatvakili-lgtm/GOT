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
        Check.that(g.isFinished(), "the game ended");
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("users/user_1/Record_night2.ser"))) {
            GameRecord r = (GameRecord) in.readObject();
            PlayerResult[] rs = new PlayerResult[2];
            int i = 0;
            for (PlayerResult p : r.getPlayerResults()) rs[i++] = p;
            Check.that(rs[0].getUsername().equals("alice") && rs[0].getFinalRank() == 1, "the player with the bigger army is ranked first");
            Check.that(rs[0].getScore() > rs[1].getScore(), "scores differ (" + rs[0].getScore() + " > " + rs[1].getScore() + ")");
        }
        Check.assertAll();
    }
}
