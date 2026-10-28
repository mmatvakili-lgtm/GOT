package com.yourname.got.logic.Balance;

import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Castle;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Position;
import com.yourname.got.logic.model.UserSubMap;
import com.yourname.got.logic.system.GameRulesManager;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

import java.io.Serializable;


public final class BuildingManager implements Serializable {

    private BuildingManager() {}

    public static class ConstructionOption {
        public final String displayName;
        public final String buildingType;
        public final String resourceType;
        public final String costString;
        public final String statusMessage;
        public final boolean isActionable;

        public ConstructionOption(String displayName, String buildingType, String resourceType, String costString, String statusMessage, boolean isActionable) {
            this.displayName = displayName;
            this.buildingType = buildingType;
            this.resourceType = resourceType;
            this.costString = costString;
            this.statusMessage = statusMessage;
            this.isActionable = isActionable;
        }
    }


    public static class UpgradeOption {
        public final String name;         // نام نمایشی: Mine (WOOD)
        public final String resourceType; // نوع منبع، ممکن است null باشد
        public final int currentLevel;   // سطح فعلی
        public final int nextLevel;      // سطح بعدی
        public final int time;           // زمان مورد نیاز (دقیقه)
        public final String costString;   // رشته هزینه: "W:100 S:50 C:20"
        public final String coords;       // مختصات محلی: "(x,y)"

        // --- فیلدهای کلیدی جدید ---
        public final String statusMessage; // پیام دقیق برای نمایش به کاربر
        public final boolean isActionable; // مشخص می‌کند که آیا دکمه ارتقاء باید فعال باشد یا خیر

        // فیلدهای قبلی برای سازگاری یا منطق داخلی حفظ شده‌اند
        public final boolean permitted;
        public final boolean affordable;

        public UpgradeOption(String name, String resourceType, int currentLevel, int nextLevel,
                             int time, String costString, boolean permitted, boolean affordable,
                             String coords, String statusMessage, boolean isActionable) {
            this.name = name;
            this.resourceType = resourceType;
            this.currentLevel = currentLevel;
            this.nextLevel = nextLevel;
            this.time = time;
            this.costString = costString;
            this.permitted = permitted;
            this.affordable = affordable;
            this.coords = coords;
            this.statusMessage = statusMessage;
            this.isActionable = isActionable;
        }
    }

    public static class RepairOption {
        public final String name;
        public final int level;
        public final int time;
        public final String costString;
        public final String coords;
        public final String statusMessage;
        public final boolean isActionable;

        public RepairOption(String name, int level, int time, String costString, String coords,
                            String statusMessage, boolean isActionable) {
            this.name = name;
            this.level = level;
            this.time = time;
            this.costString = costString;
            this.coords = coords;
            this.statusMessage = statusMessage;
            this.isActionable = isActionable;
        }
    }

    public static Result construct(Kingdom kingdom, Building building, String resourceType, int existingCount) {
        int level = 1; // ساخت همیشه از سطح 1
        int wood  = getConstructionCost(resourceType, level, existingCount, "WOOD", building);
        int stone = getConstructionCost(resourceType, level, existingCount, "STONE", building);
        int coin  = getConstructionCost(resourceType, level, existingCount, "COIN", building);
        int food  = getConstructionCost(resourceType, level, existingCount, "FOOD", building);

        if (!kingdom.canAffordResources(wood, stone, coin, food)) {
            return new Result(false, "Not enough resources to build " + building.getName() + ".");
        }

        kingdom.detuctResources(wood, stone, coin, food);
        building.startConstruction(kingdom.getOwner().getGame().getSpeed());
        kingdom.addHistory("Construct", building);

        return new Result(true, "Construction of " + building.getName() + " has started.");
    }

    public static Result upgrade(Kingdom kingdom, Building building, String resourceType, int existingCount, int gameSpeed) {
        if (!building.isBuilt()) return new Result(false, "Building is not constructed yet.");
        if (building.getStatus() == Status.upgrading) return new Result(false, "Building is already being upgraded.");

        int nextLevel = building.getLevel() + 1;
        int wood  = getUpgradeCost(resourceType, nextLevel, existingCount, "WOOD", building);
        int stone = getUpgradeCost(resourceType, nextLevel, existingCount, "STONE", building);
        int coin  = getUpgradeCost(resourceType, nextLevel, existingCount, "COIN", building);

        if (!kingdom.canAffordResources(wood, stone, coin, 0)) {
            return new Result(false, "Not enough resources to upgrade " + building.getName() + ".");
        }

        kingdom.detuctResources(wood, stone, coin, 0);
        building.upgrade(gameSpeed);
        kingdom.addHistory("Upgrade", building);
        return new Result(true, "Upgrade for " + building.getName() + " has started.");
    }

