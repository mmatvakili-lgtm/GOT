package com.yourname.got.logic.Balance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Optional tuning knobs read from a plain text file, so the feel of the game can be changed without recompiling
 * (e.g. faster building while testing). The formulas themselves are not touched: every knob defaults to the
 * current behaviour and a missing, unreadable or invalid value simply falls back to that default.
 *
 * The file is "balance.properties" in the working directory (or in "assets/"). It is re-read when it changes.
 *
 * Keys: production.multiplier, cost.multiplier, buildtime.multiplier (1.0 = unchanged),
 *       mercenary.slots.per.castle.level, hero.revive.minutes, loot.percent, game.tick.millis
 */
public final class BalanceConfig {
    private static final String[] LOCATIONS = {"balance.properties", "assets/balance.properties"};
    private static final long RECHECK_MILLIS = 2000;

    private static Properties props = new Properties();
    private static long lastCheck = 0;
    private static String loadedFrom = null;
    private static long loadedModified = -1;

    private BalanceConfig() {}

    /** Forces the file to be read again on the next access. */
    public static synchronized void reload() {
        props = new Properties(); // forget the old values: the file may be gone now
        lastCheck = 0;
        loadedFrom = null;
        loadedModified = -1;
    }

    private static synchronized Properties current() {
        long now = System.currentTimeMillis();
        if (now - lastCheck >= RECHECK_MILLIS || lastCheck == 0) {
            lastCheck = now;
            File found = null;
            for (String location : LOCATIONS) {
                File f = new File(location);
                if (f.isFile()) { found = f; break; }
            }
            if (found == null) {
                if (loadedFrom != null) { props = new Properties(); loadedFrom = null; loadedModified = -1; }
            } else if (!found.getPath().equals(loadedFrom) || found.lastModified() != loadedModified) {
                Properties fresh = new Properties();
                try (InputStream in = new FileInputStream(found)) {
                    fresh.load(in);
                } catch (IOException | IllegalArgumentException e) {
                    fresh = new Properties(); // unreadable file: defaults
                }
                props = fresh;
                loadedFrom = found.getPath();
                loadedModified = found.lastModified();
            }
        }
        return props;
    }

    /** A positive factor; anything missing, unparsable or <= 0 gives 1.0. */
    public static double multiplier(String key) {
        double v = number(key, 1.0);
        return v > 0 ? v : 1.0;
    }

    public static double number(String key, double fallback) {
        String raw = current().getProperty(key);
        if (raw == null) return fallback;
        try {
            double v = Double.parseDouble(raw.trim());
            return (Double.isNaN(v) || Double.isInfinite(v)) ? fallback : v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** An integer that must stay inside [min, max]; otherwise the fallback is used. */
    public static int integer(String key, int fallback, int min, int max) {
        String raw = current().getProperty(key);
        if (raw == null) return fallback;
        try {
            int v = Integer.parseInt(raw.trim());
            return (v < min || v > max) ? fallback : v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
