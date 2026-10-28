package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Army.TrainingTask;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;

import java.io.Serializable;

public class Barrack extends Building implements Serializable {
    private Kingdom owner;

    public Barrack( Kingdom owner) {
        super("Barrack", 1, 150, 60, 60);
        this.owner = owner;
    }

    public boolean trainSoldier (SoldierType type, int gameSpeed) {
        if (!this.isActiveBuilding()) {
            System.out.println("⛔ Barrack is not available right now");
            return false;
        }
        if (type == SoldierType.SPY && owner.hasSpy()) {
            System.out.println("⛔ جاسوس قبلاً ساخته شده یا در حال آموزش است.");
            return false;
        }

        // --- رفع باگ ظرفیت کمپ: تطبیق با کلاس TrainingTask ---
        // هر آبجکت در صف دقیقا یک سرباز است، پس سایز صف برابر است با تعداد سربازان در حال آموزش
        int currentTotalSoldiers = owner.getArmyManager().getTotalSoldier();
        int soldiersInQueue = owner.getTrainingQueue().getSize();
        int totalExpectedSoldiers = currentTotalSoldiers + soldiersInQueue;
        int totalKingdomCapacity = owner.getTotalSoldierCapacity();

        if (totalKingdomCapacity == 0 || totalExpectedSoldiers >= totalKingdomCapacity) {
            System.out.println("⛔️ Army capacity is full or no active Camp exists. Cannot train " + type);
            return false;
        }
        // ---------------------------------------------------------

        int foodCost = type.foodCost;
        int woodCost = type.woodCost;
        int coinCost = type.coinCost;

        System.out.println(type + " training cost: " + woodCost + " WOOD " + coinCost + " COIN " + foodCost + " FOOD");

        if (!owner.canAffordResources(woodCost, 0, coinCost, foodCost)) {
            System.out.println("⛔ Not enough resources to train " + type);
            return false;
        }

        owner.detuctResources(woodCost, 0, coinCost, foodCost);

        MyLinkedList<TrainingTask> queue = owner.getTrainingQueue();
        long startTime;
        if (queue.isEmpty()) {
            startTime = System.currentTimeMillis();
        } else {
            TrainingTask lastTask = queue.getTail().data;
            startTime = lastTask.getStartMillis() + lastTask.getRequireMillies();
        }

        // سازنده دقیقاً با پارامترهای TrainingTask شما تنظیم شده است
        owner.getTrainingQueue().add(new TrainingTask(type, owner, startTime, gameSpeed));
        System.out.println("⏳ Training " + type + " added to queue.");
        return true;
    }

    public MyLinkedList<String> updateTrainingQueue() {
        MyLinkedList<String> messages = new MyLinkedList<>();
        MyLinkedList<TrainingTask> queue = owner.getTrainingQueue();

        if (queue != null && !queue.isEmpty()) {
            TrainingTask task = queue.getHead().data;
            if (!task.isStarted()) {
                task.start();
                // به جای چاپ، پیام را به لیست اضافه می‌کنیم
                messages.add("SYSTEM::Training for " + task.getType() + " has started.");
            }

            if (task.isCompleted()) {
                Result result = task.finishTraining();
                // پیام بازگشتی از finishTraining را به لیست اضافه می‌کنیم
                messages.add(result.getMessage());

                // وظیفه را فقط در صورت موفقیت (پیدا کردن کمپ) حذف نکنید، بلکه همیشه حذف کنید
                queue.remove(task);
            }
        }
        return messages;
    }

    public void printTrainingQueueStatus() {
        final String RESET = "\u001B[0m";
        final String GREEN = "\u001B[32m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String GRAY = "\u001B[90m";

        MyLinkedList<TrainingTask> queue = owner.getTrainingQueue();
        if (queue.isEmpty()) {
            System.out.println("📭 " + GRAY + "Training queue is empty." + RESET);
            return;
        }

        System.out.println("📋 " + CYAN + "Training Queue:" + RESET);
        for (TrainingTask task : queue) {
            long remainingMillis = task.getRemainingMillis();
            String status;
            String color;

            if (task.isCompleted()) {
                status = "✅ Completed";
                color = GREEN;
            } else if (task.isStarted()) {
                long totalSeconds = remainingMillis / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                String timeStr = String.format("%02dm %02ds", minutes, seconds);
                status = "⏳ Remaining: " + timeStr;
                color = YELLOW;
            } else {
                status = "🕓 Waiting to start";
                color = GRAY;
            }

            System.out.println(" - " + task.getType() + " → " + color + status + RESET);
        }
    }

    private Camp getCampFromKingdom() {
        for (Building b : owner.getBuildings()) {
            if (b instanceof Camp && b.isActiveBuilding()) {
                return (Camp) b;
            }
        }
        return null;
    }

    @Override
    public boolean upgrade(int gameSpeed) {
        System.out.println("🏋️‍♂️ Barrack start to upgrade to level " + level);
        startUpgrade(gameSpeed);
        return true;
    }

    @Override
    public boolean repair(int gameSpeed) {
        System.out.println("🏋️‍♂️ Barrack start to repair " );
        startRepair(gameSpeed);
        return true;
    }

    @Override
    public int getRawRepairTime() {
        return GameBalance.getMilitaryRepairTime("BARRACK", this.level);
    }
    @Override
    public int getRawUpgradeTime() {
        return GameBalance.getMilitaryBuildTime("BARRACK", this.level);
    }
    @Override
    public int getRawBuildTime() {
        return GameBalance.getMilitaryBuildTime("BARRACK", this.level);
    }
    public Kingdom getKingdom() {
        return owner;
    }
}
