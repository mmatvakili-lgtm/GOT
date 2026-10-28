package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.Result;

import java.io.Serializable;

public class Castle extends Building implements Serializable {
    private static final long serialVersionUID = -8909103786108344456L;

    private Kingdom owner;

    public Castle( Kingdom owner) {
        super("Castle", 1, 300, 60, 60);
        this.owner = owner;
        completeConstruction();
    }
    public Castle() {}

    public Kingdom getKingdom() {
        return owner;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🏰 Castle start to upgrade to level " + level);
        startUpgrade(gameSpeed);
        return true;
    }
    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ Castle start to repair " );
        startRepair(gameSpeed);
        return true;
    }

    // در کلاس Castle.java متد completeUpgrade را جایگزین کنید:

    @Override
    public Result completeUpgrade() {
        // ابتدا ارتقای عادی انجام می‌شود (level++)
        Result res = super.completeUpgrade();

        // --- رفع باگ: حالا level مقدار جدید (5) را دارد ---
        if (this.level == 5 && owner != null) {
            var hero = owner.getHero();
            if (hero != null && !hero.isUnlocked()) {
                hero.unlock();
                // ثبت نوتیفیکشن باز شدن قهرمان
                if (owner.getOwner().getGame() != null) {
                    owner.getOwner().getGame().sendSystemNotificationToUser(owner.getOwner(), "Hero Unlocked!", "Your Castle reached Level 5. Hero " + hero.getName() + " has arrived!");
                }
                return new Result(true, "Hero " + hero.getName() + " has been unlocked!");
            }
        }
        return new Result(true, "Castle upgrade completed. Now at level " + level);
    }

    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("CASTLE", this.level);
    }
    @Override
    public int getRawBuildTime() {
        return GameBalance.getMilitaryRepairTime("CASTLE", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("CASTLE", this.level);
    }

}
