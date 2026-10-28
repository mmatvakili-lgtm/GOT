package com.yourname.got.logic.Balance;

import com.yourname.got.logic.enums.KingdomType;
import java.io.Serializable;


public final class GameBalance implements Serializable {

    public static final int BASE_WOOD  = 5;
    public static final int BASE_STONE = 4;
    public static final int BASE_COIN  = 3;
    public static final int BASE_FOOD  = 6;


    public static int getProductionPerMinute(String type, int level) {
        return switch (type.toUpperCase()) {
            case "WOOD"  -> BASE_WOOD  * (int) Math.pow(1.2, level-1);
            case "STONE" -> BASE_STONE * (int) Math.pow(1.2, level-1);
            case "COIN"  -> BASE_COIN  * (int) Math.pow(1.2, level-1);
            case "FOOD"  -> BASE_FOOD  * (int) Math.pow(1.2, level-1);
            default -> 0;
        };
    }

    public static double getKingdomProductionMultiplier(KingdomType type, String resourceType) {
        return switch (type) {
            case WINTERFELL -> (
                resourceType.equalsIgnoreCase("FOOD") ? 0.7 :
                    resourceType.equalsIgnoreCase("STONE")? 1.2 : 1.0);
            case EYRIE      -> (
                resourceType.equalsIgnoreCase("FOOD") ? 0.6 :
                    resourceType.equalsIgnoreCase("COIN")?  1.1 : 1.0 );
            case IRON_ISLANDS ->( resourceType.equalsIgnoreCase("COIN") ? 1.2 : 1.0 );
            case CASTERLY_ROCK ->
                resourceType.equalsIgnoreCase("COIN") ? 1.4 :
                    resourceType.equalsIgnoreCase("FOOD") ? 0.8 : 1.0;
            case STORMS_END -> (resourceType.equalsIgnoreCase("STONE") ? 1.2 : 1.0);
            case HIGHGARDEN -> (resourceType.equalsIgnoreCase("FOOD") ? 1.5 : 1.0);
            case DRAGONSTONE -> (resourceType.equalsIgnoreCase("WOOD") ? 0.8 : 1.0 );
        };
    }

    public static KingdomConstraints getConstraintsFor(KingdomType type) {
        return switch (type) {
            case WINTERFELL     -> new KingdomConstraints(true, true, true,  4, 5, 8,8);
            case EYRIE          -> new KingdomConstraints(true, false, true ,4, 5, 7 ,8);
            case IRON_ISLANDS   -> new KingdomConstraints(true, true, false,5, 5, 7 ,8);
            case CASTERLY_ROCK  -> new KingdomConstraints(true, true, true,5, 5, 8 ,8);
            case STORMS_END     -> new KingdomConstraints(true, true, true ,4, 5, 8,8);
            case HIGHGARDEN     -> new KingdomConstraints(true, true,true, 5, 5, 7,8);
            case DRAGONSTONE    -> new KingdomConstraints(false, true, true , 4, 5, 7,8);
        };
    }

    public static int getStorageCapacity(String type, int level) {
        return roundCost( switch (type.toUpperCase()) {
            case "WOOD"  -> 1000 * BASE_WOOD  * (level + 5);
            case "STONE" -> 1000 * BASE_STONE * (level + 5);
            case "COIN"  -> 1000 * BASE_COIN  * (level + 5);
            case "FOOD"  -> 1000 * BASE_FOOD  * (level + 5);
            default -> 0;
        });
    }

    public static int getMineUpgradeCost(String resourceType, int nextLevel, int numbers, String costType) {
        int base = switch (resourceType.toUpperCase()) {
            case "WOOD"  -> BASE_WOOD;
            case "STONE" -> BASE_STONE;
            case "COIN"  -> BASE_COIN;
            case "FOOD"  -> BASE_FOOD;
            default -> 1;
        };
        int baseCost = base * numbers * nextLevel * 150;

        return roundCost( switch (costType.toUpperCase()) {
            case "WOOD"  -> baseCost;
            case "STONE" -> (int)(0.7 * baseCost);
            case "COIN"  -> (nextLevel >= 3) ? (int)(0.5 * baseCost) : 0;
            default -> 0;
        });
    }

    public static int getStorageUpgradeCost(String resourceType, int nextLevel, int numbers, String costType) {
        int base = switch (resourceType.toUpperCase()) {
            case "WOOD"  -> BASE_WOOD;
            case "STONE" -> BASE_STONE;
            case "COIN"  -> BASE_COIN;
            case "FOOD"  -> BASE_FOOD;
            default -> 1;
        };
        int baseCost = base * numbers * nextLevel * 300;

        return roundCost( switch (costType.toUpperCase()) {
            case "WOOD"  -> baseCost;
            case "STONE" -> (int)(0.7 * baseCost);
            case "COIN"  -> (nextLevel >= 3) ? (int)(0.5 * baseCost) : 0;
            default -> 0;
        });
    }

    public static int getBuildTimeMinutes(String type, int level) {
        int Bt = switch (type.toUpperCase()) {
            case "WOOD"  -> 90;
            case "STONE" -> 135;
            case "COIN"  -> 180;
            case "FOOD"  -> 90;
            default -> 90;
        };
        return roundTime((int) Math.round(Bt * Math.pow(1.4, level - 1)));
    }

    public static int getMilitaryBuildTime(String buildingType, int level) {
        double base = switch (buildingType.toUpperCase()) {
            case "CASTLE"    -> 210;
            case "CAMP"      -> 60;
            case "BARRACK"   -> 60;
            case "CROWTOWER" -> 90;
            case "DEFENSIVETOWER" -> 90;
            case "WALLSOFGATE" -> 0;
            case "GATE"    -> 0;
            case "TRANSPORT"    -> 90;
            case "BATTLESHIP" -> 120;

            default -> 60;
        };
        return roundTime ((int) Math.round(base * Math.pow(1.4, level - 1)));
    }

