package com.yourname.got.logic.model;

import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.model.Building.WallsOfGate;
import com.yourname.got.logic.util.MapArrangement;

import java.io.Serializable;

public class UserSubMap implements Serializable {
    public Map mainMap;
    private int startX, startY, width, length;
    protected MapArrangement arrangement;
    private static final long serialVersionUID = -6905529238046766289L;

    public UserSubMap(Map mainMap, int startX, int startY, int width, int length,Position position) {
        this.mainMap = mainMap;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.length = length;
        arrangement=new MapArrangement();
    }


    public Tile getTile(int localX, int localY) {
        if (!isInsideBounds(localX, localY)) return null;
        return mainMap.getMap()[startX + localX][startY + localY];
    }

    public boolean isInsideBounds(int localX, int localY) {
        return localX >= 0 && localY >= 0 && localX < width && localY < length;
    }

    public int getStartX() {
        return startX;
    }
    public int getStartY() {
        return startY;
    }
    public int getWidth() { return width; }
    public int getLength() { return length; }

    public void printUserMap(){
        for(int i=0;i<width;i++) {
            for (int j=0;j<length;j++){
                System.out.print("|");

                System.out.print(mainMap.getMap()[startX+i][startY+j]);
            }
            System.out.print("|\n");

        }

    }

    public Tile[][] getMapGrid() {
        Tile[][] subMapGrid = new Tile[width][length];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < length; j++) {
                subMapGrid[i][j] = getTile(i, j);
            }
        }
        return subMapGrid;
    }

    public  Tile selectTile( int x, int y){
        if(!isInsideBounds(x,y)) {
            System.out.println("(x,Y) OUT OF YOUR MAP");
            return  null;
        }
        else if (mainMap.getMap()[startX+x][startY+y].isEmpty()) {
            System.out.println("this location is empty");
            return  null;
        }
        else {
            return mainMap.getMap()[startX+x][startY+y];
        }

    }
// در UserSubMap.java:

    public Building getBuildingAt(int localX, int localY) {
        // --- رفع باگ عدم سینک: استفاده از مختصات لوکال تبدیل شده به تایل ---
        Tile tile = getTile(localX, localY);
        if (tile != null && !tile.isEmpty() && tile.getObjectOnTile() != null) {
            return tile.getObjectOnTile().getBuilding();
        }
        return null;
    }


    public boolean addBuildingInMap(Building building, int localX, int localY) {
        int buildableMin = 1;
        int buildableMax = 11;

        if (localX < buildableMin || localY < buildableMin || localX > buildableMax || localY > buildableMax) {
            System.err.println("ERROR: Coordinates (" + localX + ", " + localY + ") are out of the buildable area.");
            return false;
        }

        int globalX = startX + (localX );
        int globalY = startY + (localY );
        Tile targetTile = mainMap.getMap()[globalX][globalY];

        if (targetTile.getObjectOnTile() != null && targetTile.getObjectOnTile().getBuilding() != null) {
            System.err.println("ERROR: A building already exists at this location.");
            return false;
        }

        targetTile.getObjectOnTile().setBuilding(building);
        building.setPosition(globalX, globalY);
        targetTile.setEmpty(false);

        System.out.println("INFO: Building placed at global map coordinates: " + globalX + "/" + globalY);
        return true;
    }

    public Position globalToLocal(Position globalPosition) {
        if (globalPosition == null) return null;

        int localX = globalPosition.getX() - this.startX;
        int localY = globalPosition.getY() - this.startY;

        if (localX >= 0 && localX < this.width && localY >= 0 && localY < this.length) {
            return new Position(localX, localY);
        } else {
            return null;
        }
    }

    public Position localToGlobal(Position localPosition) {
        if (localPosition == null) return null;

        int globalX = localPosition.getX() + this.startX;
        int globalY = localPosition.getY() + this.startY;

        return new Position(globalX, globalY);
    }
    public Boolean changeTilePosition(Tile tile, int x, int y) {
        if (!isInsideBounds(x, y)) {
            System.out.println("(x, Y) OUT OF YOUR MAP");
            return false;
        }
        // same buildable area as addBuildingInMap (the ring around it belongs to walls/gate)
        if (x < 1 || y < 1 || x > 11 || y > 11) {
            System.out.println("this location is outside the buildable area");
            return false;
        }

        int mapX = startX + x;
        int mapY = startY + y;
        Tile dest = mainMap.getMap()[mapX][mapY];

        if (!dest.isEmpty() || dest.getObjectOnTile().building != null) {
            System.out.println("this location is not empty");
            return false;
        }

        Building b = (tile == null || tile.getObjectOnTile() == null) ? null : tile.getObjectOnTile().building;
        if (b == null) {
            System.out.println("there is no building to move");
            return false;
        }
        if (b instanceof WallsOfGate || b instanceof Castle || b instanceof Gate || b instanceof Port) {
            System.out.println("this building is not moveable");
            return false;
        }

        // Move the BUILDING between the two tiles. The old code swapped the Tile objects inside the map
        // array and shared one Position object between them, so after a move the empty tile believed it
        // was at the new coordinates and the building kept its old coordinates: buildings showed up on
        // empty tiles and clicks pointed at things that were not there.
        tile.getObjectOnTile().setBuilding(null);
        tile.setEmpty(true);

        dest.getObjectOnTile().setBuilding(b);
        dest.setEmpty(false);
        b.setPosition(mapX, mapY);
        return true;
    }

    public boolean canPlaceBuilding(int x, int y) {
        if (!isInsideBounds(x, y)) return false;

        Tile tile = getTile(x, y);
        return tile.isEmpty() && tile.getObjectOnTile().building == null;
    }

    public void markTileAsUnavailable(int x, int y) {
        if (isInsideBounds(x, y)) {
            Tile tile = getTile(x, y);
            tile.setEmpty(false);
            tile.getObjectOnTile().setBuilding(null);
            System.out.println("🔒 مختصات " + x + "," + y + " برای ساخت قفل شد.");
        }
    }

    public boolean removeBuilding(int x, int y) {
        int buildableWidth = 11;
        int buildableLength = 11;

        if(!isInsideBounds(x,y)){
            System.out.println("⛔️ مختصات خارج از محدوده ساخت‌وساز (روی دیوار یا گیت) است.");
            return false;
        }

        int mapX = startX + x ;
        int mapY = startY + y ;

        Tile tile = mainMap.getMap()[mapX][mapY];
        Building targetBuilding = tile.getObjectOnTile().getBuilding();

        if (targetBuilding == null) {
            System.out.println("❌ ساختمانی در این مکان نیست.");
            return false;
        }

        tile.getObjectOnTile().setBuilding(null);
        tile.setEmpty(true);
        return true;
    }
}