    public static Result repair(Kingdom kingdom, Building building, String resourceType, int existingCount, int gameSpeed) {
        if (!building.isDestroyed()) return new Result(false, "This building is not destroyed.");
        if (building.getStatus() == Status.underRepair) return new Result(false, "Building is already under repair.");

        int level = building.getLevel();
        int wood  = getRepairCost(resourceType, level, existingCount, "WOOD", building);
        int stone = getRepairCost(resourceType, level, existingCount, "STONE", building);
        int coin  = getRepairCost(resourceType, level, existingCount, "COIN", building);

        if (!kingdom.canAffordResources(wood, stone, coin, 0)) {
            return new Result(false, "Not enough resources to repair " + building.getName() + ".");
        }

        kingdom.detuctResources(wood, stone, coin, 0);
        building.repair(gameSpeed);
        kingdom.addHistory("Repair", building);
        return new Result(true, "Repair of " + building.getName() + " has started.");
    }

    public static Result destroy(Kingdom kingdom, Building building) {
        if (building instanceof Castle) return new Result(false, "The Castle cannot be destroyed.");
        if (building.isDestroyed()) return new Result(false, "Building is already destroyed.");

        building.destroy();
        kingdom.addHistory("Destroy", building);
        return new Result(true, building.getName() + " has been destroyed.");
    }


    public static MyLinkedList<ConstructionOption> getConstructionOptions(GameUser user) {
        MyLinkedList<ConstructionOption> options = new MyLinkedList<>();
        Kingdom kingdom = user.getKingdom();
        GameRulesManager rules = new GameRulesManager(user.getGame());

        String[] buildingTypes = {"Mine", "Storage", "Barrack", "Camp", "CrowTower", "Transport", "Defensive Tower"};

        for (String type : buildingTypes) {
            if ("Mine".equals(type) || "Storage".equals(type)) {
                String[] resTypes = {"WOOD", "STONE", "FOOD", "COIN"};
                for (String res : resTypes) {
                    int count = "Mine".equals(type) ? kingdom.countMinesOfType(res) : kingdom.countStoragesOfType(res);
                    Building example = "Mine".equals(type) ? new Mine(res, kingdom) : new Storage(res, kingdom);

                    int w = getConstructionCost(res, 1, count, "WOOD", example);
                    int s = getConstructionCost(res, 1, count, "STONE", example);
                    int c = getConstructionCost(res, 1, count, "COIN", example);
                    int f = getConstructionCost(res, 1, count, "FOOD", example);
                    boolean affordable = kingdom.canAffordResources(w, s, c, f);
                    boolean permitted = rules.canBuildSpecificType(user, type, res);

                    String statusMessage;
                    if (!permitted) {
                        statusMessage = "Limit reached for this building type.";
                    } else if (!affordable) {
                        statusMessage = "Not enough resources.";
                    } else {
                        statusMessage = "Ready to build.";
                    }

                    String displayName = type + " (" + res + ")";
                    String cost = String.format("W:%d S:%d C:%d F:%d", w, s, c, f);
                    options.add(new ConstructionOption(displayName, type, res, cost, statusMessage, permitted && affordable));
                }
            } else { // برای ساختمان‌های عمومی
                int count = kingdom.countBuildingsOfType(type);
                Building example = new Barrack(kingdom); // یک نمونه برای گرفتن هزینه

                int w = getConstructionCost(null, 1, count, "WOOD", example);
                int s = getConstructionCost(null, 1, count, "STONE", example);
                int c = getConstructionCost(null, 1, count, "COIN", example);
                int f = getConstructionCost(null, 1, count, "FOOD", example);
                boolean affordable = kingdom.canAffordResources(w, s, c, f);
                boolean permitted = rules.canBuild(user, type, count);

                String statusMessage;
                if (!permitted) {
                    statusMessage = "Prerequisites not met or limit reached.";
                } else if (!affordable) {
                    statusMessage = "Not enough resources.";
                } else {
                    statusMessage = "Ready to build.";
                }

                String cost = String.format("W:%d S:%d C:%d F:%d", w, s, c, f);
                options.add(new ConstructionOption(type, type, null, cost, statusMessage, permitted && affordable));
            }
        }
        return options;
    }    // ───────────────────────── گزینه‌های ارتقاء (غیرترمینالی)

//    public static Result update(Building building, int gameSpeed) {
//        if (building.getStatus() == Status.underConstruction && building.isConstructionComplete(gameSpeed)) {
//            building.completeConstruction();
//            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
//        }
//        if (building.getStatus() == Status.upgrading && building.isUpgradeComplete(gameSpeed)) {
//            Result result = building.completeUpgrade();
//            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
//            return result;
//        }
//        if (building.getStatus() == Status.underRepair && building.isRepairComplete(gameSpeed)) {
//            Result result = building.completeRepair(gameSpeed);
//            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
//            return result;
//        }
//        return new Result(false,"update false");
//    }

