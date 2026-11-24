package com.yourname.got.logic.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central log of the game.
 *
 * Every message still goes to the console (exactly like the old System.out / System.err calls), and is also
 * written to "logs/got.log" with a time stamp and a level, so a problem that happened while nobody was watching the
 * terminal can be found afterwards. The file is rotated (got.log -> got.log.1) when it gets bigger than 2 MB.
 * Logging never throws: a broken log file must not break the game.
 */
public final class GameLog {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "got.log";
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final long FLUSH_EVERY_MILLIS = 1000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static BufferedWriter writer;
    private static long writtenBytes;
    private static long lastFlush;
    private static boolean fileDisabled = false;
    private static boolean consoleEnabled = true;

    private GameLog() {}

    public static void info() { log("INFO", ""); }

    public static void info(Object message) { log("INFO", message); }

    public static void warn(Object message) { log("WARN", message); }

    /** Goes to System.err, like the old System.err.println calls. */
    public static void error(Object message) { log("ERROR", message); }

    /** Tests or tools can silence the console; the file keeps being written. */
    public static synchronized void setConsoleEnabled(boolean enabled) {
        consoleEnabled = enabled;
    }

    /** Writes what is still buffered to the file (also done every second and when the JVM stops). */
    public static synchronized void flush() {
        if (writer == null) return;
        try {
            writer.flush();
            lastFlush = System.currentTimeMillis();
        } catch (IOException ignored) {
            // nothing sensible to do if the log itself fails
        }
    }

    private static synchronized void log(String level, Object message) {
        String text = String.valueOf(message);
        if (consoleEnabled) {
            if ("ERROR".equals(level)) System.err.println(text);
            else System.out.println(text);
        }
        writeToFile(level, text);
    }

    private static void writeToFile(String level, String text) {
        if (fileDisabled) return;
        try {
            if (writer == null) open();
            String line = TIME.format(LocalDateTime.now()) + " [" + level + "] " + text + System.lineSeparator();
            writer.write(line);
            writtenBytes += line.length();
            long now = System.currentTimeMillis();
            // Warnings and errors are flushed at once; the rest at most once a second (the game logs a lot).
            if (!"INFO".equals(level) || now - lastFlush >= FLUSH_EVERY_MILLIS) {
                writer.flush();
                lastFlush = now;
            }
            if (writtenBytes > MAX_BYTES) rotate();
        } catch (IOException | RuntimeException e) {
            fileDisabled = true; // do not retry (and slow the game down) on every message
            closeQuietly();
        }
    }

    private static void open() throws IOException {
        File dir = new File(LOG_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + LOG_DIR);
        }
        File file = new File(LOG_FILE);
        writtenBytes = file.exists() ? file.length() : 0;
        writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        lastFlush = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(GameLog::flush, "GameLog-flush"));
    }

    private static void rotate() throws IOException {
        closeQuietly();
        File current = new File(LOG_FILE);
        File old = new File(LOG_FILE + ".1");
        Files.move(current.toPath(), old.toPath(), StandardCopyOption.REPLACE_EXISTING);
        writtenBytes = 0;
        writer = Files.newBufferedWriter(current.toPath(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void closeQuietly() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ignored) {
            // ignore
        }
        writer = null;
    }
}
