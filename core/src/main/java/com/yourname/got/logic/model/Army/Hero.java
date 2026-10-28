package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;

import java.io.Serializable;

public class Hero implements Serializable {
    private final String name;
    private final KingdomType kingdomType;
    private final String description;
    private final int unlockLevel;
    private final int attackBonus;
    private final int defenseBonus;
    private final boolean passiveEffect;
    private int currentHealth ; // جان پیش‌فرض
    private int maxHealth ;

    private boolean unlocked = false;
    private boolean isBusy = false;

    public Hero(String name, KingdomType kingdomType, String description,
                int unlockLevel, int attackBonus, int defenseBonus, boolean passiveEffect , int currentHealth , int maxHealth) {
        this.name = name;
        this.kingdomType = kingdomType;
        this.description = description;
        this.unlockLevel = unlockLevel;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.passiveEffect = passiveEffect;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
    }

    public void unlock() { unlocked = true; }
    public void markBusy() { isBusy = true; }
    public void release() { isBusy = false; }
    public boolean isAvailable() { return unlocked && !isBusy; }
    public int getHealth() { return currentHealth; }

    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
    }

    public void takeDamage(int damage) {
        this.currentHealth -= damage;
        if (this.currentHealth < 0) this.currentHealth = 0;
    }

    public boolean isDead() { return currentHealth <= 0; }

    // بازگردانی سلامتی کامل (مثلاً بعد از پایان نبرد یا احیا)
    public void restoreHealth() { this.currentHealth = maxHealth; }

    public boolean isUnlocked() { return unlocked; }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public boolean hasPassiveEffect() { return passiveEffect; }
    public int getUnlockLevel() { return unlockLevel; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public KingdomType getKingdomType() { return kingdomType; }
}
