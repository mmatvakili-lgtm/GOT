package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class WallsOfGate extends Building implements Serializable {
    private static final long serialVersionUID = 6072004414795482478L;

    private Kingdom owner;
    public WallsOfGate(Kingdom owner) {
        super(
            "WallsOfGate",
            1,
            1000,
            120,
            90
        );
        this.owner = owner;
    }
    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🚪 WallsOfGate upgraded to Level " + level + "! Health: " + strength);
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        completeRepair( gameSpeed);
        return true;
    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("WALLSOGGATE", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("WALLSOGGATE", this.level);
    }
    @Override
    public int getRawBuildTime(){
        return GameBalance.getMilitaryBuildTime("WALLSOGGATE", this.level);
    }
}
