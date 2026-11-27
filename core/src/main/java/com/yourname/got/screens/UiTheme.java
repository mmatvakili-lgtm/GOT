package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.UserSettings;

/**
 * One place for the look of the game: the colour palette, extra button and label styles, and the small window fade-in.
 * Every screen loads its skin through {@link #loadSkin()}. The extra styles are only added on top of the existing skin
 * (nothing is replaced), and if adding them ever fails the plain skin is used, so the theme can never stop the game.
 */
public final class UiTheme {
    private UiTheme() {}

    // palette
    public static final Color GOLD = new Color(0.95f, 0.78f, 0.30f, 1f);
    public static final Color GOOD = new Color(0.45f, 0.85f, 0.55f, 1f);
    public static final Color BAD = new Color(1.00f, 0.45f, 0.40f, 1f);
    public static final Color WARN = new Color(1.00f, 0.72f, 0.28f, 1f);
    public static final Color MUTED = new Color(0.66f, 0.69f, 0.76f, 1f);
    public static final Color PANEL = new Color(0.12f, 0.13f, 0.17f, 0.96f);

    private static final float FADE_SECONDS = 0.15f;

    /** Loads the shared skin and adds the themed styles: buttons "primary", "danger", "gold"; labels "good", "bad", "warn", "muted", "heading". */
    public static Skin loadSkin() {
        Skin skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));
        try {
            decorate(skin);
        } catch (RuntimeException e) {
            GameLog.error("UI theme could not be applied, using the plain skin: " + e);
        }
        return skin;
    }

    private static void decorate(Skin skin) {
        if (!skin.has("round-white", Drawable.class)) return;
        TextButton.TextButtonStyle base = skin.get("default", TextButton.TextButtonStyle.class);
        skin.add("primary", button(skin, base, new Color(0.16f, 0.55f, 0.28f, 1f), new Color(0.22f, 0.70f, 0.36f, 1f),
            new Color(0.10f, 0.40f, 0.20f, 1f), Color.WHITE), TextButton.TextButtonStyle.class);
        skin.add("danger", button(skin, base, new Color(0.62f, 0.16f, 0.16f, 1f), new Color(0.78f, 0.22f, 0.22f, 1f),
            new Color(0.45f, 0.10f, 0.10f, 1f), Color.WHITE), TextButton.TextButtonStyle.class);
        skin.add("gold", button(skin, base, new Color(0.75f, 0.58f, 0.16f, 1f), new Color(0.92f, 0.74f, 0.27f, 1f),
            new Color(0.55f, 0.42f, 0.10f, 1f), new Color(0.10f, 0.08f, 0.02f, 1f)), TextButton.TextButtonStyle.class);

        Label.LabelStyle label = skin.get("default", Label.LabelStyle.class);
        addLabel(skin, label, "good", GOOD);
        addLabel(skin, label, "bad", BAD);
        addLabel(skin, label, "warn", WARN);
        addLabel(skin, label, "muted", MUTED);
        addLabel(skin, label, "heading", GOLD);
    }

    private static TextButton.TextButtonStyle button(Skin skin, TextButton.TextButtonStyle base, Color up, Color over, Color down, Color font) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle(base);
        s.up = skin.newDrawable("round-white", up);
        s.over = skin.newDrawable("round-white", over);
        s.down = skin.newDrawable("round-white", down);
        s.fontColor = font;
        return s;
    }

    private static void addLabel(Skin skin, Label.LabelStyle base, String name, Color color) {
        Label.LabelStyle s = new Label.LabelStyle(base);
        s.fontColor = color;
        skin.add(name, s, Label.LabelStyle.class);
    }

    /** Colour of a line in the message log by type (ERROR, SYSTEM, NOTIF); other types are white. */
    public static Color messageColor(String type) {
        if ("ERROR".equals(type)) return BAD;
        if ("SYSTEM".equals(type)) return GOOD;
        if ("NOTIF".equals(type)) return WARN;
        return Color.WHITE;
    }

    /** A short fade-in for a freshly opened window. Does nothing when visual effects are off in Settings. */
    public static void fadeIn(Actor actor) {
        if (actor == null || !UserSettings.isEffectsEnabled()) return;
        actor.getColor().a = 0f;
        actor.addAction(Actions.fadeIn(FADE_SECONDS));
    }
}
