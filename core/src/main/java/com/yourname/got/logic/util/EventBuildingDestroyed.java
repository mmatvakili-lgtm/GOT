package com.yourname.got.logic.util;

/** A building fell; loot (if it was a storage) is reported with it (replay: ruin marker + floating loot text). */
public class EventBuildingDestroyed implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public final String name;
    public final int level;
    public final int x;
    public final int y;
    public final String lootType;   // null when nothing was looted
    public final int lootAmount;
    public final long timestamp = System.currentTimeMillis();

    public EventBuildingDestroyed(String name, int level, int x, int y, String lootType, int lootAmount) {
        this.name = name;
        this.level = level;
        this.x = x;
        this.y = y;
        this.lootType = lootType;
        this.lootAmount = lootAmount;
    }

    @Override public long getTimestamp() { return timestamp; }
}
