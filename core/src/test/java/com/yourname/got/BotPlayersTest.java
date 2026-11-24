package com.yourname.got;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.UserSystem;
import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Computer players: off by default, never stored with the real users, and the game keeps working with them. */
class BotPlayersTest {
    private static final File FILE = new File("balance.properties");

    private static void configure(String text) throws Exception {
        Files.writeString(FILE.toPath(), text);
        BalanceConfig.reload();
    }

    private static void remove() {
        FILE.delete();
        BalanceConfig.reload();
    }

    private static int bots(GameOfThrones g) {
        int n = 0;
        for (GameUser u : g.getUsers()) if (BotUser.isBot(u)) n++;
        return n;
    }

    @Test
    void noBotsByDefault() throws Exception {
        TestWorld.reset(true);
        remove();
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("nobots", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        Check.that(bots(g) == 0, "bots.count defaults to 0: no bot joins");
        Check.that(g.getNumberOfPlayers() == 2, "the player count is unchanged (" + g.getNumberOfPlayers() + ")");
        Check.assertAll();
    }

    @Test
    void botsFillEmptyKingdomsAndAreNeverStoredAsUsers() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=3\nbots.garrison=20\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("withbots", 1, u1);
            g.startGame();
            GameFileUtility.saveFullGameState(g);

            Check.that(bots(g) == 3, "3 bots joined (" + bots(g) + ")");
            Check.that(g.getNumberOfPlayers() == 4, "1 player + 3 bots = 4 (" + g.getNumberOfPlayers() + ")");

            Set<String> names = new HashSet<>();
            Set<Integer> ids = new HashSet<>();
            boolean allHaveKingdomAndGuard = true;
            for (GameUser u : g.getUsers()) {
                if (u == null) continue;
                ids.add(u.getId());
                if (!BotUser.isBot(u)) continue;
                names.add(u.getUsername());
                if (u.getId() >= 0) allHaveKingdomAndGuard = false;
                if (u.getKingdom() == null) { allHaveKingdomAndGuard = false; continue; }
                if (u.getKingdom().getArmyManager().getTotalSoldier() != 20) allHaveKingdomAndGuard = false;
                if (!u.getUsername().startsWith(BotUser.NAME_PREFIX)) allHaveKingdomAndGuard = false;
            }
            Check.that(allHaveKingdomAndGuard, "every bot has a negative id, a kingdom, a 20-soldier guard and the reserved name");
            Check.that(names.size() == 3, "bot names are unique (" + names + ")");
            Check.that(ids.size() == 4, "all player ids are unique");

            List<String> index = Files.readAllLines(new File("users_index.txt").toPath());
            boolean botInIndex = index.stream().anyMatch(l -> l.startsWith("-") || l.contains(BotUser.NAME_PREFIX));
            Check.that(!botInIndex, "bots never appear in the user index");
            boolean botFolder = false;
            File[] folders = new File("users").listFiles();
            if (folders != null) for (File f : folders) if (f.getName().contains("-")) botFolder = true;
            Check.that(!botFolder, "no user folder is created for a bot");

            GameOfThrones loaded = GameFileUtility.loadGame(g.getName(), g.getId());
            Check.that(loaded != null && bots(loaded) == 3, "the bots survive saving and loading the game");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void botsNeverExceedTheNumberOfKingdoms() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=6\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
            GameOfThrones g = new GameOfThrones("full", 1, u1);
            g.addPlayer(u2);
            g.startGame();
            Check.that(g.getNumberOfPlayers() <= 7, "at most 7 players in total (" + g.getNumberOfPlayers() + ")");
            Check.that(bots(g) == 5, "2 players leave room for 5 bots (" + bots(g) + ")");
            g.startGame(); // a second start must not add more bots
            Check.that(bots(g) == 5, "starting twice does not add bots again");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void invalidBotSettingsAreIgnored() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=99\nbots.garrison=-4\n");
        try {
            Check.that(GameOfThrones.configuredBots() == 0, "an out-of-range bots.count falls back to 0");
            Check.that(GameOfThrones.configuredBotGarrison() == 30, "an out-of-range bots.garrison falls back to 30");
            configure("bots.count=abc\n");
            Check.that(GameOfThrones.configuredBots() == 0, "an unparsable bots.count falls back to 0");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void realUsersCannotTakeTheBotName() throws Exception {
        TestWorld.reset(true);
        UserSystem users = new UserSystem();
        Check.that(!users.register("[BOT] STARK", "1234").isSuccess(), "the reserved prefix is refused");
        Check.that(!users.register("  [bot] x", "1234").isSuccess(), "the prefix check ignores case and spaces");
        // (TestWorld pre-creates user folders, so registration itself may fail for an unrelated reason: only the
        // reserved-name rule matters here.)
        Check.that(!String.valueOf(users.register("Robert", "1234").getMessage()).contains("reserved"),
            "an ordinary name is not refused as reserved");
        boolean refused = false;
        try { new BotUser(5); } catch (IllegalArgumentException e) { refused = true; }
        Check.that(refused, "a bot id must be negative");
        Check.assertAll();
    }

    @Test
    void battlesAgainstABotWork() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=1\nbots.garrison=10\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("fight", 1, u1);
            g.startGame();
            GameUser bot = null;
            for (GameUser u : g.getUsers()) if (BotUser.isBot(u)) bot = u;
            Check.that(bot != null, "a bot is in the game");
            SoldierUnit troops = new SoldierUnit(SoldierType.INFANTRY);
            for (int i = 0; i < 80; i++) troops.addSoldier(new Infantry());
            Mission mission = new Mission(u1, bot, new SoldierUnit[]{troops}, 0, AttackType.GROUND);
            mission.startAttack();
            Check.that(mission.isCompleted(), "the battle against the bot completes");
            Check.that(!bot.isInFight(), "the bot is not stuck in a fight afterwards");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void gameEndsWhenEveryHumanHasFallenAndBotsRemain() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=2\nbots.garrison=5\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("lastone", 1, u1);
            g.startGame();
            g.checkGameOver();
            Check.that(!g.isFinished(), "the game goes on while the human is alive");
            g.handlePlayerDefeat(u1);
            g.checkGameOver();
            Check.that(g.isFinished(), "the game ends when no human is left, even though bots are alive");
            Check.that(!new File("users/user_-1").exists(), "ending the game created no folder for a bot");
        } finally {
            remove();
        }
        Check.assertAll();
    }

