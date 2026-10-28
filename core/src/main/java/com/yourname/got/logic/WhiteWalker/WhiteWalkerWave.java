package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Army.Soldier;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;

import java.io.Serializable;

public class WhiteWalkerWave implements Serializable {
    private static final long serialVersionUID = 1L;
    private GameOfThrones gameOfThrones;
    private final int day;
    private final int attackInterval;
    private final boolean isFinal;

    public WhiteWalkerWave(GameOfThrones game, int day, int attackInterval, boolean isFinal) {
        this.day = day;
        this.attackInterval = Math.max(1, attackInterval);
        this.isFinal = isFinal;
        this.gameOfThrones = game;
    }

    public SoldierUnit[] generateArmy() {
        int waveIndex = Math.max(0, (day / attackInterval));
        int baseCount = 20 + waveIndex * 8;
        String difficulty = gameOfThrones.getDifficulty();
        int difficultyMultiplier;

        switch (difficulty.toLowerCase()) {
            case "easy" -> difficultyMultiplier = 1;
            case "medium" -> difficultyMultiplier = 2;
            case "hard" -> difficultyMultiplier = 3;
            default -> difficultyMultiplier = 1; // fallback
        }

        int atk = 20 + waveIndex * 6 * difficultyMultiplier ;
        int hp  = 40 + waveIndex * 10 * difficultyMultiplier ;

        if (isFinal) {
            baseCount += 60;
            atk += 40;
            hp  += 80;
        }

        SoldierUnit unit = new SoldierUnit(SoldierType.WHITEWALKER);
        for (int i = 0; i < (int)baseCount/10; i++) {
            WhiteWalkerSoldier s = new WhiteWalkerSoldier(atk/10, hp/10);
            unit.addSoldier((Soldier) s);
        }
        return new SoldierUnit[]{ unit };
    }
}
