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
import com.yourname.got.logic.util.EventArmyMove;
import com.yourname.got.logic.util.EventArmyState;
import com.yourname.got.logic.util.EventStrike;
import com.badlogic.gdx.math.Interpolation;
import java.util.ArrayList;
import java.util.List;

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

    // ---- armies: a marker with soldier dots for the attackers (it walks) and for the garrison (at the castle) ----
    private static final int MAX_DOTS_PER_UNIT = 4;
    private final ArmyMarker attackers = new ArmyMarker(new Color(1f, 0.55f, 0.15f, 1f));
    private final ArmyMarker garrison = new ArmyMarker(new Color(0.25f, 0.75f, 1f, 1f));
    private int garrisonX = CELLS / 2, garrisonY = CELLS / 2;
    private float speed = 1f;

    /** The replay speed, so the walking animation keeps up with the playback. */
    public void setPlaybackSpeed(float speed) { this.speed = Math.max(0.5f, speed); }

    /** A coloured square with a soldier count and one small dot per few soldiers of every unit type. */
    private final class ArmyMarker extends Group {
        final Color color;
        final Image body;
        final Label count;
        final Map<String, Integer> firstSeen = new HashMap<>();
        final Map<String, List<Image>> dots = new HashMap<>();
        int rows = 0;

        ArmyMarker(Color color) {
            this.color = color;
            setSize(cell, cell);
            setTouchable(Touchable.disabled);
            body = new Image(skin.newDrawable("white", color));
            body.setBounds(cell * 0.1f, cell * 0.1f, cell * 0.8f, cell * 0.8f);
            addActor(body);
            count = new Label("", skin);
            count.setFontScale(0.55f);
            count.setColor(Color.BLACK);
            addActor(count);
            setVisible(false);
        }

        void update(List<EventArmyState.Unit> units) {
            int total = 0;
            for (EventArmyState.Unit u : units) total += u.count;
            count.setText(String.valueOf(total));
            count.pack();
            count.setPosition(cell / 2f - count.getWidth() / 2f, cell / 2f - count.getHeight() / 2f);
            setVisible(total > 0);

            int row = 0;
            for (EventArmyState.Unit u : units) {
                int first = firstSeen.merge(u.type, u.count, Math::max);
                int wanted = Math.max(1, (int) Math.ceil((double) MAX_DOTS_PER_UNIT * u.count / Math.max(1, first)));
                List<Image> list = dots.computeIfAbsent(u.type, k -> new ArrayList<>());
                Color c = ArmyStatusPanel.colorOf(u.type);
                while (list.size() < wanted) {
                    Image dot = new Image(skin.newDrawable("white", c));
                    int j = list.size();
                    dot.setBounds(cell + 1f + j * 6f, cell - 5f - row * 7f, 5f, 5f);
                    addActor(dot);
                    list.add(dot);
                }
                while (list.size() > wanted) { // casualties: a red flash, then the dot is gone
                    Image dot = list.remove(list.size() - 1);
                    dot.setColor(1f, 0.15f, 0.1f, 1f);
                    dot.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.removeActor()));
                }
                row++;
            }
            for (Map.Entry<String, List<Image>> e : dots.entrySet()) { // a unit type that is gone entirely
                boolean alive = false;
                for (EventArmyState.Unit u : units) if (u.type.equals(e.getKey())) alive = true;
                if (!alive) {
                    for (Image d : e.getValue()) {
                        d.setColor(1f, 0.15f, 0.1f, 1f);
                        d.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.removeActor()));
                    }
                    e.getValue().clear();
                }
            }
        }

        void flash() {
            body.clearActions();
            body.setColor(1f, 1f, 1f, 1f);
            body.addAction(Actions.color(color, 0.35f));
        }
    }

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
        } else if (event instanceof EventArmyMove m) {
            move(m);
        } else if (event instanceof EventArmyState st) {
            state(st);
        } else if (event instanceof EventStrike strike) {
            strike(strike);
        }
    }

    private void ensureMarkers() {
        if (attackers.getParent() == null) addActor(attackers);
        if (garrison.getParent() == null) {
            garrison.setPosition(px(garrisonY), py(garrisonX));
            addActor(garrison);
        }
    }

    private void state(EventArmyState st) {
        ensureMarkers();
        attackers.update(st.attackers);
        garrison.update(st.defenders);
    }

    private void move(EventArmyMove m) {
        ensureMarkers();
        float tx = px(m.toY), ty = py(m.toX);
        attackers.setVisible(true);
        if (m.fromX < 0) { // first appearance: come in from the nearest edge
            float left = tx, right = getWidth() - tx, bottom = ty, top = getHeight() - ty;
            float min = Math.min(Math.min(left, right), Math.min(bottom, top));
            float sx = tx, sy = ty;
            if (min == left) sx = -cell; else if (min == right) sx = getWidth(); else if (min == bottom) sy = -cell; else sy = getHeight();
            attackers.setPosition(sx, sy);
        }
        attackers.clearActions();
        attackers.addAction(Actions.moveTo(tx, ty, 0.7f / speed, Interpolation.smooth));
    }

    private void strike(EventStrike e) {
        ensureMarkers();
        float x, y;
        Color color;
        if (e.byAttacker) { // the blow lands on a building or on the garrison
            if (e.x >= 0) { x = px(e.y); y = py(e.x); } else { x = garrison.getX(); y = garrison.getY(); }
            color = new Color(1f, 0.55f, 0.15f, 1f);
            attackers.flash();
        } else { // the blow lands on the attacking army
            x = attackers.getX(); y = attackers.getY();
            color = e.kind.equals("TOWER") ? new Color(1f, 0.9f, 0.2f, 1f) : new Color(0.4f, 0.8f, 1f, 1f);
            garrison.flash();
        }
        String text = (e.kind.equals("TOWER") ? "Tower " : "") + "-" + e.amount;
        Label floater = new Label(text, skin);
        floater.setFontScale(0.55f);
        floater.setColor(color);
        floater.pack();
        floater.setPosition(x + cell / 2f - floater.getWidth() / 2f, y + cell);
        floater.addAction(Actions.sequence(
            Actions.parallel(Actions.moveBy(0f, 26f, 1.0f / speed), Actions.fadeOut(1.0f / speed)),
            Actions.removeActor()));
        addActor(floater);
    }

    private float px(int y) { return y * cell; }
    private float py(int x) { return (CELLS - 1 - x) * cell; }
    private static String key(int x, int y) { return x + "," + y; }

    private void setup(EventBattleSetup setup) {
        for (Image old : pieces.values()) old.remove();
        pieces.clear();
        clearFocus();
        for (EventBattleSetup.Piece p : setup.pieces) {
            if (p.name != null && p.name.toLowerCase().contains("castle")) { garrisonX = p.x; garrisonY = p.y; }
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
