package com.yourname.got.logic.enums;

// در Game.enums
public enum SoldierType {
    INFANTRY(150, 35, 100, 500, 60, 1, 2),
    WHITEWALKER(300, 120, 0, 0, 0, 0, 0),
    ARCHER(100, 50, 120, 300, 90, 1.5, 3),
    CAVALRY(250, 80, 200, 800, 200, 2.5, 5),
    SPY(80, 10, 300, 150, 50, 180, 0);

    public final int health, power, foodCost, woodCost, coinCost;
    public final double buildTimeMinutes;
    public final int foodPerHour;

    SoldierType(int hp, int pow, int food, int wood, int coin, double time, int upkeep) {
        this.health = hp;
        this.power = pow;
        this.foodCost = food;
        this.woodCost = wood;
        this.coinCost = coin;
        this.buildTimeMinutes = time;
        this.foodPerHour = upkeep;
    }
}

