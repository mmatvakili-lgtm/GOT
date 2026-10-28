package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class Transport extends Building implements Serializable {   private Kingdom owner;
    private Boolean active;
    public  Double wagonSpeed;
    public Transport( Kingdom owner) {
        super("Transport", 1, 180, 60, 60);
        this.owner = owner;
        wagonSpeed=1.0;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🏯 Transport start to upgrade to level " + level);
        startUpgrade(gameSpeed);
        return true;
    }
    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ Transport start to repair " );
        startRepair(gameSpeed);
        return true;
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("TRANSPORT", this.level);
    }
    @Override
    public int getRawBuildTime(){
        return GameBalance.getMilitaryBuildTime("TRANSPORT", this.level);
    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("TRANSPORT", this.level);
    }
    public Boolean isActive(){return active;};
    public void setActive(Boolean active){
        this.active=active;
    }
}

