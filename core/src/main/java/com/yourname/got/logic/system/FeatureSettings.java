package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.Result;
import com.yourname.got.logic.util.SafeFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The switches and numbers of the optional features (bots, seasonal events), editable by the admin panel.
 * Only the keys listed in {@link #SPEC} can be written, and only with a whole number inside their range, so a typo
 * can never put the game in a state the code does not expect. The values are saved into balance.properties
 * (same file BalanceConfig reads; comments and other keys are kept) and take effect at once.
 */
public final class FeatureSettings {
    public static final class Spec {
        public final String key, label;
        public final int min, max, def;
        Spec(String key, String label, int min, int max, int def) {
            this.key = key; this.label = label; this.min = min; this.max = max; this.def = def;
        }
        public boolean isSwitch() { return min == 0 && max == 1; }
    }

    public static final List<Spec> SPEC = List.of(
        new Spec("bots.count", "Bots in a new game", 0, 6, 0),
        new Spec("bots.garrison", "Bot starting soldiers", 0, 500, 30),
        new Spec("bots.growth.perDay", "Bot free soldiers per day", 0, 50, 0),
        new Spec("bots.garrison.max", "Bot soldier cap", 0, 2000, 200),
        new Spec("bots.build.enabled", "Bots build (0/1)", 0, 1, 0),
        new Spec("bots.train.perDay", "Bot training per day", 0, 50, 0),
        new Spec("events.enabled", "Seasonal events (0/1)", 0, 1, 0),
        new Spec("events.chance.percent", "Event chance per day (%)", 0, 100, 15),
        new Spec("events.effect.percent", "Event effect (%)", 1, 50, 10),
        new Spec("research.enabled", "Research tree (0/1)", 0, 1, 0),
        new Spec("research.cost.percent", "Research cost (%)", 10, 500, 100),
        new Spec("research.time.percent", "Research time (%)", 10, 1000, 100));

    private FeatureSettings() {}

    public static Spec find(String key) {
        for (Spec s : SPEC) if (s.key.equals(key)) return s;
        return null;
    }

    public static int get(String key) {
        Spec s = find(key);
        return s == null ? 0 : BalanceConfig.integer(key, s.def, s.min, s.max);
    }

    /** Validates and saves one value. */
    public static synchronized Result set(String key, String rawValue) {
        Spec s = find(key);
        if (s == null) return new Result(false, "Unknown setting: " + key);
        int v;
        try {
            v = Integer.parseInt(rawValue == null ? "" : rawValue.trim());
        } catch (NumberFormatException e) {
            return new Result(false, s.label + " must be a whole number.");
        }
        if (v < s.min || v > s.max) {
            return new Result(false, s.label + " must be between " + s.min + " and " + s.max + ".");
        }
        try {
            write(key, Integer.toString(v));
        } catch (IOException e) {
            GameLog.error("Could not save " + key + ": " + e);
            return new Result(false, "Could not save the setting (" + e.getMessage() + ").");
        }
        BalanceConfig.reload();
        GameLog.info("ADMIN: " + key + " = " + v);
        return new Result(true, s.label + " is now " + v + ".");
    }

    private static File targetFile() {
        File f = new File("balance.properties");
        if (f.isFile()) return f;
        File assets = new File("assets/balance.properties");
        return assets.isFile() ? assets : f;
    }

    private static void write(String key, String value) throws IOException {
        File target = targetFile();
        List<String> lines = target.isFile()
            ? new ArrayList<>(Files.readAllLines(target.toPath(), StandardCharsets.UTF_8)) : new ArrayList<>();
        boolean done = false;
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (!t.startsWith("#") && (t.startsWith(key + "=") || t.startsWith(key + " ="))) {
                lines.set(i, key + "=" + value);
                done = true;
            }
        }
        if (!done) lines.add(key + "=" + value);
        File tmp = new File(target.getPath() + ".tmp");
        Files.write(tmp.toPath(), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        SafeFiles.replace(tmp, target);
    }
}
