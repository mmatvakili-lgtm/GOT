package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.util.*;
import java.lang.reflect.Field;

class MercenaryQuartersTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    static Transaction pack(int n) {
        SoldierUnit u = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < n; i++) u.addSoldier(new Infantry());
        return new Transaction(null, 0, new MyLinkedList<ResourceItem>(), u, new MyLinkedList<ResourceItem>(), null, null);
    }
    @Test
    void hiredSoldiersDoNotBlockTraining() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice","1234",1), u2 = new GameUser("bob","1234",2);
        GameOfThrones g = new GameOfThrones("merc", 1, u1); g.addPlayer(u2); g.startGame();
        Kingdom k = u1.getKingdom();
        Field lv = Building.class.getDeclaredField("level"); lv.setAccessible(true); lv.set(k.getCastle(), 5);
        Camp c = new Camp(k); c.markAsBuilt(); k.getBuildings().add(c);                    // 20 camp slots
        Barrack b = new Barrack(k); b.markAsBuilt(); b.setStatus(Status.stable); k.getBuildings().add(b);
        for (int i = 0; i < 5; i++) k.getArmyManager().recruit(new Infantry());            // 5 trained soldiers
        System.out.println("camp capacity=" + k.getTotalSoldierCapacity() + " mercenary capacity=" + k.getMercenaryCapacity());
        check(k.getMercenaryCapacity() == 100, "castle level 5 gives 100 mercenary slots");

        Transaction t1 = pack(30);
        check(g.getBazar().buy(t1, u1).isSuccess(), "hire 30 mercenaries from the market");
        check(k.getMercenaryCount() == 30 && k.getTrainedSoldierCount() == 5, "30 hired + 5 trained (" + k.getMercenaryCount() + "/" + k.getTrainedSoldierCount() + ")");
        check(k.getOwnedSoldierCount() == 35, "army total is 35 although camps only hold 20");
        check(k.getFreeSoldierSlots() == 15, "camps still have 15 free slots for training (" + k.getFreeSoldierSlots() + ")");
        check(k.getFreeTrainingSlots() == 15 && c.hasCapacity(), "training is NOT blocked by the hired soldiers");

        Transaction t2 = pack(80);
        check(!g.getBazar().buy(t2, u1).isSuccess(), "hiring 80 more (total 110) is refused: quarters are full");
        check(k.getMercenaryCount() == 30, "...and nothing was added");
        Check.assertAll();
    }
}
