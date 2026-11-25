package com.yourname.got.logic.research;

/**
 * The technologies. Every technology has up to maxLevel levels; each level adds perLevelPercent to its effect.
 * A level costs base cost x level and takes baseMinutes x level (game minutes: the game speed shortens them).
 * Prerequisites are written "ID:level" and must be fully researched before the technology can be started.
 */
public enum Tech {
    // ----- Economy -----
    TOOLS(Branch.ECONOMY, "Better Tools", "Sturdier picks and axes make every mine work harder.",
        Effect.PRODUCTION, 5, 3, 1, 1, new int[]{200, 100, 50, 0}, 20),
    GRANARIES(Branch.ECONOMY, "Granaries", "Bigger, drier stores hold more of everything.",
        Effect.STORAGE, 10, 3, 2, 2, new int[]{300, 250, 100, 0}, 25, "TOOLS:1"),
    ARCHIVES(Branch.ECONOMY, "Maesters' Archive", "Scholars copy and organise knowledge, so research goes faster.",
        Effect.RESEARCH_SPEED, 10, 3, 2, 2, new int[]{250, 200, 200, 0}, 25, "TOOLS:1"),
    TRADE(Branch.ECONOMY, "Trade Guild", "A guild of merchants squeezes more out of every mine.",
        Effect.PRODUCTION, 5, 3, 3, 3, new int[]{500, 400, 400, 100}, 40, "GRANARIES:2", "ARCHIVES:1"),

    // ----- Military -----
    DRILL(Branch.MILITARY, "Combat Drill", "Daily drilling sharpens every soldier's blows.",
        Effect.ATTACK, 5, 3, 1, 1, new int[]{300, 100, 100, 100}, 20),
    FORGE(Branch.MILITARY, "Forge", "Better steel for armour: soldiers survive more punishment.",
        Effect.HEALTH, 5, 3, 2, 2, new int[]{400, 300, 150, 0}, 25, "DRILL:1"),
    RECRUITS(Branch.MILITARY, "Quick Recruits", "Veteran sergeants train new soldiers faster.",
        Effect.TRAIN_SPEED, 10, 3, 2, 2, new int[]{300, 150, 200, 150}, 25, "DRILL:1"),
    HORSES(Branch.MILITARY, "War Horses", "Fast mounts and supply wagons: armies reach their target sooner.",
        Effect.MARCH_SPEED, 10, 3, 3, 3, new int[]{400, 200, 300, 300}, 35, "RECRUITS:1"),
    SPOILS(Branch.MILITARY, "Spoils of War", "Organised looting carries away more from raided storages.",
        Effect.LOOT, 10, 3, 4, 4, new int[]{500, 400, 400, 200}, 45, "FORGE:1", "HORSES:1"),
    TACTICS(Branch.MILITARY, "Grand Tactics", "Commanders coordinate the whole army for a mighty strike.",
        Effect.ATTACK, 5, 2, 4, 4, new int[]{800, 600, 600, 300}, 60, "DRILL:3", "FORGE:2"),

    // ----- Defense -----
    MASONRY(Branch.DEFENSE, "Masonry", "Thicker stone makes every building harder to break.",
        Effect.STRUCTURE, 8, 3, 1, 1, new int[]{200, 300, 50, 0}, 20),
    SLITS(Branch.DEFENSE, "Arrow Slits", "Archers in the towers shoot with better aim.",
        Effect.TOWER, 10, 3, 2, 2, new int[]{300, 350, 100, 0}, 25, "MASONRY:1"),
    GATES(Branch.DEFENSE, "Iron Gates", "Iron-bound doors and walls resist rams for longer.",
        Effect.STRUCTURE, 8, 3, 3, 3, new int[]{400, 500, 200, 0}, 35, "MASONRY:2"),
    WATCH(Branch.DEFENSE, "Watchtowers", "Higher towers see the enemy sooner and strike harder.",
        Effect.TOWER, 10, 2, 3, 3, new int[]{500, 600, 300, 100}, 40, "SLITS:2"),
    BULWARK(Branch.DEFENSE, "Bulwark", "A well-fed, well-armed garrison holds the line.",
        Effect.HEALTH, 5, 2, 4, 4, new int[]{700, 700, 500, 300}, 55, "MASONRY:2", "FORGE:1");

    public final Branch branch;
    public final String title, description;
    public final Effect effect;
    public final int perLevelPercent, maxLevel, tier, castleLevel;
    private final int[] cost; // wood, stone, coin, food (for level 1)
    private final int minutes; // for level 1
    private final String[] prerequisites;

    Tech(Branch branch, String title, String description, Effect effect, int perLevelPercent, int maxLevel,
         int tier, int castleLevel, int[] cost, int minutes, String... prerequisites) {
        this.branch = branch;
        this.title = title;
        this.description = description;
        this.effect = effect;
        this.perLevelPercent = perLevelPercent;
        this.maxLevel = maxLevel;
        this.tier = tier;
        this.castleLevel = castleLevel;
        this.cost = cost;
        this.minutes = minutes;
        this.prerequisites = prerequisites;
    }

    /** Resource cost {wood, stone, coin, food} of researching the given level (1-based). */
    public int[] costOfLevel(int level) {
        int[] c = new int[4];
        for (int i = 0; i < 4; i++) c[i] = cost[i] * Math.max(1, level);
        return c;
    }

    /** Game minutes the given level takes (before game speed and research bonuses). */
    public int minutesOfLevel(int level) {
        return minutes * Math.max(1, level);
    }

    public static final class Requirement {
        public final Tech tech;
        public final int level;
        Requirement(Tech tech, int level) { this.tech = tech; this.level = level; }
    }

    public java.util.List<Requirement> requirements() {
        java.util.List<Requirement> list = new java.util.ArrayList<>();
        for (String p : prerequisites) {
            String[] parts = p.split(":");
            list.add(new Requirement(Tech.valueOf(parts[0]), Integer.parseInt(parts[1])));
        }
        return list;
    }

    /** Short effect text for one level, e.g. "+5% Mine production". */
    public String effectText() {
        return "+" + perLevelPercent + "% " + effect.label + " per level";
    }
}
