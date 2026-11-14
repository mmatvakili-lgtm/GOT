package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

/** Wounded soldiers recover after a battle (nothing healed them before). */
class HealingTest {
    @Test
    void survivorsAreHealedAfterBattle() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("heal", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom ka = u1.getKingdom(), kd = u2.getKingdom();
        for (int i = 0; i < 20; i++) kd.getArmyManager().recruit(new Infantry());
        SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 60; i++) troops.addSoldier(new Infantry());
        new Mission(u1, u2, new SoldierUnit[]{troops}, 0, AttackType.GROUND).startAttack();
        int full = SoldierType.INFANTRY.health, wounded = 0, n = 0;
        SoldierUnit back = ka.getArmyManager().getDefenceManager().findUnitInDefenders(SoldierType.INFANTRY);
        if (back != null) for (Soldier s : back.getSoldiers()) { n++; if (s.getHealth() < full) wounded++; }
        Check.that(n > 0, "there are survivors (" + n + ")");
        Check.that(wounded == 0, "no survivor is still wounded (" + wounded + ")");
        Check.assertAll();
    }
}
