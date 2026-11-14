package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/** With a 5 second tick and game speed 1 a mine must still produce (the fraction used to be thrown away). */
class MineProductionTest {
    @Test
    void shortTicksStillProduce() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("mine", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Mine m = new Mine("WOOD", u1.getKingdom());
        m.markAsBuilt();
        m.setStatus(Status.stable);
        Field f = Mine.class.getDeclaredField("lastUpdateTime");
        f.setAccessible(true);
        int total = 0;
        for (int tick = 0; tick < 12; tick++) {          // 12 ticks x 5 s = 60 s
            f.setLong(m, System.currentTimeMillis() - 5000);
            total += m.produceAmount(1);
        }
        Check.that(total >= 3, "a mine produced " + total + " units in one minute (was 0 per tick before)");
        Check.assertAll();
    }

    @Test
    void upgradingAMineIncreasesProduction() {
        int l1 = com.yourname.got.logic.Balance.GameBalance.getProductionPerMinute("WOOD", 1);
        int l3 = com.yourname.got.logic.Balance.GameBalance.getProductionPerMinute("WOOD", 3);
        int l5 = com.yourname.got.logic.Balance.GameBalance.getProductionPerMinute("WOOD", 5);
        Check.that(l1 < l3 && l3 < l5, "production rises with the level: " + l1 + " < " + l3 + " < " + l5);
        Check.assertAll();
    }
}
