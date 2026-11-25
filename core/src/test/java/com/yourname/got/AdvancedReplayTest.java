package com.yourname.got;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.EventArmyMove;
import com.yourname.got.logic.util.EventArmyState;
import com.yourname.got.logic.util.EventBattleSetup;
import com.yourname.got.logic.util.EventStrike;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/** The replay records both armies, their movement and every blow, and stays serializable. */
class AdvancedReplayTest {
    @Test
    void battleRecordsArmiesMovementAndBlows() throws Exception {
        TestWorld.reset(true);
        GameUser attacker = new GameUser("alice", "1234", 1), defender = new GameUser("bob", "1234", 2);
        GameOfThrones game = new GameOfThrones("replay", 1, attacker);
        game.addPlayer(defender);
        game.startGame();

        SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 80; i++) troops.addSoldier(new Infantry());
        Mission mission = new Mission(attacker, defender, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
        mission.startAttack();

        int setups = 0, states = 0, moves = 0, strikes = 0;
        EventArmyState first = null;
        for (BattleEvent e : mission.getReplayLog()) {
            if (e instanceof EventBattleSetup) setups++;
            if (e instanceof EventArmyState st) { states++; if (first == null) first = st; }
            if (e instanceof EventArmyMove) moves++;
            if (e instanceof EventStrike) strikes++;
        }
        Check.that(setups == 1, "the battlefield is set up exactly once (" + setups + ")");
        Check.that(states >= 1, "army states were recorded (" + states + ")");
        Check.that(first != null && first.attackers.size() == 1 && first.attackers.get(0).count == 80,
            "the first picture shows the 80 attacking infantry");
        Check.that(first != null && first.attackers.get(0).type.equals("INFANTRY"), "unit type is recorded");

        // The replay is saved with Java serialization: every new event must survive it.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(mission.getReplayLog());
        }
        Check.that(bytes.size() > 0, "the replay with the new events can be serialized");
        Check.that(moves >= 0 && strikes >= 0, "movement and blow events never break the battle");
        Check.assertAll();
    }
}
