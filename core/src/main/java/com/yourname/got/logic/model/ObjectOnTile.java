package com.yourname.got.logic.model;

import com.yourname.got.logic.model.Army.BattleShip;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.WhiteWalker.*;

import java.io.Serializable;

public class ObjectOnTile implements Serializable {
    protected Building building;
    protected SoldierUnit[] military;
    protected BattleShip ship;
    protected Wall wall;
    protected String type;

    public ObjectOnTile() {
        ship = null;
        building = null;
        military = null;
        wall = null;
        type = "";
    }

    public ObjectOnTile(String type) {
        this();
        this.type = type;
    }

    public BattleShip getShip() { return ship; }
    public Building getBuilding() { return building; }
    public SoldierUnit[] getMilitary() { return military; }
    public Wall getWall() { return wall; }
    public String getType() { return type; }

    public void setBuilding(Building building) {
        this.building = building;
        this.type = "Building";
    }

    public void setMilitary(SoldierUnit[] military) {
        this.military = military;
        this.type = "Military";
    }

    public void setWall(Wall wall) {
        wall = wall;
        this.type = "Wall";
    }

    @Override
    public String toString() {
        return building.toString()+military.toString()+ship.toString();
    }
}


