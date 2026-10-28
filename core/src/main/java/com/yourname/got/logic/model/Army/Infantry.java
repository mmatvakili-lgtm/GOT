package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;

import java.io.Serializable;

public class Infantry extends Soldier implements Serializable {
    public Infantry() {
        super(SoldierType.INFANTRY);
    }
}
