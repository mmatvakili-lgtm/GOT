package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Building.DefenseComplex;
import com.yourname.got.logic.model.Building.DefensiveTower;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;

import java.io.Serializable;

public class DefenceManager implements Serializable {
    protected MyLinkedList<DefensiveTower> towers;
    protected MyLinkedList<SoldierUnit> defenders;
    public DefenseComplex DefenseComplex;
    private double SoldierEfficiency;

    public DefenceManager() {
        this.towers = new MyLinkedList<>();
        this.defenders = new MyLinkedList<>();
        this.DefenseComplex = new DefenseComplex();
        this.SoldierEfficiency = 1.0;
    }

    public MyLinkedList<SoldierUnit> getDefenders() {
        return defenders;
    }

    public MyLinkedList<DefensiveTower> getTowers() {
        return towers;
    }


    public DefenseComplex getDefenseComplex() {
        return DefenseComplex;
    }

    public void setSoldierEfficiency(double efficiency) {
        this.SoldierEfficiency = efficiency;
    }

    public double getSoldierEfficiency() {
        return SoldierEfficiency;
    }


    public SoldierUnit findUnitInDefenders(SoldierType type) {
        Node<SoldierUnit> current = defenders.getHead();
        while (current != null) {
            if (current.data.getType().equals(type)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    public SoldierUnit findOrCreateUnitInDefenders(SoldierType type) {
        SoldierUnit unit = findUnitInDefenders(type);
        if (unit == null) {
            unit = new SoldierUnit(type);
            defenders.add(unit);
        }
        return unit;
    }
}
