package com.yourname.got.logic.model;

import com.yourname.got.logic.enums.TerrainType;

import java.io.Serializable;
public class Tile implements Serializable {
    ObjectOnTile objectOnTile;
    Position position;
    TerrainType terrainType;
    boolean isEmpty;

    @Override
    public String toString() {
        if (objectOnTile != null) {
            if (objectOnTile.building != null) {
                String name = objectOnTile.building.getName();
                switch (name) {
                    case "WallsOfGate": return "⏹️";
                    case "Gate": return "↔️";
                    case "Port": return "⚓️";
                    case "Castle": return "🏰";
                    case "Wall": return "💠";
                    case "Sea": return "🟦";
                    case "Land": return "🟫";
                    default: return "🌾" + name;
                }
            } else if (objectOnTile.getType() != null) {
                switch (objectOnTile.getType()) {
                    case "Wall": return "💠";
                    case "Sea": return "🟦";
                    case "Port": return "⚓️";
                    case "Gate": return "↔️";
                    case "Land": return "🟫";
                    default: return "◻️";
                }
            }
        }

        if (terrainType != null) {
            switch (terrainType) {
                case SEA: return "🟦";
                case LAND: return "🟫";
                case MOUNTAIN: return "⛰️";
                case SNOW: return "❄️";
                default: return "⬜";
            }
        }

        return "◻️";
    }


    public ObjectOnTile getObjectOnTile() {
        return objectOnTile;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setObjectOnTile(ObjectOnTile objectOnTile) {
        this.objectOnTile = objectOnTile;
    }

    public void setEmpty(boolean change) {
        isEmpty = change;
    }

    public void setTerrainType(TerrainType terrainType) {
        this.terrainType = terrainType;
    }
    public  Tile(int x,int y){
        objectOnTile=new ObjectOnTile();
        isEmpty=true;
        terrainType=TerrainType.LAND;
        position=new Position(x,y);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}

