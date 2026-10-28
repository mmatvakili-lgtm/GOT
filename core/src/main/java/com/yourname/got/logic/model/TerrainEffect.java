package com.yourname.got.logic.model;

public  class TerrainEffect {
    private final double attackModifier;
    private final int healthReductionPercentage;

    public TerrainEffect(double attackModifier, int healthReductionPercentage) {
        this.attackModifier = attackModifier;
        this.healthReductionPercentage = healthReductionPercentage;
    }

    public double getAttackModifier() { return attackModifier; }
    public int getHealthReductionPercentage() { return healthReductionPercentage; }
}
