package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Building.*;

class BuildingMoveTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    static int badTiles(Map m) {
        int bad = 0; Tile[][] g = m.getMap();
        for (int x = 0; x < g.length; x++) for (int y = 0; y < g[x].length; y++) {
            Tile t = g[x][y]; if (t == null || t.getPosition() == null) continue;
            if (t.getPosition().getX() != x || t.getPosition().getY() != y) bad++;
        }
        return bad;
    }
    @Test
    void movingABuildingKeepsMapConsistent() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("t14", 1, u1); g.addPlayer(u2); g.startGame();
        Kingdom k = u1.getKingdom(); UserSubMap um = k.getUserMap();
        Mine mine = new Mine("WOOD", k); mine.markAsBuilt();
        check(um.addBuildingInMap(mine, 3, 3), "mine placed at local (3,3)");
        check(badTiles(g.getMap()) == 0, "map consistent before the move");
        Tile src = um.getTile(3, 3);
        boolean moved = um.changeTilePosition(src, 5, 6);
        check(moved, "mine moved to (5,6)");
        check(um.getBuildingAt(3, 3) == null && um.getTile(3, 3).isEmpty(), "old tile (3,3) is empty again");
        check(um.getBuildingAt(5, 6) == mine, "building is on the new tile (5,6)");
        Position p = mine.getPosition();
        check(p.getX() == um.getStartX() + 5 && p.getY() == um.getStartY() + 6, "building's own coordinates follow the move (" + p.getX() + "," + p.getY() + ")");
        check(badTiles(g.getMap()) == 0, "every tile still knows its own coordinates (bad tiles: " + badTiles(g.getMap()) + ")");
        check(!um.changeTilePosition(um.getTile(5, 6), 5, 6), "cannot move onto an occupied tile");
        Check.assertAll();
    }
}
