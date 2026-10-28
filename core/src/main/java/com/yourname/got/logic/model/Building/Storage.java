package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public class Storage extends Building implements Serializable {
    private static final long serialVersionUID = 1141247228549147375L;

    private String resourceType;
    private int quantity =0;
    private int maxCapacity;
    private Kingdom owner;
    private transient boolean fullNotificationSent = false;


    public Storage(String resourceType, Kingdom owner) {
        super("Storage of " + resourceType, 1, 100, 60, 45);
        this.resourceType = resourceType.toUpperCase();
        this.owner = owner;
        this.maxCapacity = GameBalance.getStorageCapacity(resourceType, level);
    }
    public Kingdom getKingdom() {
        return owner;
    }

    public int addResource(int amount) {
        int space = getMaxCapacity() - quantity;
        int added = Math.min(space, amount);
        quantity += added;
        return added;
    }

    public boolean useResource(int amount) {
        if (quantity >= amount) {
            quantity -= amount;
            this.fullNotificationSent = false;
            return true;
        }
        return false;
    }
    public boolean hasSentFullNotification() {
        return this.fullNotificationSent;
    }

    public void setFullNotificationSent(boolean sent) {
        this.fullNotificationSent = sent;
    }
    @Override
    public boolean upgrade(int gameSpeed) {
        startUpgrade(gameSpeed);
        System.out.println("✅ Storage of " + resourceType + " started to upgrade " + level);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ STORAGE start to repair " );
        startRepair(gameSpeed);
        return true;
    }
    @Override
    public int getRawBuildTime() {
        return GameBalance.getBuildTimeMinutes(resourceType, level);
    }
    @Override
    public int getRawRepairTime() {
        return GameBalance.getStorageRepairTimeMinutes(resourceType, level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getBuildTimeMinutes(resourceType, level);
    }

    public String getResourceType() { return resourceType; }
    public int getQuantity() { return quantity; }
    public int getMaxCapacity() { return GameBalance.getStorageCapacity(resourceType, level); }
}
