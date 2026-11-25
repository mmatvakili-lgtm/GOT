package com.yourname.got.logic.util;

/**
 * One blow of the battle for the replay: who hit, with what, and how hard.
 * kind is one of MELEE, ARCHER, TOWER, WALL_ARCHERS, GARRISON, SHIP, ARMY.
 * (x, y) is the kingdom-local tile that was hit, or -1 when the army itself was hit / the target has no tile.
 */
public class EventStrike implements VisualEvent {
    private static final long serialVersionUID = 1L;
    public final boolean byAttacker;
    public final String kind;
    public final long amount;
    public final int x, y;
    public final long timestamp = System.currentTimeMillis();

    public EventStrike(boolean byAttacker, String kind, long amount, int x, int y) {
        this.byAttacker = byAttacker; this.kind = kind; this.amount = amount; this.x = x; this.y = y;
    }

    @Override public float pauseSeconds() { return 0.35f; }
    @Override public long getTimestamp() { return timestamp; }
}
