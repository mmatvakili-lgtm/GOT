package com.yourname.got.logic.system;

import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.enums.Seasons;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Building.Castle;
import com.yourname.got.logic.model.Building.Mine;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Position;
import com.yourname.got.logic.model.UserSubMap;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.mediator.GameMessageListener;

import java.io.Serializable;

public class GameRulesManager implements Serializable {
    private static final long serialVersionUID = 5580752269032371837L;

    private final GameOfThrones game;
    private GameMediator uiMediator;
    private GameMessageListener messageListener;
    public GameRulesManager(GameOfThrones game) {

        this.game = game;
    }

    public void setUiMediator(GameMediator uiMediator) {
        this.uiMediator = uiMediator;
        this.messageListener = uiMediator.getMessageListener();
    }

    private void sendMessage(String msg) {
        if (uiMediator != null) {
            messageListener.onMessage(msg);
        } else {
            System.out.println("[UI Missing] " + msg);
        }
    }

    public Seasons getCurrentSeason() {
        return game.getGameTime().getSeason();
    }

    public boolean isWinterSeason() {
        return getCurrentSeason() == Seasons.WINTER;
    }

    public boolean isSpringSeason() {
        return getCurrentSeason() == Seasons.SPRING;
    }


    public Result canAttack(GameUser attacker, GameUser defender, AttackType attackType) {
        if (isSpringSeason() || isWinterSeason()) {
            return new Result(false, "Attacks are not allowed in the " + getCurrentSeason() + " season.");
        }
        GameOfThrones.PlayerState defenderState = game.getPlayerState(defender.getId());
        if (defenderState != null && defenderState.status == GameOfThrones.PlayerStatus.DEFEATED) {
            // اگر شکست خورده است، اجازه حمله نده
            return new Result(false, "This player has already been defeated and cannot be attacked.");
        }
        if (attacker == null || defender == null) {
            return new Result(false, "Internal Error: Player is not valid.");
        }
        if (!attacker.isActive() || !defender.isActive()) {
            return new Result(false, "One of the players is inactive.");
        }
        if (attacker.isInFight() || defender.isInFight()) {
            return new Result(false, "One of the players is already in combat.");
        }

        if (attackType == AttackType.GROUND) {
            if (attacker.getKingdom().getType() == KingdomType.IRON_ISLANDS) {
                return new Result(false, "Iron Islands cannot perform Ground Attacks.");
            }
        } else if (attackType == AttackType.SEA) {
            if (!attacker.getKingdom().hasPort()) {
                return new Result(false, "You need a Port to launch Sea Attacks.");
            }
            if (attacker.getKingdom().getBattleShip() == null) {
                return new Result(false, "You need to rent or own a ship for Sea Attacks.");
            }
        }

        return new Result(true, "This target is attackable.");
    }
    public int getRequiredCastleLevelForBuildingType(int buildingType) {
        return switch (buildingType) {
            case 2 -> 2;
            case 3 -> 1;
            case 4 -> 1;
            case 5 -> 1;
            case 6 -> 3;
            case 7 -> 3;
            case 8 -> 4;
            default -> Integer.MAX_VALUE;
        };
    }

    public boolean canBuild(GameUser user, String buildingType, int currentCount) {
        int castleLevel = user.getKingdom().getCastle().getLevel();
        int allowedCount = getAllowedBuildingCount(buildingType, castleLevel);
        return currentCount < allowedCount;
    }

    private int getAllowedBuildingCount(String buildingType, int castleLevel) {
        return switch (buildingType) {
            case "Mine" -> getAllowedMineCount(castleLevel);
            case "Storage" -> getAllowedStorageCount(castleLevel);
            case "Gate" -> (castleLevel >= 1) ? 1 : 0;
            case "Defensive Tower" -> (castleLevel >= 2) ? castleLevel - 1 : 0;
            case "Barrack" -> (castleLevel >= 3) ? 1 : 0;
            case "Camp" -> (castleLevel >= 3) ? castleLevel - 2 : 0;
            case "CrowTower" -> (castleLevel >= 3) ? 1 : 0;
            case "Transport" -> (castleLevel >= 4) ? 1 : 0;
            default -> 0;
        };
    }

    private int getAllowedMineCount(int castleLevel) {
        return 4 + (castleLevel - 1);
    }

    private int getAllowedStorageCount(int castleLevel) {
        return 4 + (castleLevel - 1);
    }

