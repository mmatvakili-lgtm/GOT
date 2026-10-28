package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Army.Soldier;

import java.io.Serializable;

public class WhiteWalkerSoldier extends Soldier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int baseAttack;
    private int baseHealth;

    public WhiteWalkerSoldier(int attack, int health) {
        super(SoldierType.WHITEWALKER);
        this.baseAttack = Math.max(1, attack);
        this.baseHealth = Math.max(1, health);
        try { setAttackPower(this.baseAttack); } catch (Throwable ignored) {}
        try { setHealth(this.baseHealth); } catch (Throwable ignored) {}
    }

    @Override
    public boolean isEquippedWithDragonGlass() {
        return false;
    }

    @Override
    public String toString() {
        return "WhiteWalker{" + "atk=" + baseAttack + ", hp=" + baseHealth + '}';
    }
}
