package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.model.Building.Barrack;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Camp;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.Result;

/**
 * A bot's economy (balance.properties: bots.build.enabled, OFF by default).
 *
 * Once per game day a bot tries to build ONE storage or mine, through exactly the same rules and costs a player
 * has (BuildingManager.construct and GameRulesManager). The first mine and storage of each resource are free in
 * this game, so a bot can bootstrap itself the same way a player does; later ones are only built when affordable.
 * Order of preference (the first thing that is allowed and affordable is done, one per day):
 *   1. the free first storage and mine of every resource (storage before its mine, wood, stone, food, coin)
 *   2. upgrade the castle (camps and the barrack need castle level 3)
 *   3. a camp, then the barrack, when the rules allow them
 *   4. further storages and mines
 */
public final class BotBuilder {
    private static final String[] RESOURCES = {"WOOD", "STONE", "FOOD", "COIN"};

    private BotBuilder() {}

    public static boolean enabled() {
        return BalanceConfig.integer("bots.build.enabled", 0, 0, 1) == 1;
    }

    /** Tries to start one construction. Returns a short description of what was started, or null. */
    public static String buildOne(GameOfThrones game, BotUser bot) {
        Kingdom kingdom = bot.getKingdom();
        if (game == null || kingdom == null || kingdom.getCastle() == null || kingdom.getCastle().isDestroyed()
                || kingdom.getOwner() == null || kingdom.getOwner().getGame() == null) return null;

        GameRulesManager rules = new GameRulesManager(game);
        try {
            // 1. the free first ones
            String r = economy(kingdom, rules, bot, true);
            if (r != null) return r;
            // 2. castle
            if (upgradeCastle(kingdom, rules, bot, game)) return "Castle upgrade";
            // 3. military buildings
            if (rules.canBuild(bot, "Camp", kingdom.countBuildingsOfType("Camp"))
                    && tryBuild(kingdom, new Camp(kingdom), null, kingdom.countBuildingsOfType("Camp"))) return "Camp";
            if (rules.canBuild(bot, "Barrack", kingdom.countBuildingsOfType("Barrack"))
                    && tryBuild(kingdom, new Barrack(kingdom), null, kingdom.countBuildingsOfType("Barrack"))) return "Barrack";
            // 4. more economy
            return economy(kingdom, rules, bot, false);
        } catch (RuntimeException e) {
            GameLog.error("Bot build failed: " + e);
            return null;
        }
    }

    private static String economy(Kingdom kingdom, GameRulesManager rules, BotUser bot, boolean onlyFirst) {
        for (String res : RESOURCES) {
            for (String type : new String[]{"Storage", "Mine"}) {
                boolean mine = "Mine".equals(type);
                int existing = mine ? kingdom.countMinesOfType(res) : kingdom.countStoragesOfType(res);
                if (onlyFirst && existing > 0) continue;
                if (!rules.canBuildSpecificType(bot, type, res)) continue;
                Building b = mine ? new Mine(res, kingdom) : new Storage(res, kingdom);
                if (tryBuild(kingdom, b, res, existing)) return type + " (" + res + ")";
            }
        }
        return null;
    }

    private static boolean upgradeCastle(Kingdom kingdom, GameRulesManager rules, BotUser bot, GameOfThrones game) {
        Building castle = kingdom.getCastle();
        if (castle == null || !castle.isBuilt() || !rules.canUpgradeBuilding(bot, castle).isSuccess()) return false;
        Result r = BuildingManager.upgrade(kingdom, castle, null, kingdom.countBuildingsOfType("Castle"), game.getSpeed());
        return r.isSuccess();
    }

    private static boolean tryBuild(Kingdom kingdom, Building b, String res, int existing) {

        // Do not place anything unless it can be paid: placing and rolling back would only add noise.
        int w = BuildingManager.getConstructionCost(res, 1, existing, "WOOD", b);
        int s = BuildingManager.getConstructionCost(res, 1, existing, "STONE", b);
        int c = BuildingManager.getConstructionCost(res, 1, existing, "COIN", b);
        int f = BuildingManager.getConstructionCost(res, 1, existing, "FOOD", b);
        if (!kingdom.canAffordResources(w, s, c, f)) return false;

        for (int x = 1; x <= 11; x++) {
            for (int y = 1; y <= 11; y++) {
                if (!kingdom.getUserMap().canPlaceBuilding(x, y)) continue;
                if (!kingdom.getUserMap().addBuildingInMap(b, x, y)) continue;
                Result r = BuildingManager.construct(kingdom, b, res, existing);
                if (r.isSuccess()) {
                    kingdom.registerNewBuilding(b);
                    return true;
                }
                kingdom.getUserMap().removeBuilding(x, y);
                return false;
            }
        }
        return false;
    }
}
