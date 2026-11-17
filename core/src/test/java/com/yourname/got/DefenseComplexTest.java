package com.yourname.got;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Building.DefenseComplex;
import com.yourname.got.logic.model.Building.Gate;
import com.yourname.got.logic.model.Building.WallsOfGate;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

/** Breaching the gate and the walls must flag them as destroyed (otherwise they cannot be repaired). */
class DefenseComplexTest {
    private static DefenseComplex complex(Kingdom k) {
        DefenseComplex c = new DefenseComplex();
        c.gate = new Gate(k);
        c.gate.markAsBuilt();
        for (int i = 0; i < 2; i++) {
            WallsOfGate w = new WallsOfGate(k);
            w.markAsBuilt();
            c.wall.add(w);
        }
        return c;
    }

    @Test
    void damageDestroysWallsAndGate() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("defense", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom k = u1.getKingdom();

        DefenseComplex c = complex(k);
        c.applyDamage(AttackType.GROUND, 1_000_000);
        boolean allWallsDestroyed = true;
        for (WallsOfGate w : c.wall) allWallsDestroyed &= w.isDestroyed();
        Check.that(allWallsDestroyed, "both walls are flagged destroyed after enough damage");
        Check.that(c.gate.isDestroyed() && c.gate.getStatus() == Status.destroyed, "the gate is flagged destroyed too");

        DefenseComplex d = complex(k);
        d.destroyComplex();
        Check.that(d.gate.isDestroyed() && d.wall.getHead().data.isDestroyed(), "destroyComplex() flags everything destroyed (repairable)");
        Check.assertAll();
    }
}
