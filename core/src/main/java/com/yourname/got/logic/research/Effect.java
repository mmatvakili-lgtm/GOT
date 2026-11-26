package com.yourname.got.logic.research;

/** What a technology improves. Every effect is a percentage bonus that becomes a multiplier (1 + percent / 100). */
public enum Effect {
    PRODUCTION("Mine production"),
    STORAGE("Storage capacity"),
    RESEARCH_SPEED("Research speed"),
    TRAIN_SPEED("Soldier training speed"),
    MARCH_SPEED("Army march speed"),
    ATTACK("Soldier attack power"),
    HEALTH("Soldier toughness (damage taken is reduced)"),
    LOOT("Loot taken from raided storages"),
    TOWER("Defensive tower damage"),
    STRUCTURE("Building toughness (damage taken is reduced)");

    public final String label;

    Effect(String label) { this.label = label; }
}