    // ───────────────────────── بررسی نوع منبع برای Mine و Storage
    public boolean canBuildSpecificType(GameUser user, String buildingType, String resourceType) {
        Kingdom kingdom = user.getKingdom();
        int castleLevel = kingdom.getCastle().getLevel();

        int totalCount = 0;
        int sameTypeCount = 0;
        int woodCount = 0, stoneCount = 0, foodCount = 0, coinCount = 0;

        if (buildingType.equalsIgnoreCase("Mine")) {
            for (Building b : kingdom.getMines()) {
                if (b.isActiveBuilding()|| b.isFreezeBuilding()) totalCount++;
            }
            if (totalCount >= getAllowedMineCount(castleLevel)) return false;

            woodCount = kingdom.countMinesOfType("WOOD");
            stoneCount = kingdom.countMinesOfType("STONE");
            foodCount = kingdom.countMinesOfType("FOOD");
            coinCount = kingdom.countMinesOfType("COIN");

            sameTypeCount = kingdom.countMinesOfType(resourceType);
        }

        else if (buildingType.equalsIgnoreCase("Storage")) {
            for (Building b : kingdom.getStorages()) {
                if (b.isActiveBuilding()|| b.isFreezeBuilding()) totalCount++;
            }
            if (totalCount >= getAllowedStorageCount(castleLevel)) return false;

            woodCount = kingdom.countStoragesOfType("WOOD");
            stoneCount = kingdom.countStoragesOfType("STONE");
            foodCount = kingdom.countStoragesOfType("FOOD");
            coinCount = kingdom.countStoragesOfType("COIN");

            sameTypeCount = kingdom.countStoragesOfType(resourceType);
        }

        else {
            int currentCount = 0;
            for (Building b : kingdom.getBuildings()) {
                if (b.getClass().getSimpleName().equalsIgnoreCase(buildingType)) {
                    currentCount++;
                }
            }
            return currentCount < getAllowedBuildingCount(buildingType, castleLevel);
        }

        if (sameTypeCount >= 2) return false;

        int repeatedTypes = 0;
        if (woodCount > 1 || (resourceType.equals("WOOD") && woodCount == 1)) repeatedTypes++;
        if (stoneCount > 1 || (resourceType.equals("STONE") && stoneCount == 1)) repeatedTypes++;
        if (foodCount > 1 || (resourceType.equals("FOOD") && foodCount == 1)) repeatedTypes++;
        if (coinCount > 1 || (resourceType.equals("COIN") && coinCount == 1)) repeatedTypes++;

        int maxRepeatedTypes = castleLevel - 1;
        return repeatedTypes <= maxRepeatedTypes;
    }

    public Result canUpgradeBuilding(GameUser user, Building buildingToUpgrade) {
        Castle castle = user.getKingdom().getCastle();
        if (castle == null) {
            return new Result(false, "Internal error: Central castle not found");
        }
        if (user.isInFight()) {
            return new Result(false, "Buildings cannot be upgraded in war mode");
        }

        // --- تغییرات جدید برای محدودیت لول قلعه ---
        if (buildingToUpgrade instanceof Castle) {
            if (buildingToUpgrade.getLevel() >= 5) {
                return new Result(false, "Castle has reached its maximum level (5).");
            }
            return new Result(true, "The castle is ready for upgrade");
        }
        // -------------------------------------------

        if (buildingToUpgrade.getLevel() >= castle.getLevel()) {
            return new Result(false, "The building level cannot be higher than the castle level (" + castle.getLevel() + ") to increase");
        }
        return new Result(true, "Ready for upgrade");
    }

    public boolean isInWar(GameUser user) {
        return user.isInFight();
    }

    public boolean canPerformNonWarAction(GameUser user) {
        if (isInWar(user)) {
            sendMessage("Error: This activity cannot be performed in a state of war");
            return false;
        }
        return true;
    }

    public boolean isUserActive(GameUser user) {
        long elapsedMillis = System.currentTimeMillis() - user.getLastVisit();
        int inactiveDays = (int) ((elapsedMillis / 1000) / (24 * 60 * 60)) + 1;
        return inactiveDays < 25;
    }

    public void checkInactivePlayers() {
        game.checkUserActivity();
    }

    public boolean isGameOver() {
        return game.getWall() != null && game.getWall().isDestroyed();
    }

