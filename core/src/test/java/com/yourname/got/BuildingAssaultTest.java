package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.util.*;

/** A conquest attack must destroy the buildings (it used to loop 100 times on the same one) and loot storages. */
class BuildingAssaultTest {
    @Test
    void attackersDestroyBuildingsAndLoot() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("assault", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        // Kingdoms are dealt at random. Iron Islands has no land gate, so it must not be the defender of a ground attack.
        if (u2.getKingdom().getType() == KingdomType.IRON_ISLANDS) { GameUser t = u1; u1 = u2; u2 = t; }
        Kingdom def = u2.getKingdom(), att = u1.getKingdom();
        UserSubMap um = def.getUserMap();
        Storage st = new Storage("Wood", def); st.markAsBuilt(); def.addStorage(st); def.getBuildings().add(st); um.addBuildingInMap(st, 4, 4);
        def.addResources("Wood", 20000);
        Mine m1 = new Mine("WOOD", def); m1.markAsBuilt(); def.getBuildings().add(m1); um.addBuildingInMap(m1, 7, 4);
        Mine m2 = new Mine("STONE", def); m2.markAsBuilt(); def.getBuildings().add(m2); um.addBuildingInMap(m2, 7, 7);
        att.getHero().unlock();
        SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 150; i++) troops.addSoldier(new Infantry());
        Mission m = new Mission(u1, u2, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
        m.startAttack();
        int advancing = 0, failed = 0;
        StringBuilder log = new StringBuilder();
        for (BattleEvent e : m.getReplayLog()) {
            if (e instanceof EventTextLog t) {
                log.append(" | ").append(t.message);
                if (t.message.startsWith("Advancing")) advancing++;
                if (t.message.startsWith("Could not destroy")) failed++;
            }
        }
        Check.that(st.isDestroyed() && m1.isDestroyed() && m2.isDestroyed(), "all three buildings are destroyed; attacker=" + att.getType() + " defender=" + def.getType() + " log:" + log);
        Check.that(failed == 0 && advancing <= 4, "no endless loop (advancing=" + advancing + ", failed=" + failed + ")");
        Check.that(def.getTotalResources("Wood") == 10000, "half of the stored wood was taken (" + def.getTotalResources("Wood") + " left)");
        Check.that(um.getBuildingAt(7, 4) == m1 && m1.getStatus() == Status.destroyed, "the ruin stays on its tile so it can be repaired");
        Check.assertAll();
    }
}
