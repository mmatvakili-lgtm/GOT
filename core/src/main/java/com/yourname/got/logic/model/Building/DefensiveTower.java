package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class DefensiveTower extends Building implements Serializable {
    protected int powerAttack;
    protected Kingdom owner;
    protected int range;
    public DefensiveTower(Kingdom kingdom) {
        super(
            "Defensive Tower",
            1,
            500,
            90,
            60
        );
        this.owner = kingdom;
        this.powerAttack = 100;
        this.range=5;
    }

    public Kingdom getKingdom() {
        return owner;
    }

    public int getRange() {
        return range;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🏰 Defensive Tower upgraded to Level " + level + "! Health: " + strength + ", Attack Power: " + powerAttack);
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ DEFENSIVETOWER start to repair " );
        startRepair(gameSpeed);
        return true;
    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("DEFENSIVETOWER", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("DEFENSIVETOWER", this.level);
    }
    @Override
    public int getRawBuildTime(){
        return GameBalance.getMilitaryBuildTime("DEFENSIVETOWER", this.level);
    }

    public int getAttackPower() {
        return powerAttack;
    }
}
