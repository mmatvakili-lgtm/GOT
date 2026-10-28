package com.yourname.got.logic.model;

import com.yourname.got.logic.enums.TerrainType;
import com.yourname.got.logic.model.Tile;
import com.yourname.got.logic.util.Result;

import java.io.Serializable;

public class Map implements Serializable {
    private static final long serialVersionUID = 6479092804106831421L;
    public final Tile[][] mainMap;

    public Map(int length, int width) {
        mainMap = new Tile[length][width];
        for (int i = 0; i < mainMap.length; i++)
            for (int j = 0; j < mainMap[0].length; j++)
                mainMap[i][j] = new Tile(i, j);

        int lastRow = length - 1;
        for (int j = 0; j < width; j++) {
            mainMap[lastRow][j].setTerrainType(TerrainType.SEA);
            mainMap[lastRow][j].setObjectOnTile(new ObjectOnTile("Sea"));
        }
        for (int i = 0; i < length; i++) {
            mainMap[i][0].setTerrainType(TerrainType.SEA);
            mainMap[i][0].setObjectOnTile(new ObjectOnTile("Sea"));
        }
        int lastCol = width - 1;
        for (int i = 0; i < length; i++) {
            mainMap[i][lastCol].setTerrainType(TerrainType.SEA);
            mainMap[i][lastCol].setObjectOnTile(new ObjectOnTile("Sea"));
        }
        for (int i = 1; i < 15 && i < length; i++) {
            for (int j = 0; j < 31 && j < width; j++) {
                if (j < 9 || (j > 21 && j < 31)) {
                    mainMap[i][j].setTerrainType(TerrainType.SEA);
                    mainMap[i][j].setObjectOnTile(new ObjectOnTile("Sea"));
                }
            }
        }
    }

    public Result isEmpty(int x, int y) {
        if (x < 0 || y < 0 || x >= mainMap.length || y >= mainMap[0].length) {
            return new Result(false, "Coordinates (" + x + "," + y + ") are out of map range.");
        }
        boolean empty = this.mainMap[x][y].isEmpty();
        return new Result(empty, empty ? "Tile is empty" : "Tile is not empty", mainMap[x][y]);
    }

    public void printMap() {
        for (int i = 0; i < mainMap.length; i++) {
            System.out.println("Row " + i + ":");
            for (int j = 0; j < mainMap[0].length; j++) {
                System.out.print("|" + mainMap[i][j]);
            }
            System.out.print("|\n");
        }
    }


    public Tile[][] getMap() {
        return this.mainMap;
    }

    public Result placeWallAtTopRow() {
        for (int j = 0; j < mainMap[0].length; j++) {
            Tile tile = mainMap[0][j]; // first row
            tile.setEmpty(false);
            tile.setObjectOnTile(new ObjectOnTile("Wall"));
        }
        return new Result(true, "The Great Wall has been placed at the north of the map.");
    }

}
