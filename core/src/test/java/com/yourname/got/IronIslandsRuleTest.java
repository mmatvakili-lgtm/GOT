package com.yourname.got;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.util.Result;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/** A ground army has no entry point into Iron Islands (it only has ports): the attack must be refused up front. */
class IronIslandsRuleTest {
    @Test
    void groundAttackOnIronIslandsIsRefusedWithAReason() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("rule", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Field type = com.yourname.got.logic.model.Kingdom.class.getDeclaredField("type");
        type.setAccessible(true);
        type.set(u1.getKingdom(), KingdomType.WINTERFELL);
        type.set(u2.getKingdom(), KingdomType.IRON_ISLANDS);
        g.getGameTime().setGameSpeed(1);
        g.getGameTime().setStartTime(System.currentTimeMillis() - 30L * 24 * 3600 * 1000); // summer: attacks are allowed
        g.getGameTime().updateTime();
        Result r = g.getRulesManager().canAttack(u1, u2, AttackType.GROUND);
        Check.that(!r.isSuccess() && r.getMessage().contains("only be attacked by sea"), "refused with a reason: " + r.getMessage());
        Check.assertAll();
    }
}
