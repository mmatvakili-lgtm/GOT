package com.yourname.got;

import com.yourname.got.logic.util.UserSettings;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** Settings are clamped, saved, reloaded, and bad files fall back to defaults. */
class UserSettingsTest {
    private static final File FILE = new File("settings.properties");

    @Test
    void saveAndReloadKeepsClampedValues() throws Exception {
        TestWorld.reset(true);
        FILE.delete();
        UserSettings.load();
        Check.that(UserSettings.getMusicVolume() == 0.5f && UserSettings.getSfxVolume() == 1f, "defaults");
        UserSettings.setMusicVolume(2f);
        UserSettings.setSfxVolume(0.25f);
        Check.that(UserSettings.getMusicVolume() == 1f, "volume is clamped to 1");
        UserSettings.save();
        UserSettings.setMusicVolume(0f);
        UserSettings.load();
        Check.that(UserSettings.getMusicVolume() == 1f && UserSettings.getSfxVolume() == 0.25f, "reloaded from the file");
        Check.that(!new File("settings.properties.tmp").exists(), "no temp file left");
        Check.assertAll();
        FILE.delete();
        UserSettings.load();
    }

    @Test
    void badValuesFallBackToDefaults() throws Exception {
        TestWorld.reset(true);
        Files.writeString(FILE.toPath(), "music.volume=abc\nsfx.volume=NaN\n");
        UserSettings.load();
        Check.that(UserSettings.getMusicVolume() == 0.5f, "unreadable music value -> default");
        Check.that(UserSettings.getSfxVolume() == 1f, "NaN sfx value -> default");
        Check.assertAll();
        FILE.delete();
        UserSettings.load();
    }

    @Test
    void visualEffectsAreOnByDefaultAndSaved() throws Exception {
        TestWorld.reset(true);
        FILE.delete();
        UserSettings.load();
        Check.that(UserSettings.isEffectsEnabled(), "effects are on by default");
        UserSettings.setEffectsEnabled(false);
        UserSettings.save();
        UserSettings.setEffectsEnabled(true);
        UserSettings.load();
        Check.that(!UserSettings.isEffectsEnabled(), "the off choice is kept");
        Files.writeString(FILE.toPath(), "visual.effects=banana\n");
        UserSettings.load();
        Check.that(UserSettings.isEffectsEnabled(), "an unknown value means on");
        Check.that(com.yourname.got.screens.UiTheme.messageColor("ERROR") != com.yourname.got.screens.UiTheme.messageColor("SYSTEM"),
            "message types have different colours");
        Check.assertAll();
        FILE.delete();
        UserSettings.load();
    }
}
