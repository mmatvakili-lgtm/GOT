package com.yourname.got;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.system.FeatureSettings;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

/** The admin can only set known settings, inside their range; other lines of the file are kept. */
class FeatureSettingsTest {
    @Test
    void validatesAndKeepsTheRestOfTheFile() throws Exception {
        TestWorld.reset(true);
        File f = new File("balance.properties");
        Files.writeString(f.toPath(), "# my comment\nloot.percent=20\nevents.enabled=0\n");
        BalanceConfig.reload();
        try {
            Check.that(FeatureSettings.get("events.enabled") == 0, "starts off");
            Check.that(FeatureSettings.set("events.enabled", "1").isSuccess(), "valid value is saved");
            Check.that(FeatureSettings.get("events.enabled") == 1, "takes effect at once");
            Check.that(!FeatureSettings.set("events.enabled", "2").isSuccess(), "out of range is refused");
            Check.that(!FeatureSettings.set("events.enabled", "abc").isSuccess(), "text is refused");
            Check.that(!FeatureSettings.set("loot.percent", "5").isSuccess(), "unknown keys cannot be written");
            Check.that(FeatureSettings.set("bots.count", "2").isSuccess(), "a new key is appended");
            Check.that(FeatureSettings.get("bots.count") == 2, "new key is read back");
            String text = Files.readString(f.toPath());
            Check.that(text.contains("# my comment") && text.contains("loot.percent=20"), "comments and other keys are kept");
            Check.that(!new File("balance.properties.tmp").exists(), "no temp file left");
            Check.assertAll();
        } finally {
            f.delete();
            BalanceConfig.reload();
        }
    }
}
