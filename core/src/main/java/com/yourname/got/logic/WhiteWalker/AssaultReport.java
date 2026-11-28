package com.yourname.got.logic.WhiteWalker;

import java.io.Serializable;

/** What happened when the dead attacked one kingdom (shown to that player right after the battle). */
public class AssaultReport implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int day;
    public final boolean finalWave;
    public final int userId;
    public final String kingdom;
    /** e.g. "312 Wights, 40 White Walkers, 6 Ice Giants and the Night King". */
    public final String enemies;
    public final int enemiesBefore, enemiesAfter;
    public final int defendersBefore, defendersAfter;
    public final int glassUsed;
    /** True when the kingdom's army was destroyed and the kingdom fell. */
    public final boolean overrun;

    public AssaultReport(int day, boolean finalWave, int userId, String kingdom, String enemies, int enemiesBefore,
                         int enemiesAfter, int defendersBefore, int defendersAfter, int glassUsed, boolean overrun) {
        this.day = day; this.finalWave = finalWave; this.userId = userId; this.kingdom = kingdom; this.enemies = enemies;
        this.enemiesBefore = enemiesBefore; this.enemiesAfter = enemiesAfter;
        this.defendersBefore = defendersBefore; this.defendersAfter = defendersAfter;
        this.glassUsed = glassUsed; this.overrun = overrun;
    }

    public int enemiesSlain() { return Math.max(0, enemiesBefore - enemiesAfter); }
    public int defendersLost() { return Math.max(0, defendersBefore - defendersAfter); }
}
