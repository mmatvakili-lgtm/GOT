package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import java.util.Random;

/**
 * While the dead are marching on the player: a cold blue tint that pulses, a blizzard of snowflakes and a banner with
 * the enemy list and a countdown. It never takes clicks and removes itself when the countdown ends (or when the
 * battle report replaces it).
 */
public class LongNightOverlay extends Group {
    private static final int FLAKES = 170;

    private final Drawable white;
    private final boolean finalWave;
    private final Label countdown;
    private float remaining;
    private float time;

    private final float[] fx = new float[FLAKES], fy = new float[FLAKES], speed = new float[FLAKES], size = new float[FLAKES], phase = new float[FLAKES];

    public LongNightOverlay(Skin skin, float width, float height, String enemies, int seconds, boolean finalWave) {
        this.white = skin.getDrawable("white");
        this.finalWave = finalWave;
        this.remaining = Math.max(1, seconds);
        setSize(width, height);
        setTouchable(Touchable.disabled);
        setName("longNight");

        Random r = new Random(7);
        for (int i = 0; i < FLAKES; i++) {
            fx[i] = r.nextFloat() * width;
            fy[i] = r.nextFloat() * height;
            speed[i] = 40f + r.nextFloat() * (finalWave ? 220f : 120f);
            size[i] = 2f + r.nextFloat() * (finalWave ? 5f : 3f);
            phase[i] = r.nextFloat() * 6.28f;
        }

        addActor(new Actor() {
            @Override public void draw(Batch batch, float parentAlpha) { drawWeather(batch, parentAlpha); }
        });

        Table banner = new Table(skin);
        banner.setTouchable(Touchable.disabled);
        banner.setBackground(skin.newDrawable("white", new Color(0.05f, 0.09f, 0.16f, 0.92f)));
        Label title = new Label(finalWave ? "THE LONG NIGHT HAS COME" : "THE DEAD ARE MARCHING", skin, "title");
        title.setFontScale(0.85f);
        title.setColor(finalWave ? new Color(0.75f, 0.9f, 1f, 1f) : new Color(0.6f, 0.82f, 1f, 1f));
        Label list = new Label(enemies, skin);
        list.setWrap(true);
        list.setFontScale(0.75f);
        list.setAlignment(com.badlogic.gdx.utils.Align.center);
        list.setColor(Color.LIGHT_GRAY);
        countdown = new Label("", skin);
        countdown.setFontScale(0.9f);
        countdown.setColor(UiTheme.WARN);
        banner.add(title).padTop(10f).row();
        banner.add(list).width(Math.min(760f, width * 0.7f)).pad(4f, 16f, 2f, 16f).row();
        banner.add(countdown).padBottom(10f);
        banner.pack();
        banner.setPosition((width - banner.getWidth()) / 2f, height - banner.getHeight() - 70f);
        addActor(banner);
        updateCountdown();
    }

    private void drawWeather(Batch batch, float parentAlpha) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * (finalWave ? 2.2f : 1.4f));
        float tint = (finalWave ? 0.18f : 0.10f) + 0.06f * pulse;
        batch.setColor(0.45f, 0.65f, 1f, tint * parentAlpha);
        white.draw(batch, 0, 0, getWidth(), getHeight());
        // darker edges, like a vignette
        float edge = Math.min(getWidth(), getHeight()) * 0.08f;
        batch.setColor(0.02f, 0.04f, 0.1f, 0.35f * parentAlpha);
        white.draw(batch, 0, 0, getWidth(), edge);
        white.draw(batch, 0, getHeight() - edge, getWidth(), edge);
        white.draw(batch, 0, 0, edge, getHeight());
        white.draw(batch, getWidth() - edge, 0, edge, getHeight());
        batch.setColor(0.92f, 0.96f, 1f, 0.85f * parentAlpha);
        for (int i = 0; i < FLAKES; i++) white.draw(batch, fx[i], fy[i], size[i], size[i]);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        remaining -= delta;
        for (int i = 0; i < FLAKES; i++) {
            fy[i] -= speed[i] * delta;
            fx[i] += (float) Math.sin(time * 0.8f + phase[i]) * 30f * delta + (finalWave ? 90f : 40f) * delta;
            if (fy[i] < -8f) { fy[i] = getHeight() + 8f; }
            if (fx[i] > getWidth() + 8f) { fx[i] = -8f; }
        }
        updateCountdown();
        if (remaining <= -2f) remove(); // the report normally removes it first
    }

    private void updateCountdown() {
        int s = (int) Math.max(0, Math.ceil(remaining));
        countdown.setText(remaining > 0 ? "They arrive in " + (s / 60) + ":" + String.format("%02d", s % 60) : "They are at the gates!");
    }
}
