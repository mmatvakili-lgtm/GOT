package com.yourname.got;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Army.Archer;
import com.yourname.got.logic.model.Army.BattleShip;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Building.Barrack;
import com.yourname.got.logic.model.Building.Camp;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ship capacity, training several unit types at once, and launching an attack without losing soldiers. */
class ShipAndLaunchTest {
    private static void set(Object o, Class<?> c, String field, Object value) throws Exception {
        Field f = c.getDeclaredField(field);
        f.setAccessible(true);
        f.set(o, value);
    }

    @Test
    void upgradingAShipAddsAReasonableCapacity() {
        BattleShip ship = new BattleShip(false);
        int before = ship.soldiersCapacity;
        ship.upgrade();
        Check.that(ship.soldiersCapacity - before == 50, "one upgrade adds 50 places (" + before + " -> " + ship.soldiersCapacity + ")");
        Check.assertAll();
    }

    @Test
    void trainingSeveralTypesAndSafeLaunch() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("launch", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        set(u1.getKingdom(), Kingdom.class, "type", KingdomType.WINTERFELL);
        set(u2.getKingdom(), Kingdom.class, "type", KingdomType.HIGHGARDEN);
        Kingdom k = u1.getKingdom();
        for (String t : new String[]{"Wood", "Coin", "Food", "Stone"}) {
            Storage st = new Storage(t, k); st.markAsBuilt(); k.addStorage(st); k.getBuildings().add(st);
            k.addResources(t, 20000);
        }
        Camp camp = new Camp(k); camp.markAsBuilt(); k.getBuildings().add(camp);
        Barrack barrack = new Barrack(k); barrack.markAsBuilt(); barrack.setStatus(Status.stable); k.getBuildings().add(barrack);

        GameMediator med = new GameMediator(null);
        set(med, GameMediator.class, "currentUser", u1);
        set(med, GameMediator.class, "currentGame", g);

        // --- both requested types are queued (only the first one used to be)
        Map<SoldierType, Integer> request = new LinkedHashMap<>();
        request.put(SoldierType.INFANTRY, 2);
        request.put(SoldierType.ARCHER, 2);
        Result trained = med.trainOrManageShip(request, false);
        Check.that(trained.isSuccess(), "training request accepted: " + trained.getMessage());
        Check.that(k.getTrainingQueue().getSize() == 4, "all 4 soldiers of BOTH types are queued (" + k.getTrainingQueue().getSize() + ")");

        // --- launching with more soldiers than available must not take anything
        for (int i = 0; i < 5; i++) k.getArmyManager().recruit(new Infantry());
        for (int i = 0; i < 3; i++) k.getArmyManager().recruit(new Archer());
        g.getGameTime().setGameSpeed(1);
        g.getGameTime().setStartTime(System.currentTimeMillis() - 30L * 24 * 3600 * 1000); // summer
        g.getGameTime().updateTime();

        Map<SoldierType, Integer> troops = new LinkedHashMap<>();
        troops.put(SoldierType.INFANTRY, 5);
        troops.put(SoldierType.ARCHER, 99);
        MyLinkedList<SoldierType> formation = new MyLinkedList<>();
        formation.add(SoldierType.INFANTRY);
        formation.add(SoldierType.ARCHER);
        int before = k.getArmyManager().getTotalSoldier();
        Result r = med.launchAttack(u2, AttackType.GROUND, troops, formation);
        Check.that(!r.isSuccess() && r.getMessage().contains("Not enough ARCHER"), "refused with a reason: " + r.getMessage());
        Check.that(k.getArmyManager().getTotalSoldier() == before, "no soldier left the army (" + k.getArmyManager().getTotalSoldier() + " of " + before + ")");
        Check.that(k.getArmyManager().getDefenceManager().findUnitInDefenders(SoldierType.INFANTRY).getCount() == 5,
            "the 5 infantry are still at home (they used to be packaged first and lost)");
        Check.assertAll();
    }
}