    public static int getMilitaryBuildingCost(String buildingType, int level, String resourceType) {
        int Bw = switch (buildingType.toUpperCase()) {
            case "CASTLE"    -> 3000;
            case "CAMP"      -> 1500;
            case "BARRACK"   -> 2000;
            case "CROWTOWER" -> 2500;
            case "DEFENSIVETOWER" -> 2500;
            case "WALLSOFGATE" -> 500;
            case "GATE"    -> 2000;
            case "TRANSPORT"    -> 2500;
            case "BATTLESHIP" -> 2500;

            default -> 1000;
        };
        int Cw = (int)(Bw * level * (level + 1) / Math.pow(1.4, level));
        return roundCost( switch (resourceType.toUpperCase()) {
            case "WOOD"  -> Cw;
            case "STONE" -> (int)(0.7 * Cw);
            case "COIN"  -> (level >= 3) ? (int)(0.5 * Cw) : 0;
            default      -> 0;
        });
    }

    private GameBalance() {}

    public static int getCampCapacity(int level) {
        int base = 20;
        int perLevel = 15;
        return base + (perLevel * (level-1));
    }

    public static int getMilitaryRepairCost(String buildingType, int level, String resourceType) {
        int Bw = switch (buildingType.toUpperCase()) {
            case "CASTLE" -> 3000;
            case "CAMP" -> 1500;
            case "BARRACK" -> 2000;
            case "CROWTOWER" -> 2500;
            case "DEFENSIVETOWER" -> 2500;
            case "WALLSOFGATE" -> 500;
            case "GATE"    -> 2000;
            case "TRANSPORT"    -> 2500;

            default -> 1000;
        };
        int Cw = Bw * level * (level + 1) / (int) (Math.pow(2.1, level));
        return roundCost( switch (resourceType.toUpperCase()) {
            case "WOOD" -> Cw;
            case "STONE" -> (int) (0.7 * Cw);
            case "COIN" -> (level >= 3) ? (int) (0.5 * Cw) : 0;
            default -> 0;
        });
    }

    public static int getMilitaryRepairTime(String buildingType, int level) {
        double base = switch (buildingType.toUpperCase()) {
            case "CASTLE"    -> 210;
            case "CAMP"      -> 60;
            case "BARRACK"   -> 60;
            case "CROWTOWER" -> 90;
            case "DEFENSIVETOWER" -> 90;
            case "WALLSOFGATE" -> 0;
            case "GATE"    -> 0;
            case "TRANSPORT"    -> 90;

            default -> 60;
        };
        return roundTime ((int) Math.round(base * Math.pow(1.4, level - 1)/2));
    }


    public static int getMineRepairCost(String resourceType, int nextLevel, int numbers, String costType) {
        int base = switch (resourceType.toUpperCase()) {
            case "WOOD"  -> BASE_WOOD;
            case "STONE" -> BASE_STONE;
            case "COIN"  -> BASE_COIN;
            case "FOOD"  -> BASE_FOOD;
            default -> 1;
        };
        int baseCost = base * numbers * nextLevel * 150/ (int) Math.pow(2.1, nextLevel);

        return roundCost( switch (costType.toUpperCase()) {
            case "WOOD"  -> baseCost;
            case "STONE" -> (int)(0.7 * baseCost);
            case "COIN"  -> (nextLevel >= 3) ? (int)(0.5 * baseCost) : 0;
            default -> 0;
        });
    }

    public static int getStorageRepairCost(String resourceType, int nextLevel, int numbers, String costType) {
        int base = switch (resourceType.toUpperCase()) {
            case "WOOD"  -> BASE_WOOD;
            case "STONE" -> BASE_STONE;
            case "COIN"  -> BASE_COIN;
            case "FOOD"  -> BASE_FOOD;
            default -> 1;
        };
        int baseCost = base * numbers * nextLevel * 300/(int)(Math.pow(2.1, nextLevel));

        return roundCost( switch (costType.toUpperCase()) {
            case "WOOD"  -> baseCost;
            case "STONE" -> (int)(0.7 * baseCost);
            case "COIN"  -> (nextLevel >= 3) ? (int)(0.5 * baseCost) : 0;
            default -> 0;
        });
    }

    public static int getMineRepairTimeMinutes(String type, int level) {
        int Bt = switch (type.toUpperCase()) {
            case "WOOD"  -> 30;
            case "STONE" -> 45;
            case "COIN"  -> 60;
            case "FOOD"  -> 30;
            default -> 30;
        };
        return roundTime((int) Math.round(Bt * Math.pow(1.4, level - 1)/2));
    }

    public static int getStorageRepairTimeMinutes(String type, int level) {
        int Bt = switch (type.toUpperCase()) {
            case "WOOD"  -> 60;
            case "STONE" -> 90;
            case "COIN"  -> 120;
            case "FOOD"  -> 60;
            default -> 60;
        };
        return roundTime((int) Math.round(Bt * Math.pow(1.4, level - 1)/2));
    }

    private static int roundCost(int value) {
        if (value < 1000) return Math.round(value / 50f) * 50;
        if (value < 10000) return Math.round(value / 100f) * 100;
        return Math.round(value / 500f) * 500;
    }

    private static int roundTime(int value) {
        if (value < 60) return Math.round(value / 5f) * 5;
        if (value < 300) return Math.round(value / 10f) * 10;
        return Math.round(value / 30f) * 30;
    }

}
