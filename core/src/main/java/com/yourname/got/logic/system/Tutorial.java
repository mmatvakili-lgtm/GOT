package com.yourname.got.logic.system;

import com.yourname.got.logic.model.Kingdom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The beginner's guide. Every step is checked against the real state of the player's kingdom, so nothing is stored:
 * a step is done when the kingdom has what it asks for (also after loading a game, or if the player did it earlier).
 * Steps of optional features (research, diplomacy) only appear while that feature is switched on.
 */
public final class Tutorial {
    private Tutorial() {}

    public static final class Step {
        public final String title, why, how;
        private final Predicate<Kingdom> done;
        private final boolean optionalFeature;
        Step(String title, String why, String how, Predicate<Kingdom> done, boolean optionalFeature) {
            this.title = title; this.why = why; this.how = how; this.done = done; this.optionalFeature = optionalFeature;
        }
        public boolean isDone(Kingdom k) { return k != null && done.test(k); }
    }

    private static List<Step> all() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("Build your first mine",
            "Mines produce wood, stone, coin and food. Everything you build or train costs these resources.",
            "Right menu > New Building > Mine. The first mine of each kind is free.",
            k -> k.getMines().getSize() > 0, false));
        s.add(new Step("Build a storage",
            "Without a storage the resources you produce have nowhere to be kept. It also sets how much you can hold.",
            "Right menu > New Building > Storage. The first storage of each kind is free.",
            k -> k.getStorages().getSize() > 0, false));
        s.add(new Step("Upgrade the castle to level 3",
            "Barracks and Camps need castle level 3. A higher castle also unlocks stronger buildings.",
            "Right menu > Overhaul > Castle. Upgrading takes time and resources.",
            k -> k.getCastle() != null && k.getCastle().getLevel() >= 3, false));
        s.add(new Step("Build a Camp",
            "A Camp is where soldiers live. Your army size is limited by your Camp capacity.",
            "Right menu > New Building > Camp (needs castle level 3).",
            k -> k.countBuildingsOfType("Camp") > 0, false));
        s.add(new Step("Build a Barrack",
            "The Barrack trains soldiers. Without it you cannot grow an army.",
            "Right menu > New Building > Barrack (needs castle level 3).",
            k -> k.countBuildingsOfType("Barrack") > 0, false));
        s.add(new Step("Train your first soldiers",
            "Soldiers defend you and fight in attacks. Training takes time and costs resources.",
            "Right menu > Train Soldiers, choose a type and how many.",
            k -> k.getOwnedSoldierCount() > 0, false));
        s.add(new Step("Build a defensive tower",
            "Towers shoot at attackers and make your kingdom much harder to beat.",
            "Right menu > New Building > Defensive Tower.",
            k -> k.countBuildingsOfType("DefensiveTower") > 0, false));
        s.add(new Step("Learn a technology",
            "Research gives permanent bonuses such as more production, stronger soldiers or bigger storage.",
            "Right menu > Extra > Research Tree. Pick one and press Start.",
            k -> k.getResearch().completedLevels() > 0, true));
        s.add(new Step("Make a pact with another player",
            "A peace treaty or alliance keeps you safe from that player's attacks.",
            "Right menu > Extra > Diplomacy. Offer peace or an alliance.",
            Tutorial::hasPact, true));
        return s;
    }

    private static boolean hasPact(Kingdom k) {
        if (k.getOwner() == null || k.getOwner().getGame() == null) return false;
        var game = k.getOwner().getGame();
        return !game.getDiplomacy().pactsOf(k.getOwner().getId(), game.getGameTime().getDay()).isEmpty();
    }

    /** The steps that apply right now (optional features only when switched on). */
    public static List<Step> steps() {
        boolean research = BalanceConfigAccess.on("research.enabled");
        boolean diplomacy = Diplomacy.enabled();
        List<Step> out = new ArrayList<>();
        for (Step s : all()) {
            if (s.optionalFeature) {
                boolean isResearch = s.title.startsWith("Learn");
                if (isResearch ? !research : !diplomacy) continue;
            }
            out.add(s);
        }
        return out;
    }

    public static int doneCount(Kingdom k) {
        int n = 0;
        for (Step s : steps()) if (s.isDone(k)) n++;
        return n;
    }

    /** First step not done yet, or null when the guide is finished. */
    public static Step next(Kingdom k) {
        for (Step s : steps()) if (!s.isDone(k)) return s;
        return null;
    }

    /** Tiny helper so the switch lookup has one place. */
    static final class BalanceConfigAccess {
        static boolean on(String key) {
            return com.yourname.got.logic.Balance.BalanceConfig.integer(key, 0, 0, 1) == 1;
        }
    }
}
