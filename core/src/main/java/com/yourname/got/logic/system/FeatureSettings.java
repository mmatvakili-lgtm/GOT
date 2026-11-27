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
    /** The three optional features, each with a short explanation for the admin screen. */
    public enum Group {
        BOTS("Bots", "Computer-controlled kingdoms that fill empty player slots."),
        EVENTS("Seasonal Events", "Random harvests, famines and trade windfalls that follow the seasons."),
        RESEARCH("Research Tree", "Technologies that permanently improve a kingdom."),
        DIPLOMACY("Diplomacy", "Peace treaties and alliances: players can agree not to attack each other.");

        public final String title, summary;
        Group(String title, String summary) { this.title = title; this.summary = summary; }
    }

    public static final class Spec {
        public final String key, label, description, applies, unit;
        public final int min, max, def;
        public final Group group;

        Spec(Group group, String key, String label, String description, int min, int max, int def, String unit, String applies) {
            this.group = group; this.key = key; this.label = label; this.description = description;
            this.min = min; this.max = max; this.def = def; this.unit = unit; this.applies = applies;
        }

        public boolean isSwitch() { return min == 0 && max == 1; }
    }

    private static final String NOW = "Applies at once";
    private static final String NEXT_GAME = "Applies when the next game starts";

    public static final List<Spec> SPEC = List.of(
        new Spec(Group.BOTS, "bots.count", "Bots in a new game",
            "How many bots join each new game (they take free slots; the game holds 7 kingdoms at most). 0 = no bots.",
            0, 6, 0, "", NEXT_GAME),
        new Spec(Group.BOTS, "bots.garrison", "Starting soldiers",
            "Soldiers each bot starts with. They defend the bot's castle against real players.",
            0, 500, 30, " soldiers", NEXT_GAME),
        new Spec(Group.BOTS, "bots.growth.perDay", "Free soldiers per day",
            "Soldiers each bot gains for free every game day, until it reaches the cap below. 0 = bots never grow.",
            0, 50, 0, " / day", NOW),
        new Spec(Group.BOTS, "bots.garrison.max", "Soldier cap",
            "Bots stop growing (and training) once they own this many soldiers.",
            0, 2000, 200, " soldiers", NOW),
        new Spec(Group.BOTS, "bots.build.enabled", "Bots build an economy",
            "Each game day a bot builds one storage or mine with the same rules and costs as a player (the first of each is free), "
                + "then upgrades its castle and builds a Camp and a Barrack.",
            0, 1, 0, "", NOW),
        new Spec(Group.BOTS, "bots.train.perDay", "Soldiers trained per day",
            "Soldiers a bot queues in its Barrack every day, paying the normal cost. Needs the economy option above to reach a Barrack.",
            0, 50, 0, " / day", NOW),

        new Spec(Group.BOTS, "bots.attack.enabled", "Bots attack",
            "Lets bots march on real players. Off = bots only defend. Bots obey every normal rule (no attacks in spring or winter, "
                + "peace treaties and alliances protect you) and the target gets a warning as soon as they set out.",
            0, 1, 0, "", NOW),
        new Spec(Group.BOTS, "bots.attack.startDay", "Grace period",
            "Bots do not attack before this game day, so new players can build up first.",
            0, 200, 10, " days", NOW),
        new Spec(Group.BOTS, "bots.attack.days", "Attack interval",
            "A bot attacks at most once in this many game days, and never while its last army is still marching.",
            1, 60, 7, " days", NOW),
        new Spec(Group.BOTS, "bots.attack.strength", "Army sent",
            "Share of its infantry, archers and cavalry a bot sends. The rest stays home to defend.",
            10, 100, 50, " %", NOW),

        new Spec(Group.EVENTS, "events.enabled", "Seasonal events",
            "Turns the events on. Summer and fall can bring a Bountiful Harvest, winter a Famine, spring a Trade Windfall. "
                + "Every kingdom (players and bots) is affected the same, so nobody is favoured.",
            0, 1, 0, "", NOW),
        new Spec(Group.EVENTS, "events.chance.percent", "Chance per game day",
            "How likely it is that a given game day has an event. One roll per day, the same for everybody.",
            0, 100, 15, " %", NOW),
        new Spec(Group.EVENTS, "events.effect.percent", "Event strength",
            "Harvest and windfall add this percent of a kingdom's storage capacity; a famine removes this percent of its food.",
            1, 50, 10, " %", NOW),

        new Spec(Group.DIPLOMACY, "diplomacy.enabled", "Diplomacy",
            "Turns diplomacy on. Players open it from Extra > Diplomacy to offer a Peace Treaty (ends after a set number of game days) "
                + "or an Alliance (lasts until someone cancels). While a pact is active neither side can attack the other.",
            0, 1, 0, "", NOW),
        new Spec(Group.DIPLOMACY, "diplomacy.peace.days", "Peace treaty length",
            "How many game days a peace treaty lasts after it is accepted. Alliances have no end date.",
            1, 60, 10, " days", NOW),

        new Spec(Group.RESEARCH, "research.enabled", "Research tree",
            "Turns research on: 15 technologies in Economy, Military and Defense. Players open it from Extra > Research Tree. "
                + "When switched off, every bonus stops (what was learned is kept and returns when you switch it on).",
            0, 1, 0, "", NOW),
        new Spec(Group.RESEARCH, "research.cost.percent", "Research cost",
            "Percent of the listed resource cost that is paid. 50 = half price, 200 = double. Affects levels queued from now on.",
            10, 500, 100, " %", NOW),
        new Spec(Group.RESEARCH, "research.time.percent", "Research time",
            "Percent of the listed research time. 50 = twice as fast, 200 = twice as slow. Affects levels started from now on.",
            10, 1000, 100, " %", NOW));

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

    /** Validates every value first, then saves them all in ONE write, so a bad value changes nothing. */
    public static synchronized Result setMany(java.util.Map<String, Integer> values) {
        if (values == null || values.isEmpty()) return new Result(true, "Nothing to save.");
        for (java.util.Map.Entry<String, Integer> e : values.entrySet()) {
            Spec s = find(e.getKey());
            if (s == null) return new Result(false, "Unknown setting: " + e.getKey());
            if (e.getValue() == null || e.getValue() < s.min || e.getValue() > s.max) {
                return new Result(false, s.label + " must be between " + s.min + " and " + s.max + ".");
            }
        }
        try {
            writeAll(values);
        } catch (IOException e) {
            GameLog.error("Could not save settings: " + e);
            return new Result(false, "Could not save the settings (" + e.getMessage() + ").");
        }
        BalanceConfig.reload();
        GameLog.info("ADMIN: saved " + values);
        return new Result(true, values.size() + " setting(s) saved.");
    }

    private static File targetFile() {
        File f = new File("balance.properties");
        if (f.isFile()) return f;
        File assets = new File("assets/balance.properties");
        return assets.isFile() ? assets : f;
    }

    private static void write(String key, String value) throws IOException {
        writeAll(java.util.Map.of(key, Integer.parseInt(value)));
    }

    private static void writeAll(java.util.Map<String, Integer> values) throws IOException {
        File target = targetFile();
        List<String> lines = target.isFile()
            ? new ArrayList<>(Files.readAllLines(target.toPath(), StandardCharsets.UTF_8)) : new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : values.entrySet()) {
            String key = e.getKey();
            boolean done = false;
            for (int i = 0; i < lines.size(); i++) {
                String t = lines.get(i).trim();
                if (!t.startsWith("#") && (t.startsWith(key + "=") || t.startsWith(key + " ="))) {
                    lines.set(i, key + "=" + e.getValue());
                    done = true;
                }
            }
            if (!done) lines.add(key + "=" + e.getValue());
        }
        File tmp = new File(target.getPath() + ".tmp");
        Files.write(tmp.toPath(), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        SafeFiles.replace(tmp, target);
    }
}