    @Test
    void botsGrowOncePerDayUpToTheCap() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=1\nbots.garrison=10\nbots.growth.perDay=5\nbots.garrison.max=18\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("grow", 1, u1);
            g.startGame();
            BotUser bot = null;
            for (GameUser u : g.getUsers()) if (BotUser.isBot(u)) bot = (BotUser) u;
            Check.that(bot != null, "a bot joined");
            int start = bot.getKingdom().getOwnedSoldierCount();
            Check.that(bot.reinforce(1) == 5, "day 1: +5 soldiers");
            Check.that(bot.reinforce(1) == 0, "the same day never grows twice");
            Check.that(bot.reinforce(2) == 3, "day 2 stops at the cap of 18 (start " + start + ")");
            Check.that(bot.reinforce(3) == 0, "no growth at the cap");
            Check.assertAll();
        } finally {
            remove();
        }
    }

    @Test
    void botsBuildOneFreeBuildingPerDayWhenEnabled() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=1\nbots.build.enabled=1\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("builder", 1, u1);
            g.startGame();
            BotUser bot = null;
            for (GameUser u : g.getUsers()) if (BotUser.isBot(u)) bot = (BotUser) u;
            Check.that(bot != null, "a bot joined");
            int before = bot.getKingdom().getBuildings().size();
            String first = bot.develop(1);
            Check.that(first != null, "day 1: the bot started a building (" + first + ")");
            Check.that(bot.getKingdom().getBuildings().size() == before + 1, "exactly one building was added");
            Check.that(bot.develop(1) == null, "the same day never builds twice");
            Check.assertAll();
        } finally {
            remove();
        }
    }

    @Test
    void botsDoNotBuildByDefault() throws Exception {
        TestWorld.reset(true);
        configure("bots.count=1\n");
        try {
            GameUser u1 = new GameUser("alice", "1234", 1);
            GameOfThrones g = new GameOfThrones("nobuild", 1, u1);
            g.startGame();
            BotUser bot = null;
            for (GameUser u : g.getUsers()) if (BotUser.isBot(u)) bot = (BotUser) u;
            int before = bot.getKingdom().getBuildings().size();
            Check.that(bot.develop(1) == null, "disabled: nothing is built");
            Check.that(bot.getKingdom().getBuildings().size() == before, "building count unchanged");
            Check.assertAll();
        } finally {
            remove();
        }
    }
}
