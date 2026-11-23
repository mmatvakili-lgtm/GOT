package com.yourname.got.logic.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The defender's kingdom as it was when the attackers broke in: every building with its local tile coordinates.
 * The battle replay uses it to draw a small map of the battlefield.
 * Coordinates are the local ones of the kingdom's 13x13 area: x = row, y = column (same as the main map).
 */
public class EventBattleSetup implements BattleEvent {
    private static final long serialVersionUID = 1L;

    public static final class Piece implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String name;
        public final int level;
        public final int x;
        public final int y;

        public Piece(String name, int level, int x, int y) {
            this.name = name;
            this.level = level;
            this.x = x;
            this.y = y;
        }
    }

    public final List<Piece> pieces = new ArrayList<>();
    public final long timestamp = System.currentTimeMillis();

    @Override public long getTimestamp() { return timestamp; }
}
