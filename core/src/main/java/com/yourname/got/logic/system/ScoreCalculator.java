package com.yourname.got.logic.system;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.Hero;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Castle;
import com.yourname.got.logic.model.Kingdom;

/**
 * Final score of a kingdom, used to rank the players when a game ends.
 *
 * score = soldiers * 10
 *       + castle level * 200
 *       + sum(level * 50) of every other standing building
 *       + (wood + stone + coin + food) / 50
 *       + 100 if the hero is still alive
 *
 * All weights are constants here so they are easy to rebalance.
 */
public final class ScoreCalculator {
    public static final int POINTS_PER_SOLDIER = 10;
    public static final int POINTS_PER_CASTLE_LEVEL = 200;
    public static final int POINTS_PER_BUILDING_LEVEL = 50;
    public static final int RESOURCES_PER_POINT = 50;
    public static final int HERO_ALIVE_BONUS = 100;

    private ScoreCalculator() {}

    public static int calculate(GameUser user) {
        if (user == null || user.getKingdom() == null) return 0;
        Kingdom k = user.getKingdom();
        long score = 0;

        score += (long) k.getOwnedSoldierCount() * POINTS_PER_SOLDIER;

        Castle castle = k.getCastle();
        if (castle != null && !castle.isDestroyed()) {
            score += (long) castle.getLevel() * POINTS_PER_CASTLE_LEVEL;
        }

        if (k.getBuildings() != null) {
            for (Building b : k.getBuildings()) {
                if (b == null || b instanceof Castle) continue;
                if (b.isBuilt() && !b.isDestroyed()) {
                    score += (long) b.getLevel() * POINTS_PER_BUILDING_LEVEL;
                }
            }
        }

        long resources = (long) k.getTotalResources("Wood") + k.getTotalResources("Stone")
            + k.getTotalResources("Coin") + k.getTotalResources("Food");
        score += resources / RESOURCES_PER_POINT;

        Hero hero = k.getHero();
        if (hero != null && hero.getHealth() > 0) {
            score += HERO_ALIVE_BONUS;
        }
        return (int) Math.min(Integer.MAX_VALUE, score);
    }
}
