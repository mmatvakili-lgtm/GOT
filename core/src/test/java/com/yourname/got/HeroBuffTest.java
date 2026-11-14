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
        Check.that(back != null && back.getCount() > 0, "there are survivors");
        int min = Integer.MAX_VALUE, max = 0;
        for (Soldier s : back.getSoldiers()) { min = Math.min(min, s.getAttackPower()); max = Math.max(max, s.getAttackPower()); }
        Check.that(min == before && max == before, "survivors have their original attack power " + before + " (min " + min + ", max " + max + ")");
        Check.assertAll();
    }
}
