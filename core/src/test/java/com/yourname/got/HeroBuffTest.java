package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

/** The hero buff and kingdom maluses only last for the battle; survivors keep their normal attack power. */
class HeroBuffTest {
    @Test
    void battleBuffIsNotPermanent() throws Exception {
        // The battle has random parts; with no survivors there is nothing to measure, so try again (up to 25 times).
        for (int attempt = 0; attempt < 25; attempt++) {
            TestWorld.reset(true);
            GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
            GameOfThrones g = new GameOfThrones("combat", 1, u1);
            g.addPlayer(u2);
            g.startGame();
            Kingdom ka = u1.getKingdom();
            ka.getHero().unlock();
            SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
            for (int i = 0; i < 30; i++) troops.addSoldier(new Infantry());
            int before = troops.getSoldiers().getHead().data.getAttackPower();
            new Mission(u1, u2, new SoldierUnit[]{troops}, 0, AttackType.GROUND).startAttack();
            SoldierUnit back = ka.getArmyManager().getDefenceManager().findUnitInDefenders(SoldierType.INFANTRY);
            if (back == null || back.getCount() == 0) continue;
            int min = Integer.MAX_VALUE, max = 0;
            for (Soldier s : back.getSoldiers()) { min = Math.min(min, s.getAttackPower()); max = Math.max(max, s.getAttackPower()); }
            Check.that(min == before && max == before, "survivors have their original attack power " + before + " (min " + min + ", max " + max + ")");
            Check.assertAll();
            return;
        }
        Check.that(false, "no attempt left survivors to measure");
        Check.assertAll();
    }
}
