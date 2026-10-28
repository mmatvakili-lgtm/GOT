package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType; // Import SoldierType
import java.io.Serializable;

public class Spy extends Soldier implements Serializable {
    public Spy() { super(SoldierType.SPY); }
}

