package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.system.SeasonalEvents;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Seasonal events: off by default, rolled once per day, never crash the game. */
class SeasonalEventsTest {
    private static final File FILE = new File("balance.properties");

    private static void configure(String text) throws Exception {
        Files.writeString(FILE.toPath(), text);
        BalanceConfig.reload();
    }

    private static void remove() {
        FILE.delete();
        BalanceConfig.reload();
    }

    private static GameOfThrones newGame(String name) throws Exception {
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones(name, 1, u1);
        g.addPlayer(u2);
        g.startGame();
        return g;
    }

    @Test
    void offByDefaultDoesNothing() throws Exception {
        TestWorld.reset(true);
        remove();
        GameOfThrones g = newGame("ev-off");
        Check.that(!SeasonalEvents.enabled(), "events are off by default");
        Check.that(SeasonalEvents.onDayAdvanced(g, 5) == null, "no event when disabled");
        Check.that(g.getLastEventDay() == 0, "the day is not consumed when disabled");
        Check.assertAll();
        remove();
    }

    @Test
    void oneRollPerDayAndAlwaysFiresAtHundredPercent() throws Exception {
        TestWorld.reset(true);
        configure("events.enabled=1\nevents.chance.percent=100\nevents.effect.percent=10\n");
        GameOfThrones g = newGame("ev-on");
        String first = SeasonalEvents.onDayAdvanced(g, 7);
        Check.that(first != null, "an event fires at 100% chance");
        Check.that(g.getLastEventDay() == 7, "the day is remembered");
        Check.that(SeasonalEvents.onDayAdvanced(g, 7) == null, "the same day never rolls twice");
        Check.that(SeasonalEvents.onDayAdvanced(g, 6) == null, "an older day never rolls");
        Check.that(SeasonalEvents.onDayAdvanced(g, 8) != null, "the next day rolls again");
        Check.assertAll();
        remove();
    }

    @Test
    void zeroChanceNeverFires() throws Exception {
        TestWorld.reset(true);
        configure("events.enabled=1\nevents.chance.percent=0\n");
        GameOfThrones g = newGame("ev-zero");
        for (int d = 1; d <= 30; d++) {
            Check.that(SeasonalEvents.onDayAdvanced(g, d) == null, "no event on day " + d);
        }
        Check.assertAll();
        remove();
    }
}