    // File: BuildingManager.java

    // >> REPLACE THE ENTIRE 'update' METHOD WITH THIS NEW VERSION <<
    public static boolean update(Building building, int gameSpeed) {
        boolean visualStateChanged = false;

        // 1. بررسی اتمام ساخت
        if (building.getStatus() == Status.underConstruction && building.isConstructionComplete(gameSpeed)) {
            building.completeConstruction();
            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
            visualStateChanged = true;

            // فقط اگر اسم ساختمان شامل WallsOfGate "نبود" پیام بده
            if (!building.getName().contains("WallsOfGate")) {
                sendNotification("SUCCESS: Construction of " + building.getName() + " completed!");
                triggerVisualEffect(building, "Build Complete!", "GREEN");
            }
        }
        // 2. بررسی اتمام ارتقاء
        else if (building.getStatus() == Status.upgrading && building.isUpgradeComplete(gameSpeed)) {
            building.completeUpgrade();
            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
            visualStateChanged = true;

            if (!building.getName().contains("WallsOfGate")) {
                sendNotification("SUCCESS: " + building.getName() + " upgraded to Lvl " + building.getLevel() + "!");
                triggerVisualEffect(building, "Upgraded!", "GOLD");
            }
        }
        // 3. بررسی اتمام تعمیر
        else if (building.getStatus() == Status.underRepair && building.isRepairComplete(gameSpeed)) {
            building.completeRepair(gameSpeed);
            GameFileUtility.saveFullGameState(building.getKingdom().getOwner().getGame());
            visualStateChanged = true;

            if (!building.getName().contains("WallsOfGate")) {
                sendNotification("SUCCESS: Repair of " + building.getName() + " completed!");
                triggerVisualEffect(building, "Repaired!", "YELLOW");
            }
        }

        return visualStateChanged;
    }
    private static void sendNotification(String msg) {
        if (GameMediator.getInstance() != null) {
            GameMediator.getInstance().sendMessage(msg);
        }
    }

    // متد جدید برای ارسال درخواست افکت تصویری
    private static void triggerVisualEffect(Building building, String text, String color) {
        if (GameMediator.getInstance() != null) {
            int userId = building.getKingdom().getOwner().getId();
            GameMediator.getInstance().triggerFloatingText(userId, building, text, color);
        }
    }

