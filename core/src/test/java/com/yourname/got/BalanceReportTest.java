package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.Archer;
import com.yourname.got.logic.model.Army.Cavalry;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.Soldier;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/**
 * Balance report: pits armies of every type and size against the same garrison and prints a table
 * (stars and survivors). The test also guards the basics: more soldiers never do worse, and no army ends up
 * with more soldiers than it sent. Run it with "gradlew :core:test --tests BalanceReportTest -i" to see the table.
 */
class BalanceReportTest {
    private static final int GARRISON = 60;
    private static final int[] SIZES = {10, 50, 100, 200, 400};

    private static Soldier make(SoldierType type) {
        return switch (type) {
            case ARCHER -> new Archer();
            case CAVALRY -> new Cavalry();
            default -> new Infantry();
        };
    }

    /** One battle in a fresh world against a bot with the given garrison; returns {stars, survivors}. */
    private static int[] fight(SoldierType type, int size) throws Exception {
        TestWorld.reset(true);
        GameUser attacker = new GameUser("alice", "1234", 1);
        GameOfThrones game = new GameOfThrones("balance", 1, attacker);
        game.startGame();
        BotUser defender = null;
        for (GameUser u : game.getUsers()) if (u instanceof BotUser b) defender = b;
        defender.equipGarrison(GARRISON);

        SoldierUnit troops = new SoldierUnit(type);
        for (int i = 0; i < size; i++) troops.addSoldier(make(type));
        Mission mission = new Mission(attacker, defender, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
        mission.startAttack();
        return new int[]{mission.calculateStars(), troops.getCount()};
    }

    @Test
    void biggerArmiesNeverDoWorseForEveryUnitType() throws Exception {
        File cfg = new File("balance.properties");
        Files.writeString(cfg.toPath(), "bots.count=1\nbots.garrison=0\n");
        BalanceConfig.reload();
        try {
            StringBuilder table = new StringBuilder("\nBALANCE REPORT (garrison " + GARRISON + ")\ntype      size  stars  survivors\n");
            for (SoldierType type : new SoldierType[]{SoldierType.INFANTRY, SoldierType.ARCHER, SoldierType.CAVALRY}) {
                int lastStars = -1;
                for (int size : SIZES) {
                    int[] r = fight(type, size);
                    table.append(String.format("%-9s %4d  %5d  %9d%n", type, size, r[0], r[1]));
                    Check.that(r[0] >= 0 && r[0] <= 3, type + " " + size + ": stars between 0 and 3");
                    Check.that(r[1] >= 0 && r[1] <= size, type + " " + size + ": survivors never exceed the army sent");
                    Check.that(r[0] >= lastStars, type + " " + size + ": not fewer stars than the smaller army (" + r[0] + " < " + lastStars + ")");
                    lastStars = r[0];
                }
            }
            System.out.println(table);
            Check.assertAll();
        } finally {
            cfg.delete();
            BalanceConfig.reload();
        }
    }
}
