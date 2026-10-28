package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class Gate extends Building implements Serializable {
    private static final long serialVersionUID = 4298820267873306833L;

    private Kingdom owner;
    public Gate(Kingdom owner) {
        super(
            "Gate",
            1,
            1000,
            120,
            90
        );
        this.owner = owner;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🚪 Gate started to upgrade to Level " + level + "! Health: " + strength);
        startUpgrade(gameSpeed);
        return true;
    }
    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean repair(int gameSpeed) {
        completeRepair( gameSpeed);
        return true;
    }

    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("GATE", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("GATE", this.level);
    }
    @Override
    public int getRawBuildTime(){
        return GameBalance.getMilitaryBuildTime("GATE", this.level);
    }

}