    public void printAvailableBuildingTypes(GameUser user) {
        Kingdom kingdom = user.getKingdom();
        int castleLevel = kingdom.getCastle().getLevel();

        sendMessage("\n🏰 وضعیت ساخت‌وساز در سطح قلعه " + castleLevel + ":");

        String[] buildingTypes = {
            "Mine", "Storage", "DefensiveTower", "Gate", "Barrack", "Camp", "CrowTower", "Transport"
        };

        for (String type : buildingTypes) {
            int totalCount = 0;
            int allowedCount = getAllowedBuildingCount(type, castleLevel);

            for (Building b : kingdom.getBuildings()) {
                if (b.getClass().getSimpleName().equalsIgnoreCase(type)) {
                    totalCount++;
                }
            }

            sendMessage("\n🔧 " + type + ": " + totalCount + "/" + allowedCount);

            if (type.equals("Mine") || type.equals("Storage")) {
                String[] resourceTypes = {"WOOD", "STONE", "FOOD", "COIN"};
                int[] counts = new int[4];

                for (Building b : kingdom.getBuildings()) {
                    if (b.getClass().getSimpleName().equalsIgnoreCase(type)) {
                        String res = (b instanceof Mine m) ? m.getResourceType().toUpperCase()
                            : (b instanceof Storage s) ? s.getResourceType().toUpperCase()
                            : "";
                        switch (res) {
                            case "WOOD" -> counts[0]++;
                            case "STONE" -> counts[1]++;
                            case "FOOD" -> counts[2]++;
                            case "COIN" -> counts[3]++;
                        }
                    }
                }

                int maxTypesAllowed = castleLevel - 1;
                int overOneTypes = 0;
                for (int c : counts) if (c >= 2) overOneTypes++;

                StringBuilder sb = new StringBuilder("✅ منابع قابل ساخت: ");
                for (int i = 0; i < 4; i++) {
                    int count = counts[i];
                    if (count >= 2) continue;

                    int otherOverTwo = overOneTypes;
                    if (count >= 2) otherOverTwo--;

                    if (count == 0 || (count == 1 && otherOverTwo < maxTypesAllowed)) {
                        if (totalCount < allowedCount)
                            sb.append(resourceTypes[i]).append(" ");
                    }
                }
                sendMessage(sb.toString());
            } else {
                if (totalCount < allowedCount)
                    sendMessage("✅ قابل ساخت: بله");
                else
                    sendMessage("⛔ قابل ساخت: خیر");
            }
        }
    }

    public void printUpgradeableBuildings(GameUser user) {
        Kingdom kingdom = user.getKingdom();
        int castleLevel = kingdom.getCastle().getLevel();

        sendMessage("\n⬆️ ساختمان‌های قابل ارتقاء در سطح قلعه " + castleLevel + ":");

        boolean found = false;
        for (Building b : kingdom.getBuildings()) {
            if (b instanceof Castle) continue;
            if (b.getStatus() == Status.upgrading) continue;
            if (b.getLevel() >= castleLevel) continue;

            sendMessage("- " + b.getName() + " (سطح " + b.getLevel() +
                ") در موقعیت " + b.getPosition().getX() + "," + b.getPosition().getY());
            found = true;
        }

        if (!found) {
            sendMessage("⛔ هیچ ساختمانی برای ارتقاء وجود ندارد.");
        }
    }

    public void printRepairableBuildings(GameUser user) {
        Kingdom kingdom = user.getKingdom();

        sendMessage("\n🛠️ ساختمان‌های قابل تعمیر:");

        boolean found = false;
        for (Building b : kingdom.getBuildings()) {
            if (!b.isDestroyed()) continue;
            if (b.getStatus() == Status.underRepair) continue;

            sendMessage("- " + b.getName() + " در موقعیت " +
                b.getPosition().getX() + "," + b.getPosition().getY());
            found = true;
        }

        if (!found) {
            sendMessage("⛔ هیچ ساختمانی برای تعمیر وجود ندارد.");
        }
    }

    public void printDestroyableBuildings(GameUser user) {
        Kingdom kingdom = user.getKingdom();
        UserSubMap userSubMap = kingdom.getUserMap();

        sendMessage("\n💣 ساختمان‌های قابل تخریب:");

        boolean found = false;
        for (Building b : kingdom.getBuildings()) {
            if (b instanceof Castle) continue;
            if (b.isDestroyed()) continue;

            Position localPos = userSubMap.globalToLocal(b.getPosition());
            String coords = localPos != null
                ? "(" + (localPos.getX() + 1) + "," + (localPos.getY() + 1) + ")"
                : "❌ خارج از محدوده";

            sendMessage("- " + b.getName() + " در موقعیت " + coords);
            found = true;
        }

        if (!found) {
            sendMessage("⛔ هیچ ساختمانی برای تخریب وجود ندارد.");
        }
    }
}
