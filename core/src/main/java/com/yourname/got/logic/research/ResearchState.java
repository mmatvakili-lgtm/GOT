package com.yourname.got.logic.research;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The research of one kingdom: the level of every technology and the queue (first entry is the active one). */
public class ResearchState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final class Task implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String techId;
        public final int level; // the level this task will reach
        public final int[] paid; // wood, stone, coin, food - for refunds
        public final int minutes; // game minutes (before speed), kept so the bonus at the time of the start applies
        public long startMillis = 0; // 0 = waiting in the queue
        public long requiredMillis = 0;

        Task(String techId, int level, int[] paid, int minutes) {
            this.techId = techId;
            this.level = level;
            this.paid = paid;
            this.minutes = minutes;
        }

        public boolean started() { return startMillis != 0; }
    }

    final Map<String, Integer> levels = new HashMap<>();
    final List<Task> queue = new ArrayList<>();
    /** How many technology levels have been completed in total (shown in the screen). */
    int completed = 0;

    public int levelOf(Tech t) {
        Integer l = levels.get(t.name());
        return l == null ? 0 : l;
    }

    public List<Task> queue() { return java.util.Collections.unmodifiableList(queue); }

    public int completedLevels() { return completed; }
}
