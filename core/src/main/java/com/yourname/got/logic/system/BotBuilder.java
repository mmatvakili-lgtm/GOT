package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.model.Building.Building;
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
 * Order: storage before its mine, wood, stone, food, coin; then the same list again for the next ones.
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
        for (String res : RESOURCES) {
            for (String type : new String[]{"Storage", "Mine"}) {
                try {
                    if (!rules.canBuildSpecificType(bot, type, res)) continue;
                    if (tryBuild(kingdom, type, res)) return type + " (" + res + ")";
                } catch (RuntimeException e) {
                    GameLog.error("Bot build check failed for " + type + " " + res + ": " + e);
                }
            }
        }
        return null;
    }

    private static boolean tryBuild(Kingdom kingdom, String type, String res) {
        boolean mine = "Mine".equals(type);
        int existing = mine ? kingdom.countMinesOfType(res) : kingdom.countStoragesOfType(res);
        Building b = mine ? new Mine(res, kingdom) : new Storage(res, kingdom);

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
