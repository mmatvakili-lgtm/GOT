package com.yourname.got;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/** The map draws a progress bar from Building.getWorkProgress / getWorkRemainingSeconds. */
class BuildingProgressTest {
    private static void set(Object o, String field, long value) throws Exception {
        Field f = Building.class.getDeclaredField(field);
        f.setAccessible(true);
        f.setLong(o, value);
    }

    @Test
    void constructionAndUpgradeReportProgress() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("progress", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom k = u1.getKingdom();

        Mine idle = new Mine("WOOD", k);
        idle.markAsBuilt();
        Check.that(idle.getWorkProgress(1) == -1f, "a finished building has no progress bar");
        Check.that(idle.getWorkRemainingSeconds(1) == 0, "a finished building has 0 seconds left");

        Mine m = new Mine("WOOD", k);

        m.startConstruction(1);
        float p = m.getWorkProgress(1);
        Check.that(p >= 0f && p < 0.5f, "just started: progress is small (" + p + ")");
        Check.that(m.getWorkRemainingSeconds(1) > 0, "seconds are left (" + m.getWorkRemainingSeconds(1) + ")");

        set(m, "constructionStartTime", System.currentTimeMillis() - 1_000_000L);
        Check.that(m.getWorkProgress(1) == 1f, "long ago: clamped to 1.0");
        Check.that(m.getWorkRemainingSeconds(1) == 0, "nothing left to wait");

        m.completeConstruction();
        Check.that(m.getWorkProgress(1) == -1f, "finished: no progress bar any more");

        m.startUpgrade(1);
        float up = m.getWorkProgress(1);
        Check.that(up >= 0f && up < 0.5f, "upgrade shows progress too (" + up + ")");
        Check.assertAll();
    }
}
