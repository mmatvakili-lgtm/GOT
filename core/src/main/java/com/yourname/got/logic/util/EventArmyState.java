package com.yourname.got.logic.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The state of both armies at one moment of the battle: every unit with its soldier count and total health.
 * Attackers are listed in formation order (front row first). The replay army panel and the soldier dots are
 * drawn from it.
 */
public class EventArmyState implements VisualEvent {
    private static final long serialVersionUID = 1L;

    public static final class Unit implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String type;
        public final int count;
        public final long health;
        public Unit(String type, int count, long health) {
            this.type = type;
            this.count = count;
            this.health = health;
        }
    }

    public final String label;
    public final List<Unit> attackers = new ArrayList<>();
    public final List<Unit> defenders = new ArrayList<>();
    /** Name of the hero leading the attackers, or null. */
    public String heroName;
    public int heroHealth;
    /** The gate / wall being assaulted (name null when none), with its remaining health. */
    public String structureName;
    public long structureHealth;
    public final long timestamp = System.currentTimeMillis();

    public EventArmyState(String label) { this.label = label; }

    @Override public long getTimestamp() { return timestamp; }
}
