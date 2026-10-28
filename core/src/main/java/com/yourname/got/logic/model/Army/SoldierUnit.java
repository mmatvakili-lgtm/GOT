package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;

import java.io.Serializable;


public class SoldierUnit implements Serializable {
    private static final long serialVersionUID =3521364121713578507L;

    private SoldierType type;
    private MyLinkedList<Soldier> soldiers;
    public SoldierUnit(SoldierType type) {
        this.type = type;
        this.soldiers = new MyLinkedList<>();
    }

    public Boolean addSoldier(Soldier s) {
        if (s.getType()==type) {
            soldiers.add(s);
            return true;
        }
        else
            System.out.println("نوع سرباز اشتباه است");
        return false;

    }

    public boolean removeSoldier(Soldier s) {
        return soldiers.remove(s);
    }

    public int getCount() {
        return soldiers.getSize();
    }

    public int getTotalAttackPower() {
        int sum = 0;
        Node<Soldier> current = soldiers.getHead();
        while (current != null) {
            sum += current.data.getAttackPower();
            current = current.next;
        }
        return sum;
    }
    public int  getTotalDamage(int damage ){
        while (damage > 0 && !soldiers.isEmpty()) {
            if(soldiers.getHead().data.getHealth()<=damage) {
                damage-=soldiers.getHead().data.getHealth();
                soldiers.removeFirst();
            }
            else
            {soldiers.getHead().data.setDamage(damage);
                damage=0;
            }
        }
        return damage;
    }
    public void addSoldiers(MyLinkedList<Soldier> soldiersToAdd) {
        if (soldiersToAdd == null || soldiersToAdd.isEmpty()) {
            return;
        }

        for (Soldier soldier : soldiersToAdd) {
            this.addSoldier(soldier);
        }
    }
    public SoldierType getType() { return type; }

    public MyLinkedList<Soldier> getSoldiers() {
        return soldiers;
    }

    public int getAdjustedAttackPowerAgainstWhiteWalkers() {
        int totalPower = 0;
        Node<Soldier> current = soldiers.getHead();
        while (current != null) {
            Soldier s = current.data;
            if (s != null) {
                if (s.isEquippedWithDragonGlass()) {
                    totalPower += s.getAttackPower();
                } else {
                    totalPower += (int)(s.getAttackPower() * 0.6); // بدون DragonGlass با 60٪ قدرت
                }
            }
            current = current.next;
        }
        return totalPower;
    }

    public void applyHealthReductionPercentage(int percentage) {
        if (soldiers == null || soldiers.isEmpty()) {
            return;
        }
        for (Soldier soldier : soldiers) {
            soldier.reduceHealthByPercentage(percentage);
        }
    }

    public void applyAttackPowerReductionPercentage(int percentage) {
        if (soldiers == null || soldiers.isEmpty()) {
            return;
        }
        for (Soldier soldier : soldiers) {
            soldier.reduceAttackPowerByPercentage(percentage);
        }
    }
    public int countEquippedWithDragonGlass() {
        int count = 0;
        Node<Soldier> current = soldiers.getHead();
        while (current != null) {
            if (current.data.isEquippedWithDragonGlass()) count++;
            current = current.next;
        }
        return count;
    }

    @Override
    public String toString() {
        return "Unit of " + type + " - " + getCount() + " soldiers (Total AP: " + getTotalAttackPower() + ")";
    }


    public String toDisplayString(KingdomType contextKingdom) {
        String soldierName;
        if (contextKingdom != null) {
            soldierName = KingdomSoldierRegistry.resolveDisplayName(contextKingdom, type);
        } else {
            soldierName = KingdomSoldierRegistry.resolveDisplayNameOrBase(type);
        }
        return getCount() + " " + soldierName + " (Total AP: " + getTotalAttackPower() + ")";
    }

    public int getTotalHealth() {
        int sum = 0;
        if (soldiers == null) {
            return 0;
        }

        for (Soldier soldier : soldiers) {
            sum += soldier.getHealth();
        }
        return sum;
    }

    public void applyAttackBuff(double multiplier) {
        if (soldiers == null) return;

        // دسترسی به نودهای لیست پیوندی (چون MyLinkedList دارید)
        Node<Soldier> current = soldiers.getHead();
        while (current != null) {
            Soldier s = current.data;
            if (s != null) {
                // محاسبه قدرت جدید
                int newPower = (int) (s.getAttackPower() * multiplier);
                s.setAttackPower(newPower);
            }
            current = current.next;
        }
    }



}
