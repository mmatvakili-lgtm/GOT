package com.yourname.got;

import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** A damaged main save must not lose the game: the backup next to it is used, and writes leave no temp files. */
class SaveBackupTest {
    private static File saveOf(GameOfThrones g) {
        return new File("GAMES" + File.separator + "game_" + g.getId() + File.separator + g.getName() + "_" + g.getId() + ".ser");
    }

    @Test
    void corruptMainSaveIsRecoveredFromTheBackup() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("backup", 1, u1);
        g.addPlayer(u2);
        g.startGame();
        GameFileUtility.saveFullGameState(g);

        File main = saveOf(g);
        File backup = new File(main.getPath() + ".bak");
        File tmp = new File(main.getPath() + ".tmp");
        Check.that(main.exists() && main.length() > 0, "the main save exists");
        Check.that(backup.exists() && backup.length() > 0, "a backup was created next to the save");
        Check.that(!tmp.exists(), "no temporary file is left behind after saving");

        Files.writeString(main.toPath(), "this is not a serialized game"); // simulate a damaged save
        GameOfThrones loaded = GameFileUtility.loadGame(g.getName(), g.getId());
        Check.that(loaded != null, "the game is recovered from the backup");
        Check.that(loaded != null && loaded.getNumberOfPlayers() == 2, "the recovered game has its players");
        Check.assertAll();
    }

    @Test
    void missingSaveWithoutBackupGivesNull() throws Exception {
        TestWorld.reset(true);
        Check.that(GameFileUtility.loadGame("nothing", 9) == null, "a game that never existed loads as null");
        Check.assertAll();
    }
}
