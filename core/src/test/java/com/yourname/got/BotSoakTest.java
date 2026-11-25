package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.system.KingdomUpdater;
import com.yourname.got.logic.system.SeasonalEvents;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Runs many simulated days with every bot and event feature on: nothing may crash or go negative. */
class BotSoakTest {
    @Test
    void manyDaysWithEverythingEnabled() throws Exception {
        TestWorld.reset(true);
        File cfg = new File("balance.properties");
        Files.writeString(cfg.toPath(), String.join("\n",
            "bots.count=3", "bots.garrison=10", "bots.growth.perDay=3", "bots.garrison.max=60",
            "bots.build.enabled=1", "bots.train.perDay=4",
            "events.enabled=1", "events.chance.percent=100", "events.effect.percent=10", ""));
        BalanceConfig.reload();
        try {
            GameUser human = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("soak", 1, human);
            g.addPlayer(new GameUser("bob", "1234", 2));
            g.startGame();
            int bots = 0;
            for (int day = 1; day <= 150; day++) {
                for (GameUser u : g.getUsers()) {
                    if (u == null) continue;
                    if (u instanceof BotUser b) {
                        b.reinforce(day);
                        b.develop(day);
                        b.train(day);
                    }
                    KingdomUpdater.updateKingdom(u.getKingdom());
                }
                SeasonalEvents.onDayAdvanced(g, day);
            }
            for (GameUser u : g.getUsers()) {
                if (!(u instanceof BotUser b)) continue;
                bots++;
                for (String res : new String[]{"WOOD", "STONE", "FOOD", "COIN"}) {
                    Check.that(b.getKingdom().getTotalResources(res) >= 0, "no negative " + res + " for " + b.getUsername());
                }
                Check.that(b.getKingdom().getOwnedSoldierCount() >= 0, "soldier count is sane");
            }
            Check.that(bots == 3, "three bots were simulated (" + bots + ")");
            Check.assertAll();
        } finally {
            cfg.delete();
            BalanceConfig.reload();
        }
    }
}
