package com.yourname.got.logic.Balance;

import java.io.Serializable;

public class KingdomConstraints implements Serializable {
    public boolean canBuildBarrack;
    public boolean canBuildTransport;
    public boolean canBuildGate;
    public int maxStorageLevel;
    public int maxMineLevel;
    public int maxMineCount;
    public int maxStorageCount;

    public KingdomConstraints(boolean canBuildBarrack, boolean canBuildTransport, boolean canBuildGate, int maxStorageLevel, int maxMineLevel , int maxMineCount , int maxStorageCount) {
        this.canBuildBarrack = canBuildBarrack;
        this.canBuildTransport = canBuildTransport;
        this.canBuildGate = canBuildGate;
        this.maxStorageLevel = maxStorageLevel;
        this.maxMineLevel = maxMineLevel;
        this.maxMineCount = maxMineCount;
        this.maxStorageCount = maxStorageCount;
    }
}
