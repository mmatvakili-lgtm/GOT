package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;
import java.io.Serializable;

public class Cavalry extends Soldier implements Serializable {
    public Cavalry() {
        super(SoldierType.CAVALRY);
    }
}
