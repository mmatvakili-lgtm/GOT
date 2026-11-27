package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * A short message at the top of the screen that fades away by itself (used for errors, so the player sees "Not enough
 * resources" without hunting through the message log). It never takes clicks, only one is shown at a time, and every
 * failure is swallowed: a toast must never break the action that produced it.
 */
public final class Toast {
    private Toast() {}

    private static final String NAME = "toast";
    private static final float VISIBLE_SECONDS = 2.8f;

    public static void show(Stage stage, Skin skin, String text, Color color) {
        if (stage == null || skin == null || text == null || text.isEmpty()) return;
        try {
            Actor old = stage.getRoot().findActor(NAME);
            if (old != null) old.remove();

            Table box = new Table(skin);
            box.setName(NAME);
            box.setTouchable(Touchable.disabled);
            box.setBackground(skin.newDrawable("white", UiTheme.PANEL));
            Label l = new Label(text.length() > 140 ? text.substring(0, 137) + "..." : text, skin);
            l.setWrap(true);
            l.setAlignment(com.badlogic.gdx.utils.Align.center);
            l.setColor(color == null ? Color.WHITE : color);
            box.add(l).width(Math.min(700f, stage.getWidth() * 0.6f)).pad(10f, 18f, 10f, 18f);
            box.pack();
            box.setPosition((stage.getWidth() - box.getWidth()) / 2f, stage.getHeight() - box.getHeight() - 24f);
            box.getColor().a = 0f;
            box.addAction(Actions.sequence(Actions.fadeIn(0.15f), Actions.delay(VISIBLE_SECONDS),
                Actions.fadeOut(0.4f), Actions.removeActor()));
            stage.addActor(box);
        } catch (RuntimeException ignored) {
            // a toast is only a convenience
        }
    }
}