    public static MyLinkedList<UpgradeOption> getUpgradeOptions(GameUser user) {
        MyLinkedList<UpgradeOption> options = new MyLinkedList<>();
        Kingdom kingdom = user.getKingdom();
        GameRulesManager rules = user.getGame().getRulesManager();
        UserSubMap userSubMap = kingdom.getUserMap();

        for (Building b : kingdom.getBuildings()) {
            String type = b.getName();
            int currentLevel = b.getLevel();
            int nextLevel = currentLevel + 1;

            String resType = (b instanceof Mine m) ? m.getResourceType()
                : (b instanceof Storage s) ? s.getResourceType() : null;
            String displayName = (resType != null) ? type + " (" + resType + ")" : type;

            int count = (b instanceof Mine m) ? kingdom.countMinesOfType(m.getResourceType())
                : (b instanceof Storage s) ? kingdom.countStoragesOfType(s.getResourceType())
                : kingdom.countMinesOfType(b.getClass().getTypeName());

            int w = getUpgradeCost(resType, nextLevel, count, "WOOD", b);
            int s = getUpgradeCost(resType, nextLevel, count, "STONE", b);
            int c = getUpgradeCost(resType, nextLevel, count, "COIN", b);
            int t = GameBalance.getMilitaryBuildTime(type, nextLevel);
            String cost = String.format("W : %d S : %d C : %d", w, s, c);

            String statusMessage;
            boolean isActionable;
            Result permissionResult;
            boolean affordable;

            if (!b.isBuilt()) {
                permissionResult = new Result(false, "The building is still under construction");
            } else if (b.getStatus() == Status.upgrading) {
                permissionResult = new Result(false, "This building is currently being upgraded");
            } else {
                permissionResult = rules.canUpgradeBuilding(user, b);
            }

            if (!permissionResult.isSuccess()) {
                statusMessage = permissionResult.getMessage();
                isActionable = false;
                affordable = false;
            } else {
                affordable = kingdom.canAffordResources(w, s, c, 0);
                if (!affordable) {
                    statusMessage = "You do not have enough resources for this upgrade";
                    isActionable = false;
                } else {
                    statusMessage = "This building is ready for upgrade";
                    isActionable = true;
                }
            }

            Position localPos = userSubMap.globalToLocal(b.getPosition());
            String coords = localPos != null
                ? "(" + (localPos.getX()) + "," + (localPos.getY()) + ")" : "out of bond";

            options.add(new UpgradeOption(displayName, resType, currentLevel, nextLevel, t, cost,
                permissionResult.isSuccess(), affordable, coords, statusMessage, isActionable));
        }
        return options;
    }

    public static MyLinkedList<RepairOption> getRepairOptions(GameUser user) {
        MyLinkedList<RepairOption> options = new MyLinkedList<>();
        Kingdom kingdom = user.getKingdom();
        UserSubMap userSubMap = kingdom.getUserMap();

        for (Building b : kingdom.getBuildings()) {
            String type = b.getName();
            int level = b.getLevel();
            String resType = (b instanceof Mine m) ? m.getResourceType()
                : (b instanceof Storage s) ? s.getResourceType() : null;
            String displayName = (resType != null) ? type + " (" + resType + ")" : type;
            int count = (b instanceof Mine m) ? kingdom.countMinesOfType(m.getResourceType())
                : (b instanceof Storage s) ? kingdom.countStoragesOfType(s.getResourceType())
                : kingdom.countMinesOfType(b.getClass().getTypeName());
            int w = getRepairCost(resType, level, count, "WOOD", b);
            int s = getRepairCost(resType, level, count, "STONE", b);
            int c = getRepairCost(resType, level, count, "COIN", b);
            int t = GameBalance.getMilitaryRepairTime(type, level);
            String cost = String.format("W: %d  S: %d  C: %d", w, s, c);

            String statusMessage;
            boolean isActionable;

            if (b.getStatus() == Status.underRepair) {
                statusMessage = "This building is already under repair.";
                isActionable = false;
            } else if (!b.isDestroyed()) {
                statusMessage = "This building is not damaged and requires no repair.";
                isActionable = false;
            } else if (!kingdom.canAffordResources(w, s, c, 0)) {
                statusMessage = "You do not have enough resources to repair.";
                isActionable = false;
            } else {
                statusMessage = "This building is ready for repair.";
                isActionable = true;
            }

            Position localPos = userSubMap.globalToLocal(b.getPosition());
            String coords = localPos != null
                ? "(" + (localPos.getX()) + "," + (localPos.getY()) + ")" : "out-of-range";

            options.add(new RepairOption(displayName, level, t, cost, coords, statusMessage, isActionable));
        }
        return options;
    }

    public static int getConstructionCost(String type, int level, int count, String costType, Building b) {
        String name = b.getName().toUpperCase();
        if (name.contains("MINE")) return GameBalance.getMineUpgradeCost(type, level, count, costType);
        if (name.contains("STORAGE")) return GameBalance.getStorageUpgradeCost(type, level, count, costType);
        return GameBalance.getMilitaryBuildingCost(name, level, costType);
    }

    public static int getUpgradeCost(String type, int level, int count, String costType, Building b) {
        return getConstructionCost(type, level, count, costType, b);
    }

    public static int getRepairCost(String type, int level, int count, String costType, Building b) {
        String name = b.getName().toUpperCase();
        if (name.contains("MINE")) return GameBalance.getMineRepairCost(type, level, count, costType);
        if (name.contains("STORAGE")) return GameBalance.getStorageRepairCost(type, level, count, costType);
        return GameBalance.getMilitaryRepairCost(name, level, costType);
    }

}
