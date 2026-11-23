package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.EventBattleSetup;
import com.yourname.got.logic.util.EventBuildingDestroyed;
import com.yourname.got.logic.util.EventBuildingFocus;

import java.util.HashMap;
import java.util.Map;

/**
 * A small map of the defender's kingdom for the battle replay. It is driven by the structured battle events:
 * the setup draws the buildings, a focus event highlights the building the attackers are going for, and a
 * destroyed event darkens it, marks it with an X and floats the loot text.
 *
 * Tile coordinates are the kingdom-local ones used everywhere else: x = row, y = column.
 */
public class BattlefieldView extends Group {
    /** Gives the icon of a building (the game screen passes its cached building drawables). */
    public interface IconProvider {
        Drawable icon(String buildingName, int level);
    }

    private static final int CELLS = 13;

    private final Skin skin;
    private final IconProvider icons;
    private final float cell;
    private final Map<String, Image> pieces = new HashMap<>();
    private Image focus;

    public BattlefieldView(Skin skin, IconProvider icons, float cell) {
        this.skin = skin;
        this.icons = icons;
        this.cell = cell;
        setSize(CELLS * cell, CELLS * cell);
        setTouchable(Touchable.disabled);

        Image ground = new Image(skin.newDrawable("white", new Color(0.16f, 0.30f, 0.14f, 1f)));
        ground.setSize(getWidth(), getHeight());
        addActor(ground);
        for (int i = 0; i <= CELLS; i++) { // faint grid lines, like the real map
            Image vertical = new Image(skin.newDrawable("white", new Color(1f, 1f, 1f, 0.08f)));
            vertical.setBounds(i * cell, 0f, 1f, getHeight());
            addActor(vertical);
            Image horizontal = new Image(skin.newDrawable("white", new Color(1f, 1f, 1f, 0.08f)));
            horizontal.setBounds(0f, i * cell, getWidth(), 1f);
            addActor(horizontal);
        }
    }

    public void handle(BattleEvent event) {
        if (event instanceof EventBattleSetup setup) {
            setup(setup);
        } else if (event instanceof EventBuildingFocus f) {
            focus(f.x, f.y);
        } else if (event instanceof EventBuildingDestroyed d) {
            destroyed(d);
        }
    }

    private float px(int y) { return y * cell; }
    private float py(int x) { return (CELLS - 1 - x) * cell; }
    private static String key(int x, int y) { return x + "," + y; }

    private void setup(EventBattleSetup setup) {
        for (Image old : pieces.values()) old.remove();
        pieces.clear();
        clearFocus();
        for (EventBattleSetup.Piece p : setup.pieces) {
            Drawable icon = icons.icon(p.name, p.level);
            Image image = new Image(icon != null ? icon : skin.newDrawable("white", Color.GRAY));
            image.setSize(cell, cell);
            image.setPosition(px(p.y), py(p.x));
            addActor(image);
            pieces.put(key(p.x, p.y), image);
        }
    }

    private void clearFocus() {
        if (focus != null) {
            focus.remove();
            focus = null;
        }
    }

    private void focus(int x, int y) {
        clearFocus();
        focus = new Image(skin.newDrawable("white", new Color(1f, 0.9f, 0.2f, 0.55f)));
        focus.setBounds(px(y) - 2f, py(x) - 2f, cell + 4f, cell + 4f);
        focus.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.2f, 0.35f), Actions.alpha(0.9f, 0.35f))));
        addActor(focus);
    }

    private void destroyed(EventBuildingDestroyed d) {
        clearFocus();
        Image piece = pieces.get(key(d.x, d.y));
        if (piece != null) {
            piece.setColor(0.4f, 0.12f, 0.12f, 0.85f);
        }

        Label cross = new Label("X", skin);
        cross.setColor(1f, 0.3f, 0.25f, 1f);
        cross.pack();
        cross.setPosition(px(d.y) + cell / 2f - cross.getWidth() / 2f, py(d.x) + cell / 2f - cross.getHeight() / 2f);
        addActor(cross);

        if (d.lootAmount > 0) {
            Label loot = new Label("+" + d.lootAmount + " " + d.lootType, skin);
            loot.setFontScale(0.6f);
            loot.setColor(1f, 0.85f, 0.25f, 1f);
            loot.pack();
            loot.setPosition(px(d.y) + cell / 2f - loot.getWidth() / 2f, py(d.x) + cell);
            loot.addAction(Actions.sequence(
                Actions.parallel(Actions.moveBy(0f, 34f, 1.6f), Actions.fadeOut(1.6f)),
                Actions.removeActor()));
            addActor(loot);
        }
    }
}
