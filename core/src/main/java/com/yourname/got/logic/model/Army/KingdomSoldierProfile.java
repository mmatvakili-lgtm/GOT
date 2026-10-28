package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.SoldierType;

public class KingdomSoldierProfile {
    private KingdomType kingdom;
    private SoldierType baseType;
    private String displayName;
    private int customHealth;
    private int customAttack;

    public KingdomSoldierProfile(KingdomType kingdom, SoldierType baseType, String displayName, int health, int attack) {
        this.kingdom = kingdom;
        this.baseType = baseType;
        this.displayName = displayName;
        this.customHealth = health;
        this.customAttack = attack;
    }

    public KingdomType getKingdom() {
        return kingdom;
    }

    public SoldierType getBaseType() {
        return baseType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCustomHealth() {
        return customHealth;
    }

    public int getCustomAttack() {
        return customAttack;
    }
}
