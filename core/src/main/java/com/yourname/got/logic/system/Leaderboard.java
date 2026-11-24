package com.yourname.got.logic.system;

import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.SafeFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All-time leaderboard of the real players (bots never appear), kept in leaderboard.txt
 * as one line per player: name TAB games TAB wins TAB bestScore TAB totalScore.
 * Written atomically; a damaged line is skipped instead of breaking the board.
 */
public final class Leaderboard {
    private static final String FILE = "leaderboard.txt";

    private Leaderboard() {}

    public static final class Entry {
        public final String name;
        public int games, wins, bestScore;
        public long totalScore;

        Entry(String name) { this.name = name; }
    }

    /** Adds one finished game of one player. */
    public static synchronized void record(String name, int score, boolean won) {
        if (name == null || name.trim().isEmpty() || name.indexOf('\t') >= 0 || name.indexOf('\n') >= 0) return;
        Map<String, Entry> all = load();
        Entry e = all.computeIfAbsent(name.toLowerCase(), k -> new Entry(name));
        e.games++;
        if (won) e.wins++;
        e.bestScore = Math.max(e.bestScore, Math.max(0, score));
        e.totalScore += Math.max(0, score);
        save(all);
    }

    /** Best players first: wins, then best score, then name. */
    public static synchronized List<Entry> top(int limit) {
        List<Entry> list = new ArrayList<>(load().values());
        list.sort((a, b) -> {
            if (a.wins != b.wins) return Integer.compare(b.wins, a.wins);
            if (a.bestScore != b.bestScore) return Integer.compare(b.bestScore, a.bestScore);
            return a.name.compareToIgnoreCase(b.name);
        });
        return limit < list.size() ? new ArrayList<>(list.subList(0, Math.max(0, limit))) : list;
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> all = new LinkedHashMap<>();
        File f = new File(FILE);
        if (!f.exists()) return all;
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length != 5) continue;
                try {
                    Entry e = new Entry(p[0]);
                    e.games = Integer.parseInt(p[1]);
                    e.wins = Integer.parseInt(p[2]);
                    e.bestScore = Integer.parseInt(p[3]);
                    e.totalScore = Long.parseLong(p[4]);
                    all.put(p[0].toLowerCase(), e);
                } catch (NumberFormatException ignored) { /* skip a damaged line */ }
            }
        } catch (IOException e) {
            GameLog.error("Could not read the leaderboard: " + e);
        }
        return all;
    }

    private static void save(Map<String, Entry> all) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : all.values()) {
            sb.append(e.name).append('\t').append(e.games).append('\t').append(e.wins).append('\t')
              .append(e.bestScore).append('\t').append(e.totalScore).append('\n');
        }
        File target = new File(FILE);
        File tmp = new File(FILE + ".tmp");
        try {
            Files.writeString(tmp.toPath(), sb.toString(), StandardCharsets.UTF_8);
            SafeFiles.replace(tmp, target);
        } catch (IOException e) {
            GameLog.error("Could not save the leaderboard: " + e);
        }
    }
}
