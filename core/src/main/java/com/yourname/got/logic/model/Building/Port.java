package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;
import java.io.Serializable;

public class Port extends Building implements Serializable {
    private static final long serialVersionUID = 3903475684092028090L;

    private Kingdom owner;
    private boolean isActive;
    public Port(Kingdom owner, boolean isActive) {
        super(
            "Port",
            1,
            1000,
            120,
            90
        );
        this.isActive = isActive;
        this.owner = owner;
    }
    public Kingdom getKingdom() {
        return owner;
    }


    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🚪 Port started to upgrade to Level " + level + "! Health: " + strength);
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        completeRepair(gameSpeed);
        return true;
    }

    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("Port", this.level);
    }

    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("Port", this.level);
    }

    @Override
    public int getRawBuildTime() {
        return GameBalance.getMilitaryBuildTime("Port", this.level);
    }
}
