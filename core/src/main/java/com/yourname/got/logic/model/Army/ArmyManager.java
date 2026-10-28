package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;
import com.yourname.got.logic.util.Result;

import java.io.Serializable;

public class ArmyManager implements Serializable {
    private static final long serialVersionUID = 7862328221019909282L;
    protected MyLinkedList<Soldier> allSoldiers;
    protected DefenceManager defenceManager;
    protected  Double soldierSpeed;

    public static class TroopRequest {
        public SoldierType type;
        public int count;

        public TroopRequest(SoldierType type, int count) {
            this.type = type;
            this.count = count;
        }
    }
    public ArmyManager() {
        allSoldiers = new MyLinkedList<>();
        defenceManager = new DefenceManager();
        soldierSpeed=1.0;
    }

    public Double getSoldierSpeed() {
        return soldierSpeed;
    }

    public boolean recruit(Soldier s) {

        allSoldiers.add(s);

        SoldierUnit unitInDefenceManagerDefenders = defenceManager.findOrCreateUnitInDefenders(s.getType());
        unitInDefenceManagerDefenders.addSoldier(s);

        System.out.println("✅ " + s.getType() + " استخدام شد.");
        return true;
    }

    public void printDefenders() {
        System.out.println("Defending Units (ArmyManager):");
        printAllSoldiers();
        System.out.println("Defending Units (DefenceManager):");
        defenceManager.getDefenders().printAll();
    }

    private SoldierUnit findUnit(MyLinkedList<SoldierUnit> list, SoldierType type) {
        Node<SoldierUnit> current = list.getHead();
        while (current != null) {
            if (current.data.getType().equals(type))
                return current.data;
            current = current.next;
        }
        return null;
    }

    private SoldierUnit findOrCreateUnit(MyLinkedList<SoldierUnit> list, SoldierType type) {
        SoldierUnit unit = findUnit(list, type);
        if (unit == null) {
            unit = new SoldierUnit(type);
            list.add(unit);
        }
        return unit;
    }


    public void returnSurvivorsToDefence(SoldierUnit[] survivingAttackers) {
        if (survivingAttackers == null) {
            return;
        }

        System.out.println("... در حال بازگرداندن بازماندگان حمله به ارتش اصلی ...");

        for (SoldierUnit survivorUnit : survivingAttackers) {
            if (survivorUnit != null && survivorUnit.getCount() > 0) {

                SoldierUnit unitInDefence = this.defenceManager.findOrCreateUnitInDefenders(survivorUnit.getType());

                unitInDefence.addSoldiers(survivorUnit.getSoldiers());

                System.out.println("  + " + survivorUnit.getCount() + " عدد " + survivorUnit.getType() + " به ارتش فعال بازگشتند.");
            }
        }
    }

    public void reconcileAllSoldiers() {
        System.out.println("... همگام‌سازی لیست کل سربازان پادشاهی:   ...");
        MyLinkedList<Soldier> allSurvivingSoldiers = new MyLinkedList<>();

        MyLinkedList<SoldierUnit> defendingUnits = this.defenceManager.getDefenders();
        if (defendingUnits != null) {
            for (SoldierUnit unit : defendingUnits) {
                if (unit != null && unit.getCount() > 0) {
                    allSurvivingSoldiers.addAll(unit.getSoldiers());
                }
            }
        }

        SoldierUnit garrisonArchers = this.defenceManager.getDefenseComplex().getDefensiveArchers();
        if (garrisonArchers != null && garrisonArchers.getCount() > 0) {
            allSurvivingSoldiers.addAll(garrisonArchers.getSoldiers());
        }

        // --- اصلاح بسیار مهم: فقط بازمانده‌ها در لیست کل ذخیره می‌شوند ---
        // این کار باعث می‌شود ظرفیت خالی شده و Camp متوجه فضای جدید شود
        this.allSoldiers = allSurvivingSoldiers;

        // همگام سازی ظرفیت با خود کمپ‌ها (چون کمپ ظرفیت را بر اساس trainedSoldiers.getSize می‌سنجد)
        // از آنجا که ما به owner دسترسی مستقیم در اینجا نداریم، منطق را در Camp اصلاح می‌کنیم (پایین).
        System.out.println("✅ همگام‌سازی کامل شد. تعداد کل سربازان باقی‌مانده: " + this.allSoldiers.getSize());
    }

    public DefenceManager getDefenceManager() {
        return defenceManager;
    }


    public int getTotalSoldier(){
        int total = 0;
        for (Soldier s : allSoldiers) {
            total++;
        }
        return total;
    }

    public boolean hasEnoughSoldiers(SoldierType type, int requiredCount) {
        SoldierUnit unit = defenceManager.findUnitInDefenders(type);
        if (unit != null) {
            return unit.getCount() >= requiredCount;
        }
        return false;
    }

    public SoldierUnit removeAndPackageSoldiers(SoldierType type, int count) {
        if (!hasEnoughSoldiers(type, count)) {
            return null;
        }

        // واحد منبع در ارتش دفاعی را پیدا کن
        SoldierUnit sourceUnit = defenceManager.findUnitInDefenders(type);

        // یک واحد جدید و خالی برای بسته‌بندی سربازان فروخته شده بساز
        SoldierUnit packageUnit = new SoldierUnit(type);

        // به تعداد مشخص شده، سربازان را از واحد منبع به واحد بسته‌بندی منتقل کن
        for (int i = 0; i < count; i++) {
            if (!sourceUnit.getSoldiers().isEmpty()) {
                Soldier soldierToMove = sourceUnit.getSoldiers().getHead().data;
                sourceUnit.removeSoldier(soldierToMove); // حذف از ارتش دفاعی
                packageUnit.addSoldier(soldierToMove);   // افزودن به بسته فروشی

                // **مهم:** حذف از لیست کلی allSoldiers
                this.allSoldiers.remove(soldierToMove);
            }
        }

        return packageUnit;
    }


    public Result trainSoldiers(SoldierType type, int quantity, Kingdom kingdom, int gameSpeed) {
        if (quantity <= 0) {
            return new Result(false, "Quantity must be positive.");
        }

        int totalWood = type.woodCost * quantity;
        int totalFood = type.foodCost * quantity;
        int totalCoin = type.coinCost * quantity;

        if (!kingdom.canAffordResources(totalWood, 0, totalCoin, totalFood)) {
            return new Result(false, "Not enough resources to train " + quantity + " " + type.name());
        }

        kingdom.detuctResources(totalWood, 0, totalCoin, totalFood);

        TrainingTask task = new TrainingTask(type,kingdom, quantity, gameSpeed);
        kingdom.getTrainingQueue().add(task);

        return new Result(true, "Training for " + quantity + " " + type.name() + " has been added to the queue.");
    }
    public void addSoldierPackage(SoldierUnit purchasedUnit) {
        if (purchasedUnit == null || purchasedUnit.getCount() <= 0) {
            return;
        }

        System.out.println("... در حال اضافه کردن یگان خریداری شده به ارتش ...");

        SoldierUnit unitInDefence = defenceManager.findOrCreateUnitInDefenders(purchasedUnit.getType());

        unitInDefence.addSoldiers(purchasedUnit.getSoldiers());

        for(Soldier s : purchasedUnit.getSoldiers()) {
            this.allSoldiers.add(s);
        }

        System.out.println("  + " + purchasedUnit.getCount() + " عدد " + purchasedUnit.getType() + " با موفقیت به ارتش اضافه شد.");
    }

    public  void printAllSoldiers(){
        System.out.println("NUMBER SOLDIERS:"+allSoldiers.getSize());
    }
}
