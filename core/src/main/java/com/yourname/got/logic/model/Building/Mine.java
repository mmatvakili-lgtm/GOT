package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.model.Kingdom;
import java.io.Serializable;

public class Mine extends Building implements Serializable {
    private static final long serialVersionUID = -1525712114519654432L;
    private String resourceType;
    private Kingdom owner;

    private long lastUpdateTime;
    private double fractionalProduction = 0;
    private long constructionCompleteTime;

    public Mine(String resourceType, Kingdom owner) {
        super("Mine of " + resourceType, 1, 100, 60, 45);
        this.resourceType = resourceType.toUpperCase();
        this.owner = owner;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public int produceAmount(int gameSpeed) {
        if (this.getStatus() == Status.underConstruction && isConstructionComplete(gameSpeed)) {
            completeConstruction();
        }

        if (!this.isActiveBuilding()) {
            lastUpdateTime = System.currentTimeMillis();
            return 0;
        }

        if (constructionCompleteTime > 0 && lastUpdateTime < constructionCompleteTime) {
            lastUpdateTime = constructionCompleteTime;
        }

        long now = System.currentTimeMillis();
        long secondsPassed = (now - lastUpdateTime) / 1000;
        if (secondsPassed <= 0) return 0;

        String difficulty = owner.getOwner().getGame().getDifficulty();
        double difficultyMultiplier;

        switch (difficulty.toLowerCase()) {
            case "easy" -> difficultyMultiplier = 1;
            case "medium" -> difficultyMultiplier =  0.9;
            case "hard" -> difficultyMultiplier = 0.81;
            default -> difficultyMultiplier = 1;
        }
        int baseProduction = GameBalance.getProductionPerMinute(resourceType, level);
        double multiplier = GameBalance.getKingdomProductionMultiplier(owner.getType(), resourceType);
        int producedPerMinute = (int) Math.round(baseProduction * multiplier * difficultyMultiplier) ;
        // Keep the fractional part: with a short tick (5 s) and slow speed, (int) alone threw away
        // everything below 1 unit and the mine could produce nothing at all.
        double exact = (producedPerMinute / 60.0) * secondsPassed * gameSpeed + fractionalProduction;
        int produced = (int) exact;
        fractionalProduction = exact - produced;

        // Only consume the whole seconds that were paid for; the sub-second rest counts next time.
        lastUpdateTime += secondsPassed * 1000L;
        return produced;
    }

    @Override
    public void completeConstruction() {
        super.completeConstruction();
        constructionCompleteTime = System.currentTimeMillis();
        lastUpdateTime = constructionCompleteTime;
    }

    public Kingdom getKingdom() { return owner; }
    @Override
    public boolean upgrade(int gameSpeed) {    System.out.println("✅ Mine of " + resourceType + " started to upgrade to level " + level);
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ MINE start to repair " );
        startRepair(gameSpeed);
        return true;
    }
    @Override public int getRawRepairTime() { return 0; }
    @Override public int getRawUpgradeTime() { return 0; }
    @Override public int getRawBuildTime() { return 0; }
    public String getResourceType() { return resourceType; }
}
