package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Battle numbers can be tuned in balance.properties, bad values are ignored, and battles stay sane. */
class BattleBalanceTest {
    private static final File FILE = new File("balance.properties");

    private static void write(String text) throws Exception {
        Files.writeString(FILE.toPath(), text);
        BalanceConfig.reload();
    }

    private static void remove() {
        FILE.delete();
        BalanceConfig.reload();
    }

    @Test
    void battleKnobsDefaultToTheOriginalValuesAndIgnoreBadInput() throws Exception {
        TestWorld.reset(true);
        remove();
        try {
            Check.that(Mission.heroAttackBuff() == 1.1, "default hero buff is 1.1 (" + Mission.heroAttackBuff() + ")");
            Check.that(Mission.winterfellHealthPercentForDisplay() == 10, "default Winterfell malus is 10%");
            Check.that(Mission.eyrieAttackPercentForDisplay() == 15, "default Eyrie malus is 15%");

            write("battle.hero.attack.buff=1.5\nbattle.winterfell.health.percent=25\nbattle.eyrie.attack.percent=30\n");
            Check.that(Mission.heroAttackBuff() == 1.5, "hero buff can be tuned (" + Mission.heroAttackBuff() + ")");
            Check.that(Mission.winterfellHealthPercentForDisplay() == 25, "Winterfell malus can be tuned");
            Check.that(Mission.eyrieAttackPercentForDisplay() == 30, "Eyrie malus can be tuned");

            write("battle.hero.attack.buff=9\nbattle.winterfell.health.percent=200\nbattle.eyrie.attack.percent=-5\n");
            Check.that(Mission.heroAttackBuff() == 1.1, "an out-of-range hero buff is ignored");
            Check.that(Mission.winterfellHealthPercentForDisplay() == 10, "an out-of-range Winterfell malus is ignored");
            Check.that(Mission.eyrieAttackPercentForDisplay() == 15, "a negative Eyrie malus is ignored");

            write("battle.hero.attack.buff=abc\n");
            Check.that(Mission.heroAttackBuff() == 1.1, "an unparsable hero buff is ignored");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    /** One ground battle in a fresh world; returns {stars, survivors}. */
    private static int[] battle(int soldiers) throws Exception {
        TestWorld.reset(true);
        GameUser attacker = new GameUser("alice", "1234", 1), defender = new GameUser("bob", "1234", 2);
        GameOfThrones game = new GameOfThrones("combat", 1, attacker);
        game.addPlayer(defender);
        game.startGame();

        SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < soldiers; i++) troops.addSoldier(new Infantry());
        Mission mission = new Mission(attacker, defender, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
        mission.startAttack();
        Check.that(mission.isCompleted(), soldiers + " soldiers: the mission is completed after the battle");
        return new int[]{mission.calculateStars(), troops.getCount()};
    }

    @Test
    void battlesStaySaneAndAStrongerArmyNeverDoesWorse() throws Exception {
        remove();
        int[] weak = battle(1);
        int[] strong = battle(200);
        Check.that(weak[0] >= 0 && weak[0] <= 3 && strong[0] >= 0 && strong[0] <= 3, "stars stay between 0 and 3");
        Check.that(weak[1] >= 0 && weak[1] <= 1, "survivors never exceed the soldiers sent (weak: " + weak[1] + ")");
        Check.that(strong[1] >= 0 && strong[1] <= 200, "survivors never exceed the soldiers sent (strong: " + strong[1] + ")");
        Check.that(strong[0] >= weak[0], "200 soldiers get at least as many stars as 1 (" + strong[0] + " vs " + weak[0] + ")");
        Check.assertAll();
    }
}
