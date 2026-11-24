package com.yourname.got.logic.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Achievements are derived from the leaderboard numbers, so there is nothing extra to store or to get out of sync:
 * whatever the leaderboard says, the achievements follow.
 */
public final class Achievements {
    private Achievements() {}

    public static List<String> unlocked(Leaderboard.Entry e) {
        List<String> list = new ArrayList<>();
        if (e == null) return list;
        if (e.games >= 1) list.add("First Steps (finish a game)");
        if (e.games >= 10) list.add("Veteran (finish 10 games)");
        if (e.wins >= 1) list.add("Victor (win a game)");
        if (e.wins >= 5) list.add("Conqueror (win 5 games)");
        if (e.bestScore >= 1000) list.add("Rising Power (score 1000 in a game)");
        if (e.bestScore >= 5000) list.add("Legend (score 5000 in a game)");
        return list;
    }

    /** Entry of one player (case-insensitive), or null if he never finished a game. */
    public static Leaderboard.Entry find(String name) {
        if (name == null) return null;
        for (Leaderboard.Entry e : Leaderboard.top(Integer.MAX_VALUE)) {
            if (e.name.equalsIgnoreCase(name)) return e;
        }
        return null;
    }
}
