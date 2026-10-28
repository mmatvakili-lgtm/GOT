package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class CrowTower extends Building implements Serializable {
    private Kingdom owner;
    private Boolean active;
    private Double crowSpeed;
    public CrowTower( Kingdom owner) {
        super("CrowTower", 1, 180, 60, 60);
        this.owner = owner;
        active=true;
        crowSpeed=1.0;
    }

    public Double getCrowSpeed() {
        return crowSpeed;
    }

    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🏯 CrowTower start to upgrade to level " + level);
        startUpgrade(gameSpeed);
        return true;
    }
    public Boolean isActive(){return active;};
    public void setActive(Boolean active){
        this.active=active;
    }
    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ CROWTOWER start to repair ");
        startRepair(gameSpeed);
        return true;
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("CROWTOWER", this.level);
    }
    @Override
    public int getRawBuildTime(){
        return GameBalance.getMilitaryBuildTime("CROWTOWER", this.level);
    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("CROWTOWER", this.level);
    }
}
