package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.util.MyLinkedList;
import java.io.*;

class CampCapacityTest {
    static void check(boolean c, String m) { Check.that(c, m); }

    static Camp addCamp(Kingdom k, boolean built) {
        Camp c = new Camp(k);
        if (built) c.markAsBuilt();
        k.getBuildings().add(c);
        return c;
    }
    static void recruit(Kingdom k, int n) {
        for (int i = 0; i < n; i++) k.getArmyManager().recruit(new Infantry());
    }

    @Test
    void campCapacityIsSharedByAllCamps() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1);
        GameUser u2 = new GameUser("bob", "1234", 2);
        GameUser u3 = new GameUser("carol", "1234", 3);
        GameOfThrones g = new GameOfThrones("test", 1, u1);
        g.addPlayer(u2); g.addPlayer(u3);
        g.startGame();
        System.out.println("=== CAPACITY ===");
        Kingdom k = u1.getKingdom();
        int baseCap = k.getTotalSoldierCapacity();
        Camp c1 = addCamp(k, true), c2 = addCamp(k, true);
        addCamp(k, false); // still under construction: must NOT count
        check(k.getTotalSoldierCapacity() - baseCap == 40, "2 built camps = 40 slots, unbuilt camp ignored (cap=" + k.getTotalSoldierCapacity() + ")");
        recruit(k, 25);
        check(c1.hasCapacity() && c2.hasCapacity(), "25 soldiers / 40 capacity: camps still have room (old code: both 'full')");
        check(k.getFreeSoldierSlots() == k.getTotalSoldierCapacity() - 25, "free slots = capacity - 25 (" + k.getFreeSoldierSlots() + ")");

        // deployed soldiers still occupy space
        SoldierUnit away = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 5; i++) away.addSoldier(new Infantry());
        Mission m = new Mission(u1, u2, new SoldierUnit[]{away}, 600, AttackType.GROUND);
        g.getMissions().add(m);
        check(k.getDeployedSoldierCount() == 5, "5 soldiers away on a mission are counted (" + k.getDeployedSoldierCount() + ")");
        check(k.getOwnedSoldierCount() == 30, "owned = 25 home + 5 away (" + k.getOwnedSoldierCount() + ")");
        g.getMissions().remove(m);

        // fill exactly to the limit through TrainingTask.finishTraining
        int free = k.getFreeSoldierSlots();
        int ok = 0;
        for (int i = 0; i < free; i++) {
            TrainingTask t = new TrainingTask(SoldierType.INFANTRY, k, System.currentTimeMillis(), 1);
            if (t.finishTraining().isSuccess()) ok++;
        }
        check(ok == free, "all " + free + " free slots can be filled by trained soldiers (" + ok + ")");
        check(k.getFreeSoldierSlots() == 0 && !c1.hasCapacity(), "army now exactly full");

        // full army: soldier is not created, resources are refunded
        int woodBefore = k.getTotalResources("Wood"), coinBefore = k.getTotalResources("Coin"), foodBefore = k.getTotalResources("Food");
        TrainingTask t = new TrainingTask(SoldierType.INFANTRY, k, System.currentTimeMillis(), 1);
        boolean r = t.finishTraining().isSuccess();
        check(!r, "training in a full army fails");
        check(k.getTotalResources("Wood") >= woodBefore && k.getTotalResources("Coin") >= coinBefore && k.getTotalResources("Food") >= foodBefore,
            "...and nothing is lost (refund path ran, resources not reduced)");
        Check.assertAll();
    }
}
