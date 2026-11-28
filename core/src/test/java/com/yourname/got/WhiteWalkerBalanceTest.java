package com.yourname.got;

import com.yourname.got.logic.WhiteWalker.WhiteWalkerMission;
import com.yourname.got.logic.WhiteWalker.WhiteWalkerSoldier;
import com.yourname.got.logic.WhiteWalker.WhiteWalkerWave;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.Archer;
import com.yourname.got.logic.model.Army.Cavalry;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** The Long Night is a real threat: a prepared kingdom can win, an unprepared one cannot, and waves scale with the armies. */
class WhiteWalkerBalanceTest {

    private static long power(Kingdom k) {
        long p = 0;
        for (SoldierUnit u : k.getArmyManager().getDefenceManager().getDefenders()) if (u != null) p += u.getTotalAttackPower();
        return p;
    }

    /** One fresh kingdom with 150 soldiers of each type meets an army of {@code fraction} of its own power. Returns true if it holds. */
    private static boolean holds(double fraction, boolean finalWave, int glass) throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("longnight", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom k = u1.getKingdom();
        for (int i = 0; i < 150; i++) {
            k.getArmyManager().getDefenceManager().findOrCreateUnitInDefenders(SoldierType.INFANTRY).addSoldier(new Infantry());
            k.getArmyManager().getDefenceManager().findOrCreateUnitInDefenders(SoldierType.ARCHER).addSoldier(new Archer());
            k.getArmyManager().getDefenceManager().findOrCreateUnitInDefenders(SoldierType.CAVALRY).addSoldier(new Cavalry());
        }
        if (glass > 0) k.addDragonGlass(glass);
        long mine = power(k);
        SoldierUnit[] army = WhiteWalkerWave.build(Math.round(mine * fraction), finalWave, finalWave ? 1.0 : 0.5);
        boolean whiteWalkersWon = new WhiteWalkerMission(u1, army).soldierVsSoldierCombatAdvanced();
        return !whiteWalkersWon;
    }

    @Test
    void finalWaveNeedsPreparation() throws Exception {
        Check.that(holds(0.55, true, 0), "an easy final wave can be held without Dragonglass");
        Check.that(!holds(0.75, true, 0), "a medium final wave beats a kingdom with no Dragonglass");
        Check.that(holds(0.75, true, 1000), "a medium final wave can be held with Dragonglass for everyone");
        Check.that(!holds(1.00, true, 1000), "a hard final wave beats even a fully equipped kingdom of this size");
        Check.that(holds(0.25, false, 0), "an early ordinary wave is held");
        Check.assertAll();
    }

    @Test
    void waveCompositionAndScale() {
        SoldierUnit[] early = WhiteWalkerWave.build(10_000, false, 0.2);
        SoldierUnit[] last = WhiteWalkerWave.build(10_000, true, 1.0);
        Map<WhiteWalkerSoldier.Kind, Integer> ce = WhiteWalkerWave.census(early);
        Map<WhiteWalkerSoldier.Kind, Integer> cl = WhiteWalkerWave.census(last);
        Check.that(ce.get(WhiteWalkerSoldier.Kind.GIANT) == 0 && ce.get(WhiteWalkerSoldier.Kind.NIGHT_KING) == 0, "early waves are wights and walkers only");
        Check.that(cl.get(WhiteWalkerSoldier.Kind.NIGHT_KING) == 1, "the final wave brings exactly one Night King");
        Check.that(cl.get(WhiteWalkerSoldier.Kind.GIANT) > 0, "and Ice Giants");
        Check.that(cl.get(WhiteWalkerSoldier.Kind.WIGHT) > cl.get(WhiteWalkerSoldier.Kind.WALKER), "wights are the most numerous");
        long p = last[0].getTotalAttackPower();
        Check.that(p > 8_500 && p < 11_500, "the army's power is about the requested 10000 (was " + p + ")");
        Check.that(WhiteWalkerWave.describe(cl).contains("the Night King"), "the description names the Night King");
        Check.assertAll();
    }

    @Test
    void finalWaveIsReallyLaunchedAndResolved() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("finalwave", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        g.getWhiteWalkerManager().onDayAdvanced(g.getWhitewalkerFinalDay());
        int launched = 0;
        for (com.yourname.got.logic.model.Mission m : g.getMissions()) {
            if (m instanceof WhiteWalkerMission) {
                launched++;
                Check.that(m.getInitialDuration() > 0, "the dead need time to march");
            }
        }
        Check.that(launched >= 2, "attacks were really created for the living kingdoms (all waves up to the final one: " + launched + ")");
        Check.that(u1.isInFight() && u2.isInFight(), "the attacked kingdoms are marked as under attack");
        com.yourname.got.logic.model.Mission[] all = new com.yourname.got.logic.model.Mission[g.getMissions().size()];
        g.getMissions().fillArray(all);
        for (com.yourname.got.logic.model.Mission m : all) {
            if (m instanceof WhiteWalkerMission) Check.that(m.startAttack(), "the battle resolves");
        }
        for (com.yourname.got.logic.model.Mission m : g.getMissions()) {
            if (m instanceof WhiteWalkerMission) Check.that(m.isCompleted(), "every attack is completed");
        }
        Check.assertAll();
    }
}
