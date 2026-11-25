package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.research.Effect;
import com.yourname.got.logic.research.Research;
import com.yourname.got.logic.research.ResearchState;
import com.yourname.got.logic.research.Tech;
import com.yourname.got.logic.util.Result;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;

/** The research tree: off by default, paid and queued, finished by time, effects follow the switch, safe to save. */
class ResearchTest {
    private static final File CFG = new File("balance.properties");

    private static Kingdom richKingdom() throws Exception {
        TestWorld.reset(true);
        GameUser u = new GameUser("alice", "1234", 1);
        GameOfThrones g = new GameOfThrones("research", 1, u);
        g.addPlayer(new GameUser("bob", "1234", 2));
        g.startGame();
        Kingdom k = u.getKingdom();
        for (String r : new String[]{"WOOD", "STONE", "COIN", "FOOD"}) {
            k.registerNewBuilding(new Storage(r, k));
            k.addResources(r, 5000);
        }
        return k;
    }

    @Test
    void offByDefaultChangesNothing() throws Exception {
        CFG.delete();
        BalanceConfig.reload();
        Kingdom k = richKingdom();
        Check.that(!Research.enabled(), "research is off by default");
        Check.that(!Research.enqueue(k, Tech.TOOLS).isSuccess(), "nothing can be queued while it is off");
        Check.that(Research.multiplier(k, Effect.PRODUCTION) == 1.0, "multipliers are exactly 1.0");
        Check.assertAll();
    }

    @Test
    void queueCostTimeEffectsAndCancel() throws Exception {
        Files.writeString(CFG.toPath(), "research.enabled=1\n");
        BalanceConfig.reload();
        try {
            Kingdom k = richKingdom();
            int wood = k.getTotalResources("WOOD");

            Check.that(!Research.check(k, Tech.GRANARIES).isSuccess(), "a technology with unmet requirements is refused");
            Result first = Research.enqueue(k, Tech.TOOLS);
            Check.that(first.isSuccess(), "Better Tools level 1 can be queued: " + first.getMessage());
            Check.that(k.getTotalResources("WOOD") == wood - 200, "the cost is paid when queued");
            Check.that(Research.enqueue(k, Tech.TOOLS).isSuccess(), "level 2 can wait in the queue behind level 1");
            Check.that(k.getResearch().queue().size() == 2, "two entries are queued");
            Check.that(Research.level(k, Tech.TOOLS) == 0, "nothing is learned before the time has passed");
            Check.that(Research.update(k).isEmpty(), "no completion immediately");

            // let the running research "finish": pretend it started long ago
            ResearchState.Task running = k.getResearch().queue().get(0);
            running.startMillis = System.currentTimeMillis() - running.requiredMillis - 1000;
            Check.that(Research.update(k).size() == 1, "the running research completes");
            Check.that(Research.level(k, Tech.TOOLS) == 1, "level 1 is learned");
            Check.that(Research.multiplier(k, Effect.PRODUCTION) == 1.05, "+5% mine production");
            Check.that(k.getResearch().queue().size() == 1 && k.getResearch().queue().get(0).started(),
                "the next level starts by itself");

            // the saved game must survive serialization with the research inside
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(k.getResearch()); }
            Check.that(bytes.size() > 0, "the research state can be saved");

            int before = k.getTotalResources("WOOD");
            Check.that(Research.cancel(k, 0).isSuccess(), "the running level can be cancelled");
            Check.that(k.getResearch().queue().isEmpty(), "the queue is empty after cancelling");
            Check.that(k.getTotalResources("WOOD") > before, "half of the cost came back");
            Check.that(Research.level(k, Tech.TOOLS) == 1, "the learned level stays");

            Files.writeString(CFG.toPath(), "research.enabled=0\n");
            BalanceConfig.reload();
            Check.that(Research.multiplier(k, Effect.PRODUCTION) == 1.0, "switching research off removes the bonuses again");
            Check.assertAll();
        } finally {
            CFG.delete();
            BalanceConfig.reload();
        }
    }

    @Test
    void everyTechnologyIsConsistent() {
        for (Tech t : Tech.values()) {
            Check.that(t.maxLevel >= 1 && t.perLevelPercent > 0, t + ": positive level count and effect");
            for (Tech.Requirement r : t.requirements()) {
                Check.that(r.tech != t, t + ": does not require itself");
                Check.that(r.level >= 1 && r.level <= r.tech.maxLevel, t + ": required level exists");
                Check.that(r.tech.tier < t.tier, t + ": prerequisites are on a lower tier (no cycles)");
            }
        }
        Check.assertAll();
    }
}
