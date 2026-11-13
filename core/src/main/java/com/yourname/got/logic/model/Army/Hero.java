package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;

import java.io.Serializable;

public class Hero implements Serializable {
    // Keeps old saved games loadable (this is the value Java computed before the revive fields were added).
    private static final long serialVersionUID = 8597127391767608361L;
    /** Minutes (at game speed 1) a fallen hero needs before he can fight again. */
    public static final int REVIVE_MINUTES = 10;

    private long diedAtMillis = 0;
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
    public boolean isAvailable() { return unlocked && !isBusy && currentHealth > 0; }
    public int getHealth() { return currentHealth; }

    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
        if (this.currentHealth <= 0) markDeath(); else diedAtMillis = 0;
    }

    public void takeDamage(int damage) {
        this.currentHealth -= damage;
        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            markDeath();
        }
    }

    private void markDeath() {
        if (diedAtMillis == 0) diedAtMillis = System.currentTimeMillis();
    }

    /**
     * A fallen hero comes back with full health after REVIVE_MINUTES / gameSpeed minutes.
     * (Before, a hero who lost a duel stayed at 0 health forever but was still treated as available.)
     * @return true if the hero was revived by this call
     */
    public boolean tryRevive(int gameSpeed) {
        if (currentHealth > 0 || !unlocked) return false;
        if (diedAtMillis == 0) { diedAtMillis = System.currentTimeMillis(); return false; }
        long needed = (long) (REVIVE_MINUTES * 60_000L / Math.max(1, gameSpeed));
        if (System.currentTimeMillis() - diedAtMillis < needed) return false;
        restoreHealth();
        return true;
    }

    public boolean isDead() { return currentHealth <= 0; }

    // بازگردانی سلامتی کامل (مثلاً بعد از پایان نبرد یا احیا)
    public void restoreHealth() { this.currentHealth = maxHealth; this.diedAtMillis = 0; }

    public boolean isUnlocked() { return unlocked; }

    /** Text for the HUD (the old label printed "true/false", which looked like "locked" while the hero was just busy). */
    public String getStatusText() {
        if (!unlocked) return "Locked (Castle Lv " + unlockLevel + ")";
        if (currentHealth <= 0) return "Fallen (returns later)";
        if (isBusy) return "Busy";
        return "Ready";
    }
    public int getAttackBonus() { return attackBonus; }
    public int getDefenseBonus() { return defenseBonus; }
    public boolean hasPassiveEffect() { return passiveEffect; }
    public int getUnlockLevel() { return unlockLevel; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public KingdomType getKingdomType() { return kingdomType; }
}
