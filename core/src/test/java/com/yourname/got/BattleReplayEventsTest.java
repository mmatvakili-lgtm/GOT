package com.yourname.got;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.model.UserSubMap;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.EventBattleSetup;
import com.yourname.got.logic.util.EventBuildingDestroyed;
import com.yourname.got.logic.util.EventBuildingFocus;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** The visual replay needs structured events (which building, which tile): setup, focus, destroyed (+ loot). */
class BattleReplayEventsTest {
    @Test
    void assaultProducesMapEvents() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("replay", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        if (u2.getKingdom().getType() == KingdomType.IRON_ISLANDS) { GameUser t = u1; u1 = u2; u2 = t; } // no land gate there
        Kingdom def = u2.getKingdom(), att = u1.getKingdom();
        UserSubMap um = def.getUserMap();

        Storage st = new Storage("Wood", def); st.markAsBuilt(); def.addStorage(st); def.getBuildings().add(st); um.addBuildingInMap(st, 4, 5);
        def.addResources("Wood", 20000);
        Mine m1 = new Mine("WOOD", def); m1.markAsBuilt(); def.getBuildings().add(m1); um.addBuildingInMap(m1, 7, 4);
        att.getHero().unlock();
        SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 150; i++) troops.addSoldier(new Infantry());
        Mission m = new Mission(u1, u2, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
        m.startAttack();

        List<BattleEvent> structured = new ArrayList<>();
        for (BattleEvent e : m.getReplayLog()) {
            if (e instanceof EventBattleSetup || e instanceof EventBuildingFocus || e instanceof EventBuildingDestroyed) structured.add(e);
        }
        Check.that(!structured.isEmpty() && structured.get(0) instanceof EventBattleSetup, "the first structured event is the battlefield setup");
        EventBattleSetup setup = (EventBattleSetup) structured.get(0);
        boolean hasStorage = false, hasMine = false;
        for (EventBattleSetup.Piece p : setup.pieces) {
            if (p.x == 4 && p.y == 5 && p.name.contains("Storage")) hasStorage = true;
            if (p.x == 7 && p.y == 4 && p.name.contains("Mine")) hasMine = true;
        }
        Check.that(hasStorage && hasMine, "the setup lists both buildings at their local tiles (" + setup.pieces.size() + " pieces)");

        int focus = 0;
        EventBuildingDestroyed storageFell = null;
        for (BattleEvent e : structured) {
            if (e instanceof EventBuildingFocus) focus++;
            if (e instanceof EventBuildingDestroyed d && d.x == 4 && d.y == 5) storageFell = d;
        }
        Check.that(focus >= 2, "each target was announced (" + focus + " focus events)");
        Check.that(storageFell != null && "WOOD".equals(storageFell.lootType) && storageFell.lootAmount == 10000,
            "the storage's destruction carries its loot: " + (storageFell == null ? "missing" : storageFell.lootType + " " + storageFell.lootAmount));
        Check.assertAll();
    }
}
