package com.yourname.got.logic.model.Building;

import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.Soldier;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.util.MyLinkedList;

import java.io.Serializable;

public class DefenseComplex implements Serializable {
    private static final long serialVersionUID = 3167851659003966893L;

    public Gate gate;
    public MyLinkedList<WallsOfGate> wall;
    public SoldierUnit defensiveArchers;
    public  Port port;

    public DefenseComplex() {
        this.wall = new MyLinkedList<>();
        this.defensiveArchers = new SoldierUnit(SoldierType.ARCHER);
    }

    public Building getPrimaryTargetFor(AttackType attackType) {
        if (attackType == AttackType.SEA) {
            return this.port;
        } else {
            return this.gate;
        }
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }

    public DefenseComplex(MyLinkedList<WallsOfGate> wall, Gate gate) {
        this.wall = new MyLinkedList<>();
        this.gate = gate;
        this.defensiveArchers = new SoldierUnit(SoldierType.ARCHER);
    }

    public int getTotalHealth(com.yourname.got.logic.enums.AttackType attackType) {
        int total = 0;
        if (wall != null) {
            for (WallsOfGate w : wall) {
                if (w != null && !w.isDestroyed()) {
                    total += w.getHealth();
                }
            }
        }
        if (attackType == com.yourname.got.logic.enums.AttackType.SEA) {
            if (port != null && !port.isDestroyed()) total += port.getHealth();
        } else {
            if (gate != null && !gate.isDestroyed()) total += gate.getHealth();
        }
        return total;
    }

    public void applyDamage(AttackType attackType, int damage) {
        if (damage <= 0) {
            return;
        }

        if (wall != null && !wall.isEmpty()) {
            System.out.println("  -> آسیب ابتدا به دیوارها وارد می‌شود...");
            for (WallsOfGate w : wall) {
                if (damage <= 0) {
                    break;
                }
                if (w == null || w.isDestroyed()) {
                    continue;
                }

                int damageToWall = Math.min(damage, w.getHealth());
                w.takeDamage(damageToWall);
                damage -= damageToWall;

                if (w.isDestroyed()) {
                    System.out.println("    - دیوار در موقعیت " + w.getPosition() + " نابود شد.");
                }
            }
        }

        if (damage > 0) {

            Building primaryTarget = getPrimaryTargetFor(attackType);
            if (primaryTarget != null && !primaryTarget.isDestroyed()) {
                System.out.println("  -> آسیب باقی‌مانده (" + damage + ") به " + primaryTarget.getName() + " وارد می‌شود.");
                primaryTarget.takeDamage(damage);

                if (primaryTarget.isDestroyed()) {
                    System.out.println("    - هدف اصلی، " + primaryTarget.getName() + "، نابود شد!");
                }
            } else {
                System.out.println("  -> آسیب باقی‌مانده دفع شد زیرا هدف اصلی قبلاً نابود شده است.");
            }
        } else {
            System.out.println("  -> تمام آسیب توسط دیوارها جذب شد.");
        }
    }

    public void destroyComplex() {
        System.out.println("The Defense Complex has been breached!");
        if (gate != null) {
            gate.setStatus(Status.destroyed);
            gate.setStrength(0);
        }
        if (port != null) {
            port.setStatus(Status.destroyed);
        }
        if (wall != null) {
            for (WallsOfGate w : wall) {
                if (w != null) {
                    w.setStatus(Status.destroyed);
                    w.setStrength(0);
                }
            }
        }
    }

    public int getTotalAttackPower() {
        if (defensiveArchers == null) {
            return 0;
        }
        return defensiveArchers.getTotalAttackPower();
    }

    public boolean isDestroyed(AttackType attackType) {
        Building primaryTarget = getPrimaryTargetFor(attackType);

        return primaryTarget == null || primaryTarget.isDestroyed();
    }

    public void setWall(MyLinkedList<WallsOfGate> wall) {
        this.wall = wall;
    }

    public void addGate(WallsOfGate wallsOfGate) {
        this.wall.add(wallsOfGate);
    }

    public Gate getGate() {
        return gate;
    }

    public void setGate(Gate gate) {
        this.gate = gate;
    }

    public void setWall(WallsOfGate wall){
        this.wall.add(wall);
    }

    public Boolean addDefensiveArcherUnit(Soldier soldier) {
        if (soldier.getType() == com.yourname.got.logic.enums.SoldierType.ARCHER) {
            defensiveArchers.addSoldier(soldier);
            return true;
        } else {
            System.out.println("نوع سرباز اشتباه است");
            return false;
        }
    }

    public SoldierUnit getDefensiveArchers() {
        return defensiveArchers;
    }

    public MyLinkedList<WallsOfGate> getWall() {
        return wall;
    }
}
