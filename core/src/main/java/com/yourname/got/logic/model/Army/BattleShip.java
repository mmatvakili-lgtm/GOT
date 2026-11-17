package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.model.Moveable;
import java.io.Serializable;
public class BattleShip implements Moveable, Serializable {
    public int soldiersCapacity;
    public SoldierUnit[] Troops;
    public double Speed;
    public int attackPower;
    public int level = 1;
    public boolean isRented = false;
    private boolean isAvailable = true;

    public BattleShip(boolean isRented) {
        this.soldiersCapacity = 100;
        this.Speed = 50;
        this.attackPower = 8000;
        this.isRented = isRented;
        this.level = 1;
    }

    public BattleShip() {
        this.soldiersCapacity = 150;
        this.Speed = 60;
        this.attackPower = 6000;
        this.isRented = false;
        this.level = 1;
    }
    public boolean isAvailable() {
        return this.isAvailable;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
    public void upgrade() {
        this.level++;
        this.soldiersCapacity += 50; // was += 5000 (typo): one upgrade made the ship carry 5150 soldiers
        this.attackPower += 150;
    }
}
