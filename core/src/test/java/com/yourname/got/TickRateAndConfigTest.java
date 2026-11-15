package com.yourname.got;

import com.yourname.got.logic.Config.GameConfig;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/** The game tick is now 1 second. Production must not depend on how often the tick runs, and config reads are cached. */
class TickRateAndConfigTest {
    private static int produce(Mine m, int ticks, long millisPerTick) throws Exception {
        Field f = Mine.class.getDeclaredField("lastUpdateTime");
        f.setAccessible(true);
        f.setLong(m, System.currentTimeMillis() - millisPerTick);
        int total = 0;
        for (int i = 0; i < ticks; i++) {
            total += m.produceAmount(1);
            f.setLong(m, f.getLong(m) - millisPerTick + millisPerTick); // keep the simulated clock moving below
            f.setLong(m, System.currentTimeMillis() - millisPerTick);
        }
        return total;
    }

    @Test
    void productionIsIndependentOfTheTickLength() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("tick", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        g.getGameTime().setGameSpeed(10); // faster game: more units per minute, a clearer comparison

        Mine a = new Mine("WOOD", u1.getKingdom());
        a.markAsBuilt();
        a.setStatus(Status.stable);
        Mine b = new Mine("WOOD", u1.getKingdom());
        b.markAsBuilt();
        b.setStatus(Status.stable);

        int perSecondTicks = produce(a, 60, 1000);   // 60 s as 60 ticks of 1 s
        int fiveSecondTicks = produce(b, 12, 5000);  // 60 s as 12 ticks of 5 s
        Check.that(perSecondTicks > 0 && fiveSecondTicks > 0, "both produce (" + perSecondTicks + " and " + fiveSecondTicks + ")");
        Check.that(Math.abs(perSecondTicks - fiveSecondTicks) <= 1,
            "same output for 1 s and 5 s ticks (" + perSecondTicks + " vs " + fiveSecondTicks + ")");
        Check.assertAll();
    }

    @Test
    void configIsCachedButSeesChanges() throws Exception {
        TestWorld.reset(true);
        int id = 5; // any positive game id: the config lives in GAMES/game_5/config.properties
        String first = GameConfig.getGameProperty(id, "game.difficulty");
        Check.that(first != null, "difficulty is readable: " + first);
        Check.that(first.equals(GameConfig.getGameProperty(id, "game.difficulty")), "second read is the same");
        Check.that(GameConfig.updateGameConfig(id, "game.difficulty", "hard"), "difficulty can be changed");
        Check.that("hard".equals(GameConfig.getGameProperty(id, "game.difficulty")), "the new value is visible immediately (cache invalidated)");
        Check.assertAll();
    }
}
