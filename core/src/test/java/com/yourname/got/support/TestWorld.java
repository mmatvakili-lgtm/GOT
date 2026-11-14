package com.yourname.got.support;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * The game stores users and games in files relative to the working directory (users/, GAMES/, users_index.txt,
 * games.txt). Every test starts from an empty directory layout; the Gradle test task runs in build/test-work.
 */
public final class TestWorld {
    private TestWorld() {}

    /** @param preCreateFolders create users/user_1..8 and GAMES/game_0..9 (the game does not create them itself) */
    public static void reset(boolean preCreateFolders) throws IOException {
        Check.clear();
        deleteRecursively(new File("users"));
        deleteRecursively(new File("GAMES"));
        new File("users_index.txt").delete();
        new File("games.txt").delete();

        new File("users").mkdirs();
        new File("GAMES").mkdirs();
        new File("users_index.txt").createNewFile();
        new File("games.txt").createNewFile();
        if (preCreateFolders) {
            for (int i = 1; i <= 8; i++) new File("users/user_" + i).mkdirs();
            for (int i = 0; i <= 9; i++) new File("GAMES/game_" + i).mkdirs();
        }
        resetStatic("com.yourname.got.logic.login.GameFileUtility", "pendingSave", null);
        resetStatic("com.yourname.got.logic.login.GameFileUtility", "lastFullSaveMillis", 0L);
        resetStatic("com.yourname.got.mediator.GameMediator", "instance", null);
    }

    private static void resetStatic(String className, String field, Object value) {
        try {
            Field f = Class.forName(className).getDeclaredField(field);
            f.setAccessible(true);
            f.set(null, value);
        } catch (ReflectiveOperationException ignored) {
            // the field does not exist in this version: nothing to reset
        }
    }

    private static void deleteRecursively(File root) throws IOException {
        if (!root.exists()) return;
        try (var walk = Files.walk(Path.of(root.getPath()))) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}
