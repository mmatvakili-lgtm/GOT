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

    @Test
    void setManyIsAllOrNothing() throws Exception {
        TestWorld.reset(true);
        File f = new File("balance.properties");
        Files.writeString(f.toPath(), "# keep me\nloot.percent=20\nresearch.enabled=0\n");
        BalanceConfig.reload();
        try {
            java.util.Map<String, Integer> bad = new java.util.LinkedHashMap<>();
            bad.put("research.enabled", 1);
            bad.put("bots.count", 99);
            Check.that(!FeatureSettings.setMany(bad).isSuccess(), "one bad value refuses the whole batch");
            Check.that(FeatureSettings.get("research.enabled") == 0, "nothing was written");
            java.util.Map<String, Integer> good = new java.util.LinkedHashMap<>();
            good.put("research.enabled", 1);
            good.put("research.cost.percent", 50);
            good.put("bots.count", 2);
            Check.that(FeatureSettings.setMany(good).isSuccess(), "valid batch is saved");
            Check.that(FeatureSettings.get("research.enabled") == 1 && FeatureSettings.get("research.cost.percent") == 50
                && FeatureSettings.get("bots.count") == 2, "all values read back");
            String text = Files.readString(f.toPath());
            Check.that(text.contains("# keep me") && text.contains("loot.percent=20"), "comments and other keys kept");
            Check.that(!new File("balance.properties.tmp").exists(), "no temp file left");
            Check.assertAll();
        } finally {
            f.delete();
            BalanceConfig.reload();
        }
    }
}
