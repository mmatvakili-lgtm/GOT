package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.yourname.got.logic.util.EventArmyState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The two armies of a battle replay, unit by unit: soldiers left, losses since the last picture and a health bar
 * (full = the health the unit had when it was first seen). Rebuilt from every EventArmyState.
 */
public class ArmyStatusPanel extends Table {
    private final Skin skin;
    private final Map<String, Long> maxHealth = new HashMap<>();
    private final Map<String, Integer> lastCount = new HashMap<>();
    private final Map<String, Integer> startCount = new HashMap<>();

    public ArmyStatusPanel(Skin skin) {
        this.skin = skin;
        top();
    }

    public static Color colorOf(String type) {
        if (type == null) return Color.WHITE;
        if (type.startsWith("INFANTRY")) return new Color(0.80f, 0.82f, 0.88f, 1f);
        if (type.startsWith("ARCHER") || type.startsWith("WALL")) return new Color(0.45f, 0.85f, 0.45f, 1f);
        if (type.startsWith("CAVALRY")) return new Color(1f, 0.65f, 0.25f, 1f);
        return Color.WHITE;
    }

    public void reset() {
        clearChildren();
        maxHealth.clear();
        lastCount.clear();
        startCount.clear();
    }

    public void show(EventArmyState state) {
        clearChildren();
        Label round = new Label(state.label, skin);
        round.setFontScale(0.8f);
        round.setColor(Color.GOLD);
        add(round).left().padBottom(4f).row();

        if (state.heroName != null) {
            Label hero = new Label("Hero " + state.heroName + "  HP " + state.heroHealth, skin);
            hero.setFontScale(0.7f);
            hero.setColor(Color.GOLD);
            add(hero).left().row();
        }
        section("ATTACKERS (front row first)", Color.ORANGE, "A", state.attackers);
        if (state.structureName != null) {
            Label st = new Label(state.structureName + ": " + state.structureHealth + " HP", skin);
            st.setFontScale(0.7f);
            st.setColor(Color.SALMON);
            add(st).left().padTop(4f).row();
        }
        section("DEFENDERS", Color.CYAN, "D", state.defenders);
    }

    private void section(String title, Color color, String side, List<EventArmyState.Unit> units) {
        Label head = new Label(title, skin);
        head.setFontScale(0.7f);
        head.setColor(color);
        add(head).left().padTop(6f).row();
        if (units.isEmpty()) {
            Label none = new Label("(none left)", skin);
            none.setFontScale(0.65f);
            none.setColor(Color.GRAY);
            add(none).left().row();
            return;
        }
        ProgressBar.ProgressBarStyle barStyle = skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class);
        for (EventArmyState.Unit u : units) {
            String key = side + u.type;
            long max = maxHealth.merge(key, u.health, Math::max);
            startCount.putIfAbsent(key, u.count);
            Integer before = lastCount.put(key, u.count);
            int lost = (before == null) ? 0 : Math.max(0, before - u.count);

            Table row = new Table();
            Label name = new Label(u.type + "  x" + u.count + (lost > 0 ? "  (-" + lost + ")" : ""), skin);
            name.setFontScale(0.65f);
            name.setColor(lost > 0 ? Color.SALMON : colorOf(u.type));
            row.add(name).left().row();
            ProgressBar bar = new ProgressBar(0, Math.max(1, max), 1, false, barStyle);
            bar.setValue(u.health);
            row.add(bar).width(250f).height(10f).left();
            add(row).left().padBottom(3f).row();
        }
    }

    /** The count a unit started with in the replay (used to size the soldier dots), or 0. */
    public int startCountOf(String side, String type) {
        Integer c = startCount.get(side + type);
        return c == null ? 0 : c;
    }
}
