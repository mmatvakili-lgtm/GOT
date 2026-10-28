package com.yourname.got.view.render;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.yourname.got.logic.model.Tile;

import java.util.HashMap;

public class TileRenderer {
    private final HashMap<String, Texture> textures;

    public TileRenderer() {
        textures = new HashMap<>();
        textures.put("Castle", new Texture("tiles/castle.png"));
        textures.put("Port", new Texture("tiles/port.png"));
        textures.put("Gate", new Texture("tiles/gate.png"));
        textures.put("Wall", new Texture("tiles/wall.png"));
        textures.put("WallsOfGate", new Texture("tiles/wall.png"));
        textures.put("Sea", new Texture("tiles/sea.png"));
        textures.put("Land", new Texture("tiles/land.png"));
        textures.put("Default", new Texture("tiles/default.png"));
    }

    public void drawTile(SpriteBatch batch, Tile tile, int x, int y, int width, int height) {
        String key = "Land";

        if (tile.getObjectOnTile() != null && tile.getObjectOnTile().getBuilding() != null) {
            key = tile.getObjectOnTile().getBuilding().getName();
        } else if (tile.getTerrainType() != null && tile.getTerrainType().name().equals("SEA")) {
            key = "Sea";
        }

        Texture texture = textures.getOrDefault(key, textures.get("Default"));
        batch.draw(texture, x, y, width, height);
    }

    public void dispose() {
        for (Texture t : textures.values()) {
            t.dispose();
        }
    }
}

