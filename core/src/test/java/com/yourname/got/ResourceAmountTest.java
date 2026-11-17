package com.yourname.got;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/** The HUD shows floating +N / -N from GameMediator.getResourceAmount (numeric, unlike the "a/b" label text). */
class ResourceAmountTest {
    @Test
    void amountsFollowTheKingdom() throws Exception {
        TestWorld.reset(true);
        GameMediator idle = new GameMediator(null);
        Check.that(idle.getResourceAmount("Wood") == 0, "outside a game the amount is 0");

        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("amounts", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Kingdom k = u1.getKingdom();
        Storage st = new Storage("Wood", k); st.markAsBuilt(); k.addStorage(st); k.getBuildings().add(st);

        GameMediator med = new GameMediator(null);
        for (String f : new String[]{"currentUser", "currentGame"}) {
            Field field = GameMediator.class.getDeclaredField(f);
            field.setAccessible(true);
            field.set(med, f.equals("currentUser") ? u1 : g);
        }
        Check.that(med.getResourceAmount("Wood") == 0, "empty storage: 0");
        k.addResources("Wood", 250);
        Check.that(med.getResourceAmount("Wood") == 250, "after +250: " + med.getResourceAmount("Wood"));
        k.detuctResources(100, 0, 0, 0);
        Check.that(med.getResourceAmount("Wood") == 150, "after -100: " + med.getResourceAmount("Wood"));
        Check.assertAll();
    }
}
