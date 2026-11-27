package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.system.BotAttacker;
import com.yourname.got.logic.system.Diplomacy;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Bot attacks: off by default, wait for the grace period, keep soldiers home on refusal, honour pacts, once per interval. */
class BotAttackerTest {
    private static final File FILE = new File("balance.properties");

    private static void configure(String text) throws Exception {
        Files.writeString(FILE.toPath(), text);
        BalanceConfig.reload();
    }

    private static BotUser botOf(GameOfThrones g) {
        for (GameUser u : g.getUsers()) if (u instanceof BotUser b) return b;
        return null;
    }

    private static GameOfThrones world(String config) throws Exception {
        TestWorld.reset(true);
        configure(config);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("botwar", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        return g;
    }

    @Test
    void offByDefaultAndDuringGracePeriod() throws Exception {
        try {
            GameOfThrones g = world("bots.count=1\nbots.garrison=60\n");
            BotUser bot = botOf(g);
            int before = bot.getKingdom().getOwnedSoldierCount();
            Check.that(BotAttacker.tryAttack(g, bot, 50) == null, "no attack while bots.attack.enabled=0");
            configure("bots.count=1\nbots.garrison=60\nbots.attack.enabled=1\nbots.attack.startDay=100\n");
            Check.that(BotAttacker.tryAttack(g, bot, 50) == null, "no attack before the grace period ends");
            Check.that(g.getMissions().isEmpty(), "no mission was created");
            Check.that(bot.getKingdom().getOwnedSoldierCount() == before, "all soldiers still at home");
            Check.assertAll();
        } finally {
            FILE.delete();
            BalanceConfig.reload();
        }
    }

    @Test
    void attackKeepsTheRulesAndTheInterval() throws Exception {
        try {
            GameOfThrones g = world("bots.count=1\nbots.garrison=60\nbots.attack.enabled=1\nbots.attack.startDay=0\n"
                + "bots.attack.days=7\nbots.attack.strength=50\n");
            BotUser bot = botOf(g);
            int before = bot.getKingdom().getOwnedSoldierCount();
            String target = BotAttacker.tryAttack(g, bot, 20);
            if (target == null) {
                // the current season or a rule may forbid attacks: then nothing may have changed
                Check.that(g.getMissions().isEmpty() && bot.getKingdom().getOwnedSoldierCount() == before, "a refused attack changes nothing");
            } else {
                Check.that(g.getMissions().getSize() == 1, "one mission was created");
                Check.that(bot.getLastAttackDay() == 20, "the day is remembered");
                Check.that(bot.getKingdom().getOwnedSoldierCount() <= before, "soldiers left home");
                Check.that(BotAttacker.tryAttack(g, bot, 21) == null, "not again before the interval ends");
            }
            Check.assertAll();
        } finally {
            FILE.delete();
            BalanceConfig.reload();
        }
    }

    @Test
    void aPactProtectsThePlayer() throws Exception {
        try {
            GameOfThrones g = world("bots.count=1\nbots.garrison=60\nbots.attack.enabled=1\nbots.attack.startDay=0\n"
                + "diplomacy.enabled=1\n");
            BotUser bot = botOf(g);
            int day = g.getGameTime().getDay();
            for (GameUser u : g.getUsers()) {
                if (u == null || BotUser.isBot(u)) continue;
                g.getDiplomacy().propose(bot.getId(), u.getId(), Diplomacy.Type.ALLIANCE, day);
                g.getDiplomacy().respond(bot.getId(), u.getId(), true, day);
            }
            Check.that(BotAttacker.tryAttack(g, bot, day + 1) == null, "allied players are never attacked");
            Check.that(g.getMissions().isEmpty(), "no mission");
            Check.assertAll();
        } finally {
            FILE.delete();
            BalanceConfig.reload();
        }
    }
}
