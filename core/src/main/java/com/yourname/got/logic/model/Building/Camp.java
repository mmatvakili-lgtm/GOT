package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
//import Game.enums.SoldierType;
import com.yourname.got.logic.model.Army.Soldier;
//import Game.model.Army.TrainingTask;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.mediator.GameMediator;

import java.io.Serializable;

public class Camp extends Building implements Serializable {
    private Kingdom owner;
    private MyLinkedList<Soldier> trainedSoldiers;
    private int maxCapacity;

    public Camp(Kingdom owner) {
        super("Camp", 1, 120, GameBalance.getMilitaryBuildTime("CAMP", 1) , 60);
        this.owner = owner;
        this.trainedSoldiers = new MyLinkedList<>();
    }

    /**
     * Capacity is shared by all camps of the kingdom, so it is checked kingdom-wide.
     * (It used to compare ONE camp's capacity with the soldiers of the WHOLE kingdom,
     * which made every camp look full too early.)
     */
    public boolean hasCapacity() {
        return owner != null && owner.getFreeSoldierSlots() > 0;
    }

    public boolean addTrainedSoldier(Soldier soldier) {
        if (!hasCapacity()) {
            System.out.println("❌ Camp has no space for more soldiers.");
            GameMediator.sendGlobalMessage("Camp has no space for more soldiers");
            return false;
        }
        owner.getArmyManager().recruit(soldier);
        trainedSoldiers.add(soldier);
        return true;
    }

    public int getMaxCapacity() {
        return GameBalance.getCampCapacity(this.level);
    }


    public int getCurrentSoldierCount() {
        // --- رفع باگ: کمپ باید ظرفیت را بر اساس کل سربازان فعلی ارتش بخواند ---
        // اگر owner یا armyManager نال بود، 0 برگردان
        if (owner == null) return 0;
        return owner.getOwnedSoldierCount();
    }

    /** Free soldier slots of the whole kingdom (camps share one pool). */
    public int getFreeCapacity() {
        return owner == null ? 0 : owner.getFreeSoldierSlots();
    }

    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("⛺ Camp started to upgrade to level " + this.level + " ( Capacity: " + this.maxCapacity + ")");
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ CAMP start to repair ");
        startRepair(gameSpeed);
        return true;

    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("CAMP", this.level);
    }
    @Override
    public int getRawBuildTime() {
        return GameBalance.getMilitaryBuildTime("CAMP", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("CAMP", this.level);
    }
}
