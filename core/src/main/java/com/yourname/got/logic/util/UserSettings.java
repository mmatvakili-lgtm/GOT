package com.yourname.got.logic.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Player settings (music and effects volume, 0..1) kept in settings.properties next to the game.
 * Bad or missing values fall back to the defaults; saving is atomic.
 */
public final class UserSettings {
    private static final String FILE = "settings.properties";
    private static final float DEFAULT_MUSIC = 0.5f;
    private static final float DEFAULT_SFX = 1.0f;

    private static float music = DEFAULT_MUSIC;
    private static float sfx = DEFAULT_SFX;
    private static boolean effects = true;

    private UserSettings() {}

    public static synchronized void load() {
        music = DEFAULT_MUSIC;
        sfx = DEFAULT_SFX;
        effects = true;
        File f = new File(FILE);
        if (!f.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            GameLog.error("Could not read settings: " + e);
            return;
        }
        music = parse(p.getProperty("music.volume"), DEFAULT_MUSIC);
        sfx = parse(p.getProperty("sfx.volume"), DEFAULT_SFX);
        effects = !"false".equalsIgnoreCase(p.getProperty("visual.effects", "true").trim());
    }

    public static synchronized void save() {
        Properties p = new Properties();
        p.setProperty("music.volume", Float.toString(music));
        p.setProperty("sfx.volume", Float.toString(sfx));
        p.setProperty("visual.effects", Boolean.toString(effects));
        File target = new File(FILE);
        File tmp = new File(FILE + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            p.store(out, "Game settings");
        } catch (IOException e) {
            GameLog.error("Could not save settings: " + e);
            return;
        }
        SafeFiles.replaceQuietly(tmp, target);
    }

    public static synchronized float getMusicVolume() { return music; }
    public static synchronized float getSfxVolume() { return sfx; }
    /** Small visual extras such as window fade-ins (Settings > Visual effects). On by default. */
    public static synchronized boolean isEffectsEnabled() { return effects; }
    public static synchronized void setEffectsEnabled(boolean on) { effects = on; }
    public static synchronized void setMusicVolume(float v) { music = clamp(v); }
    public static synchronized void setSfxVolume(float v) { sfx = clamp(v); }

    private static float parse(String s, float fallback) {
        if (s == null) return fallback;
        try {
            float v = Float.parseFloat(s.trim());
            return Float.isNaN(v) ? fallback : clamp(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float clamp(float v) {
        return Float.isNaN(v) ? 0f : Math.max(0f, Math.min(1f, v));
    }
}
