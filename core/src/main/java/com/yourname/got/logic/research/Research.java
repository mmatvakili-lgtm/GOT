package com.yourname.got.logic.research;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * The research tree's rules (balance.properties, OFF by default: research.enabled=0).
 *
 *  - A kingdom researches one technology level at a time; up to MAX_QUEUE levels can wait in the queue.
 *    Everything is paid when a level is queued (cancelling refunds it: all of it for a waiting level, half for the
 *    running one).
 *  - A level needs the castle level of its technology and all prerequisites completed.
 *  - Bonuses are percentages that become multipliers. When research is switched off nothing changes in the game:
 *    every multiplier is exactly 1.0 and the techs already learned simply stay dormant.
 */
public final class Research {
    public static final int MAX_QUEUE = 3;

    private Research() {}

    // ---------------------------------------------------------------- settings
    public static boolean enabled() {
        return BalanceConfig.integer("research.enabled", 0, 0, 1) == 1;
    }

    /** Percent of the listed cost that is really paid (default 100). */
    public static int costPercent() {
        return BalanceConfig.integer("research.cost.percent", 100, 10, 500);
    }

    /** Percent of the listed time that really passes (default 100). */
    public static int timePercent() {
        return BalanceConfig.integer("research.time.percent", 100, 10, 1000);
    }

    // ---------------------------------------------------------------- bonuses
    public static int level(Kingdom k, Tech t) {
        return k == null ? 0 : k.getResearch().levelOf(t);
    }

    /** Total bonus in percent of one effect from the learned technologies (0 when research is off). */
    public static int bonusPercent(Kingdom k, Effect effect) {
        if (k == null || !enabled()) return 0;
        int sum = 0;
        ResearchState state = k.getResearch();
        for (Tech t : Tech.values()) {
            if (t.effect == effect) sum += state.levelOf(t) * t.perLevelPercent;
        }
        return sum;
    }

    /** 1.0 = no bonus. Damage-reducing effects (HEALTH, STRUCTURE) are used as "damage / multiplier". */
    public static double multiplier(Kingdom k, Effect effect) {
        return 1.0 + bonusPercent(k, effect) / 100.0;
    }

    // ---------------------------------------------------------------- costs and times
    public static int[] cost(Tech t, int level) {
        int[] c = t.costOfLevel(level);
        int pct = costPercent();
        for (int i = 0; i < 4; i++) c[i] = (int) ((long) c[i] * pct / 100);
        return c;
    }

    /** Real milliseconds a level takes for this kingdom now (game speed, research speed and the time setting). */
    public static long durationMillis(Kingdom k, Tech t, int level, int gameSpeed) {
        double minutes = t.minutesOfLevel(level) * timePercent() / 100.0;
        minutes /= Math.max(1, gameSpeed);
        minutes /= multiplier(k, Effect.RESEARCH_SPEED);
        return Math.max(1000L, (long) (minutes * 60_000));
    }

    // ---------------------------------------------------------------- queue
    /** Levels of this technology that are already queued. */
    public static int queuedLevels(Kingdom k, Tech t) {
        int n = 0;
        for (ResearchState.Task task : k.getResearch().queue) if (task.techId.equals(t.name())) n++;
        return n;
    }

    /** The level the next queued level of this technology would reach. */
    public static int nextLevel(Kingdom k, Tech t) {
        return level(k, t) + queuedLevels(k, t) + 1;
    }

    /** Why the next level of a technology cannot be queued right now, or success. */
    public static Result check(Kingdom k, Tech t) {
        if (!enabled()) return new Result(false, "Research is not enabled in this game.");
        if (k == null || k.getCastle() == null || k.getCastle().isDestroyed()) {
            return new Result(false, "You need a standing castle to research.");
        }
        int next = nextLevel(k, t);
        if (next > t.maxLevel) {
            return new Result(false, t.title + " is already at its highest level" +
                (queuedLevels(k, t) > 0 ? " (including the queued ones)." : "."));
        }
        if (k.getCastle().getLevel() < t.castleLevel) {
            return new Result(false, "Needs castle level " + t.castleLevel + " (yours is " + k.getCastle().getLevel() + ").");
        }
        for (Tech.Requirement r : t.requirements()) {
            if (level(k, r.tech) < r.level) {
                return new Result(false, "Needs " + r.tech.title + " level " + r.level + ".");
            }
        }
        if (k.getResearch().queue.size() >= MAX_QUEUE) {
            return new Result(false, "The research queue is full (" + MAX_QUEUE + ").");
        }
        int[] c = cost(t, next);
        if (!k.canAffordResources(c[0], c[1], c[2], c[3])) {
            return new Result(false, "Not enough resources (needs " + costText(c) + ").");
        }
        return new Result(true, "Ready to research.");
    }

