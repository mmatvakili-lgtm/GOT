package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;

import java.io.Serializable;

public class Archer extends Soldier implements Serializable {
    public Archer() {
        super(SoldierType.ARCHER);
    }
}
