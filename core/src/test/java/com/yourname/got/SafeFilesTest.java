package com.yourname.got;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.UserSystem;
import com.yourname.got.logic.util.SafeFiles;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Replacing a file never loses the old content, and user saves leave no temporary files. */
class SafeFilesTest {
    @Test
    void replaceOverwritesTheTargetAndRemovesTheTemp() throws Exception {
        TestWorld.reset(true);
        File target = new File("safe_target.txt");
        File tmp = new File("safe_target.txt.tmp");
        Files.writeString(target.toPath(), "old");
        Files.writeString(tmp.toPath(), "new");
        SafeFiles.replace(tmp, target);
        Check.that("new".equals(Files.readString(target.toPath())), "the target has the new content");
        Check.that(!tmp.exists(), "the temp file is gone");
        target.delete();
        Check.assertAll();
    }

    @Test
    void failedReplaceKeepsTheOldFile() throws Exception {
        TestWorld.reset(true);
        File target = new File("safe_keep.txt");
        Files.writeString(target.toPath(), "keep me");
        boolean ok = SafeFiles.replaceQuietly(new File("does_not_exist.tmp"), target);
        Check.that(!ok, "replacing from a missing temp file reports failure");
        Check.that("keep me".equals(Files.readString(target.toPath())), "the old file is untouched");
        target.delete();
        Check.assertAll();
    }

    @Test
    void userSaveLeavesNoTempFiles() throws Exception {
        TestWorld.reset(true);
        GameUser user = new GameUser("carol", "1234", 3);
        Check.that(UserSystem.updateUser(user).isSuccess(), "the user is saved");
        File[] leftovers = new File("users/user_3").listFiles((d, name) -> name.endsWith(".tmp"));
        Check.that(leftovers != null && leftovers.length == 0, "no .tmp file stays in the user folder");
        Check.that(new File("users/user_3/user_3.ser").length() > 0, "the user file has content");
        Check.that(!new File("users_index.txt.tmp").exists(), "no temp index file stays behind");
        Check.assertAll();
    }
}
