package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;

import java.io.Serializable;

public abstract class Soldier implements Serializable {
    private static final long serialVersionUID = 706117369100650113L;

    protected SoldierType type;
    protected int attackPower;
    protected int cost;
    protected int trainingTime;
    protected int health;

    protected boolean equippedWithDragonGlass = false;

    private boolean isStarving = false;

    public Soldier(SoldierType type) {
        this.type = type;
        this.attackPower = type.power;
        this.trainingTime = (int)(type.buildTimeMinutes * 60);
        this.health = type.health;
    }

    public SoldierType getType() { return type; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public void setDamage(int damage) { this.health -= damage; }


    public int getAttackPower() {
        if (isStarving) {
            // اعمال جریمه ۲۵ درصدی به صورت موقت
            return (int) (this.attackPower * 0.75);
        }
        return this.attackPower; //
    }

    public boolean isStarving() {
        return isStarving;
    }

    public void setStarving(boolean starving) {
        this.isStarving = starving;
    }

    public void setAttackPower(int attackPower){
        this.attackPower=attackPower;
    }

    public int getTrainingTime() { return trainingTime; }

    public int getFoodCost() { return type.foodCost; }
    public int getWoodCost() { return type.woodCost; }
    public int getCoinCost() { return type.coinCost; }
    public int getFoodUpkeep() { return type.foodPerHour; }

    @Override
    public String toString() {

        return type + " (AP: " + attackPower + ", Training Time: " + trainingTime + "s, HP: " + health + ")";
    }

    public void reduceAttackPowerByPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            return;
        }
        int reduction = (int) (this.attackPower * (percentage / 100.0));
        this.attackPower -= reduction;
        if (this.attackPower < 0) {
            this.attackPower = 0;
        }
    }

    public void reduceHealthByPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            return;
        }
        int damage = (int) (this.health * (percentage / 100.0));
        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }
    }
    public int getStoneCost() {
        return 0;
    }

    public boolean isEquippedWithDragonGlass() {
        return equippedWithDragonGlass;
    }

    public void equipDragonGlass() {
        this.equippedWithDragonGlass = true;
    }

    public void setEquippedWithDragonGlass(boolean equipped) {
        this.equippedWithDragonGlass = equipped;
    }

    public boolean isAlive() {
        return health > 0;
    }

}
