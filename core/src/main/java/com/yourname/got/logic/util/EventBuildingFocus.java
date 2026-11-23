package com.yourname.got.logic.util;

/** The attackers have picked this building as their next target (replay: highlight it). */
public class EventBuildingFocus implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public final String name;
    public final int level;
    public final int x;
    public final int y;
    public final long timestamp = System.currentTimeMillis();

    public EventBuildingFocus(String name, int level, int x, int y) {
        this.name = name;
        this.level = level;
        this.x = x;
        this.y = y;
    }

    @Override public long getTimestamp() { return timestamp; }
}