    public static Result enqueue(Kingdom k, Tech t) {
        Result ok = check(k, t);
        if (!ok.isSuccess()) return ok;
        int next = nextLevel(k, t);
        int[] c = cost(t, next);
        k.detuctResources(c[0], c[1], c[2], c[3]);
        k.getResearch().queue.add(new ResearchState.Task(t.name(), next, c, t.minutesOfLevel(next)));
        GameLog.info("RESEARCH: " + t.title + " level " + next + " queued.");
        startNext(k);
        return new Result(true, t.title + " level " + next + " has been added to the research queue.");
    }

    /**
     * Cancels the queued entry at index and every later level of the same technology (they depend on it).
     * A waiting level is refunded completely, the running one by half.
     */
    public static Result cancel(Kingdom k, int index) {
        List<ResearchState.Task> queue = k.getResearch().queue;
        if (index < 0 || index >= queue.size()) return new Result(false, "No such research in the queue.");
        String techId = queue.get(index).techId;
        int firstLevel = queue.get(index).level;
        List<ResearchState.Task> removed = new ArrayList<>();
        for (ResearchState.Task task : new ArrayList<>(queue)) {
            if (task.techId.equals(techId) && task.level >= firstLevel) {
                queue.remove(task);
                removed.add(task);
            }
        }
        for (ResearchState.Task task : removed) {
            int percent = task.started() ? 50 : 100;
            refund(k, task.paid, percent);
        }
        startNext(k);
        return new Result(true, "Research cancelled (" + removed.size() + " level(s) removed, resources refunded).");
    }

    private static void refund(Kingdom k, int[] paid, int percent) {
        String[] types = {"WOOD", "STONE", "COIN", "FOOD"};
        for (int i = 0; i < 4; i++) {
            int amount = (int) ((long) paid[i] * percent / 100);
            if (amount > 0) k.addResources(types[i], amount);
        }
    }

    private static void startNext(Kingdom k) {
        ResearchState state = k.getResearch();
        if (state.queue.isEmpty()) return;
        ResearchState.Task first = state.queue.get(0);
        if (first.started()) return;
        int speed = (k.getOwner() != null && k.getOwner().getGame() != null) ? k.getOwner().getGame().getSpeed() : 1;
        Tech t = Tech.valueOf(first.techId);
        first.requiredMillis = durationMillis(k, t, first.level, speed);
        first.startMillis = System.currentTimeMillis();
    }

    /** Called with every kingdom update: finishes the running research and starts the next one. */
    public static List<String> update(Kingdom k) {
        List<String> messages = new ArrayList<>();
        if (k == null || k.getResearch().queue.isEmpty()) return messages;
        ResearchState state = k.getResearch();
        startNext(k); // e.g. after loading an old game, or when research was switched on later
        while (!state.queue.isEmpty()) {
            ResearchState.Task task = state.queue.get(0);
            if (!task.started() || System.currentTimeMillis() - task.startMillis < task.requiredMillis) break;
            state.queue.remove(0);
            Tech t = Tech.valueOf(task.techId);
            state.levels.put(task.techId, Math.max(task.level, state.levelOf(t)));
            state.completed++;
            String msg = "Research complete: " + t.title + " level " + task.level + " (" + t.effectText() + ").";
            messages.add(msg);
            GameLog.info("RESEARCH: " + msg);
            if (k.getOwner() != null && k.getOwner().getGame() != null) {
                k.getOwner().getGame().sendSystemNotificationToUser(k.getOwner(), "Research complete", msg);
            }
            startNext(k);
        }
        return messages;
    }

    public static long remainingMillis(ResearchState.Task task) {
        if (!task.started()) return task.requiredMillis;
        return Math.max(0, task.requiredMillis - (System.currentTimeMillis() - task.startMillis));
    }

    public static String costText(int[] c) {
        StringBuilder sb = new StringBuilder();
        String[] names = {"Wood", "Stone", "Coin", "Food"};
        for (int i = 0; i < 4; i++) {
            if (c[i] > 0) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(names[i]).append(' ').append(c[i]);
            }
        }
        return sb.length() == 0 ? "free" : sb.toString();
    }

    public static String durationText(long millis) {
        long s = Math.max(0, millis / 1000);
        if (s >= 3600) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        if (s >= 60) return (s / 60) + "m " + (s % 60) + "s";
        return s + "s";
    }
}
