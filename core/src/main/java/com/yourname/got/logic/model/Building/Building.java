package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.model.Position;
import com.yourname.got.logic.model.UserSubMap;
import com.yourname.got.logic.util.Result;

import static com.yourname.got.logic.Config.GameConfig.adjustTime;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Building implements Serializable {
    private static final long serialVersionUID = -2237430140665262301L;
    protected String name;
    protected Kingdom kingdom;
    protected int level;
    protected int strength;
    public Status status;
    protected Position position;
    protected int baseBuildTime;
    protected int baseUpgradeTime;
    protected boolean isBuilt = false;

    protected long constructionStartTime = -1;
    protected long upgradeStartTime = -1;

    protected boolean isDestroyed = false;
    protected long repairStartTime = -1;

    public Building(String name, int level, int strength, int baseBuildTime, int baseUpgradeTime) {
        this.name = name;
        this.level = level;
        this.strength = strength;
        this.baseBuildTime = baseBuildTime;
        this.baseUpgradeTime = baseUpgradeTime;
        this.position = new Position();
        constructionStartTime=System.currentTimeMillis();
        status=Status.underConstruction;
    }

    public Building() {
        this("Default", 1, 100, 60, 60);
    }


    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusReport(int gameSpeed, UserSubMap userSubMap) {
        String nameStr = name;
        String levelStr = "L" + level;

        Position localPos = userSubMap.globalToLocal(getPosition());

        String xStr = localPos != null ? String.valueOf(localPos.getX() + 1) : "❌";
        String yStr = localPos != null ? String.valueOf(localPos.getY() + 1) : "❌";

        String statusStr = status != null ? status.name().toUpperCase() : "نامشخص";
        String healthStr = strength + (isDestroyed ? " (تخریب)" : "");

        String buildStr = "-";
        if (constructionStartTime != -1 && !isBuilt()) {
            long remaining = (getAdjustedBuildTime(gameSpeed) * 1000L) - (System.currentTimeMillis() - constructionStartTime);
            buildStr = "ساخت: " + Math.max(remaining / 1000, 0) + "s";
        } else if (upgradeStartTime != -1) {
            long remaining = (getAdjustedUpgradeTime(gameSpeed) * 1000L) - (System.currentTimeMillis() - upgradeStartTime);
            buildStr = "ارتقاء: " + Math.max(remaining / 1000, 0) + "s";
        }

        String repairStr = "-";
        if (repairStartTime != -1) {
            long remaining = (getAdjustedRepairTime(gameSpeed) * 1000L) - (System.currentTimeMillis() - repairStartTime);
            repairStr = Math.max(remaining / 1000, 0) + "s";
        }

        return String.format("%-15s %-10s %-10s %-12s %-10s %-20s %-20s %-20s",
            nameStr, levelStr, xStr, yStr, statusStr, healthStr, buildStr, repairStr);
    }


    public void destroy() {
        this.isDestroyed = true;
        this.isBuilt = false;
        this.status = Status.destroyed;
        System.out.println("Building " + name + " has been destroyed and is now inactive. ");
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }
    public void startRepair(int gameSpeed) {
        if (!isDestroyed){
            System.out.println("Building " + name + " is not destroyed. No repair needed. ");
            return;
        }

        this.repairStartTime = System.currentTimeMillis();
        this.status=Status.underRepair;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int adjustedSeconds = adjustTime(getRawRepairTime(), gameSpeed);
        long endTimeMillis = repairStartTime + adjustedSeconds * 1000L;

        System.out.println("Building " + name +
            " | Start: " + fmt.format(new Date(repairStartTime)) +
            " | End: " + fmt.format(new Date(endTimeMillis)) +
            " | Speed: " + gameSpeed + "x");
    }

    public boolean isRepairComplete( int gameSpeed) {
        if (repairStartTime == -1)
            return false;
        long elapsedTime = System.currentTimeMillis() - repairStartTime;
        return elapsedTime >= getAdjustedRepairTime(gameSpeed) * 1000L;
    }

    public Result completeRepair( int gameSpeed) {
        if (!isRepairComplete(gameSpeed))
            return new Result(false,"");
        this.isDestroyed = false;
        this.isBuilt = true;
        this.repairStartTime = -1;
        this.status = Status.stable;
        return new Result(true,"Building " + name + " has been fully repaired and is now active again. ");
    }

    public int getHealth() {
        return strength;
    }

    public void startConstruction(int gameSpeed) {
        this.constructionStartTime = System.currentTimeMillis();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int adjustedSeconds = adjustTime(getRawBuildTime(), gameSpeed);
        long endTimeMillis = constructionStartTime + adjustedSeconds * 1000L;

        System.out.println("Building " + name +
            " | Start: " + fmt.format(new Date(constructionStartTime)) +
            " | End: " + fmt.format(new Date(endTimeMillis)) +
            " | Speed: " + gameSpeed + "x");

        this.status = Status.underConstruction;
    }

    public boolean isConstructionComplete( int  gameSpeed) {
        if (constructionStartTime == -1) return false;
        long elapsed = System.currentTimeMillis() - constructionStartTime;
        return elapsed >= getAdjustedBuildTime(gameSpeed) * 1000L;
    }

    public void completeConstruction() {
        markAsBuilt();
        this.constructionStartTime = -1;
        this.status=Status.stable;
        System.out.println("✅ " + name + " construction completed.");
    }


    public void startUpgrade(int gameSpeed) {
        this.upgradeStartTime = System.currentTimeMillis();
        this.status=Status.upgrading;

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int adjustedSeconds = adjustTime(getRawUpgradeTime(), gameSpeed);
        long endTimeMillis = upgradeStartTime + adjustedSeconds * 1000L;

        System.out.println("Building " + name +
            " | Start: " + fmt.format(new Date(upgradeStartTime)) +
            " | End: " + fmt.format(new Date(endTimeMillis)) +
            " | Speed: " + gameSpeed + "x");
    }

    public boolean isUpgradeComplete(int  gameSpeed) {
        if (upgradeStartTime == -1) return false;
        long elapsed = System.currentTimeMillis() - upgradeStartTime;
        return elapsed >= getAdjustedUpgradeTime(gameSpeed) * 1000L;
    }

    public Result completeUpgrade() {
        this.level++;
        this.upgradeStartTime = -1;
        this.status=Status.stable;
        this.strength=level*100;
        System.out.println("⬆️ " + name + " upgraded to level " + level);
        return new Result(true,name + " upgraded to level " + level);
    }


    public boolean isBuilt() { return isBuilt; }

    public boolean isActiveBuilding() {
        return isBuilt()
            && !isDestroyed()
            && status == Status.stable;
    }
    public boolean isFreezeBuilding() {
        return !isDestroyed()
            && (status ==Status.underConstruction || status == Status.underRepair || status == Status.upgrading);
    }

    public void markAsBuilt() {
        this.isBuilt = true;
        status=Status.stable;}

    public Kingdom getKingdom() {
        return kingdom;
    }

    public int getAdjustedBuildTime(int gameSpeed) {
        return adjustTime(getRawBuildTime(),gameSpeed);
    }

    public int getAdjustedUpgradeTime( int gameSpeed) {
        return adjustTime(getRawUpgradeTime(),gameSpeed);
    }

    public int getAdjustedRepairTime( int gameSpeed) {
        return adjustTime(getRawRepairTime(),gameSpeed);
    }

    public void takeDamage(int damage) {
        this.strength -= damage;
        if (this.strength < 0) this.strength = 0;
    }

    public long getUpgradeStartTime() {
        return upgradeStartTime;
    }

    public long getConstructionStartTime() {
        return constructionStartTime;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(int x, int y) {
        this.position.setX(x);
        this.position.setY(y);
    }
    public void setStrength(int strength) {
        this.strength = strength;
    }

    @Override
    public String toString() {
        return name + " (Level " + level + ") at " + position + " | Health: " + strength;
    }

    public abstract boolean upgrade(int gameSpeed);
    public abstract boolean repair( int gameSpeed);
    public abstract int getRawBuildTime();
    public abstract int getRawUpgradeTime();
    public abstract int getRawRepairTime();
}

