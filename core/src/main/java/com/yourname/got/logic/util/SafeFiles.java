package com.yourname.got.logic.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * File helpers that never leave a half-written file behind: the data is written to "<name>.tmp" first and then
 * moved over the real file in one step (the old "delete the file, then rename the temp one" pattern lost the file
 * if the program stopped between the two calls).
 */
public final class SafeFiles {
    private SafeFiles() {}

    /** Replaces target with tmp (atomically when the file system can do it). */
    public static void replace(File tmp, File target) throws IOException {
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Like replace, but reports a failure to the log instead of throwing; true if the target was replaced. */
    public static boolean replaceQuietly(File tmp, File target) {
        try {
            replace(tmp, target);
            return true;
        } catch (IOException | RuntimeException e) {
            GameLog.error("Could not replace " + target.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /** Serializes the object to a temporary file and then replaces the target with it. */
    public static void writeObject(File target, Object object) throws IOException {
        File tmp = new File(target.getPath() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmp))) {
            out.writeObject(object);
        } catch (IOException | RuntimeException e) {
            tmp.delete();
            throw e;
        }
        replace(tmp, target);
    }
}
