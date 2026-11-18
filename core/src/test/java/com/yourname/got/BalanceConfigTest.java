package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;

/** balance.properties: optional knobs; defaults keep today's behaviour, bad values are ignored. */
class BalanceConfigTest {
    private static final File FILE = new File("balance.properties");

    private static void write(String text) throws Exception {
        Files.writeString(FILE.toPath(), text);
        BalanceConfig.reload();
    }

    private static void remove() {
        FILE.delete();
        BalanceConfig.reload();
    }

    @Test
    void knobsChangeTheGameAndDefaultsDoNot() throws Exception {
        TestWorld.reset(true);
        remove();
        try {
            int cost = GameBalance.getMineUpgradeCost("WOOD", 3, 1, "WOOD");
            int time = GameBalance.getBuildTimeMinutes("WOOD", 3);
            int production = GameBalance.getProductionPerMinute("WOOD", 5);
            Check.that(cost > 0 && time > 0 && production > 0, "defaults give normal numbers (" + cost + ", " + time + ", " + production + ")");

            write("cost.multiplier=2.0\nbuildtime.multiplier=0.5\nproduction.multiplier=3.0\n");
            int cost2 = GameBalance.getMineUpgradeCost("WOOD", 3, 1, "WOOD");
            Check.that(Math.abs(cost2 - 2 * cost) <= cost / 10, "cost.multiplier=2 doubles costs (" + cost + " -> " + cost2 + ")");
            Check.that(GameBalance.getBuildTimeMinutes("WOOD", 3) < time, "buildtime.multiplier=0.5 shortens building");
            int tripled = GameBalance.getProductionPerMinute("WOOD", 5);
            Check.that(Math.abs(tripled - 3 * production) <= 1, "production.multiplier=3 triples production, rounding aside (" + production + " -> " + tripled + ")");

            write("cost.multiplier=abc\nbuildtime.multiplier=-4\nproduction.multiplier=0\nloot.percent=500\n");
            Check.that(GameBalance.getMineUpgradeCost("WOOD", 3, 1, "WOOD") == cost, "an unparsable multiplier is ignored");
            Check.that(GameBalance.getBuildTimeMinutes("WOOD", 3) == time, "a negative multiplier is ignored");
            Check.that(GameBalance.getProductionPerMinute("WOOD", 5) == production, "a zero multiplier is ignored");
            Check.that(BalanceConfig.integer("loot.percent", 50, 0, 100) == 50, "an out-of-range integer falls back to the default");

            remove();
            Check.that(GameBalance.getMineUpgradeCost("WOOD", 3, 1, "WOOD") == cost, "deleting the file restores the defaults");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void mercenaryRoomAndLootFollowTheFile() throws Exception {
        TestWorld.reset(true);
        remove();
        try {
            GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
            GameOfThrones g = new GameOfThrones("knobs", 1, u1);
            g.addPlayer(u2);
            g.startGame();
            Kingdom k = u1.getKingdom();
            Field lv = com.yourname.got.logic.model.Building.Building.class.getDeclaredField("level");
            lv.setAccessible(true);
            lv.set(k.getCastle(), 5);
            Check.that(k.getMercenaryCapacity() == 100, "default: 20 per castle level = 100");
            write("mercenary.slots.per.castle.level=30\n");
            Check.that(k.getMercenaryCapacity() == 150, "30 per level = 150");
        } finally {
            remove();
        }
        Check.assertAll();
    }
}
