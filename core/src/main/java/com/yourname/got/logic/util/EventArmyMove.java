package com.yourname.got.logic.util;

/**
 * The attacking army walks to a tile of the defender's kingdom (kingdom-local coordinates, x = row, y = column).
 * fromX < 0 means the army has not been drawn yet and appears at the destination's side.
 */
public class EventArmyMove implements VisualEvent {
    private static final long serialVersionUID = 1L;
    public final int fromX, fromY, toX, toY;
    public final String note;
    public final long timestamp = System.currentTimeMillis();

    public EventArmyMove(int fromX, int fromY, int toX, int toY, String note) {
        this.fromX = fromX; this.fromY = fromY; this.toX = toX; this.toY = toY; this.note = note;
    }

    @Override public float pauseSeconds() { return 0.8f; }
    @Override public long getTimestamp() { return timestamp; }
}
