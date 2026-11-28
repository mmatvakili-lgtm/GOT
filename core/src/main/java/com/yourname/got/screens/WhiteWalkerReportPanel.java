package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.yourname.got.logic.WhiteWalker.AssaultReport;

/** The battle report after the dead have attacked: who came, who fell, what it cost, and how it ended. */
public class WhiteWalkerReportPanel extends Table {
    private static final Color ICE = new Color(0.62f, 0.82f, 1f, 1f);

    public WhiteWalkerReportPanel(Skin skin, AssaultReport r, Runnable onClose) {
        setBackground(skin.newDrawable("white", new Color(0.05f, 0.08f, 0.14f, 0.98f)));
        pad(20f);

        String headline;
        String flavour;
        Color color;
        if (r.overrun) {
            headline = r.finalWave ? "THE LONG NIGHT CLAIMS YOU" : "YOUR KINGDOM IS OVERRUN";
            flavour = "The banners burn and the dead walk through the gates. Nothing living remains.";
            color = new Color(1f, 0.42f, 0.38f, 1f);
        } else if (r.finalWave) {
            headline = "THE NIGHT IS BROKEN";
            flavour = "Against all hope your soldiers held the line until the Night King fell silent.";
            color = new Color(1f, 0.85f, 0.4f, 1f);
        } else {
            headline = "THE ASSAULT IS REPELLED";
            flavour = "The dead are driven back into the snow. Rebuild before the next wave comes.";
            color = ICE;
        }

        Label title = new Label(headline, skin, "title");
        title.setFontScale(1.1f);
        title.setColor(color);
        add(title).padBottom(6f).row();
        Label where = new Label((r.finalWave ? "Final wave" : "Day " + r.day) + "  -  " + r.kingdom.replace('_', ' '), skin);
        where.setFontScale(0.75f);
        where.setColor(Color.LIGHT_GRAY);
        add(where).padBottom(10f).row();

        Label came = new Label("Against you came: " + r.enemies + ".", skin);
        came.setWrap(true);
        came.setFontScale(0.8f);
        add(came).width(560f).padBottom(12f).row();

        add(bar(skin, "Enemies slain", r.enemiesSlain(), r.enemiesBefore, ICE)).padBottom(8f).row();
        add(bar(skin, "Your soldiers who survived", r.defendersAfter, r.defendersBefore,
            r.overrun ? new Color(1f, 0.42f, 0.38f, 1f) : new Color(0.45f, 0.85f, 0.55f, 1f))).padBottom(8f).row();

        Label cost = new Label("Soldiers lost: " + r.defendersLost() + "     Dragonglass used: " + r.glassUsed, skin);
        cost.setFontScale(0.8f);
        cost.setColor(UiTheme.WARN);
        add(cost).padBottom(12f).row();

        Label story = new Label(flavour, skin);
        story.setWrap(true);
        story.setFontScale(0.75f);
        story.setAlignment(com.badlogic.gdx.utils.Align.center);
        story.setColor(Color.LIGHT_GRAY);
        add(story).width(520f).padBottom(14f).row();

        TextButton close = new TextButton(r.overrun ? "Accept your fate" : "Continue", skin,
            skin.has("primary", TextButton.TextButtonStyle.class) ? "primary" : "default");
        close.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { onClose.run(); }
        });
        add(close).width(240f).height(40f);
    }

    private Table bar(Skin skin, String label, int value, int max, Color color) {
        Table t = new Table();
        Label l = new Label(label + ": " + value + " / " + max, skin);
        l.setFontScale(0.75f);
        t.add(l).left().row();
        ProgressBar b = new ProgressBar(0, Math.max(1, max), 1, false, skin);
        b.setValue(Math.min(value, Math.max(1, max)));
        b.setAnimateDuration(0f);
        b.setColor(color);
        t.add(b).width(540f);
        return t;
    }
}
