package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.system.Tutorial;

/** Extra > Beginner's Guide: the steps with a tick for each one that is done and the next one highlighted. */
public class TutorialPanel extends Table {
    private static final Color DONE = new Color(0.30f, 0.75f, 0.45f, 1f);
    private static final Color NEXT = new Color(0.98f, 0.76f, 0.25f, 1f);

    public TutorialPanel(Skin skin, Kingdom kingdom) {
        pad(12f);
        java.util.List<Tutorial.Step> steps = Tutorial.steps();
        int done = Tutorial.doneCount(kingdom);
        Tutorial.Step next = Tutorial.next(kingdom);

        Label head = new Label(next == null ? "Guide complete - you know the basics!" : done + " of " + steps.size() + " steps done", skin);
        head.setColor(next == null ? DONE : Color.WHITE);
        add(head).left().padBottom(4f).row();
        ProgressBar bar = new ProgressBar(0, Math.max(1, steps.size()), 1, false, skin);
        bar.setValue(done);
        bar.setAnimateDuration(0f);
        add(bar).width(640f).padBottom(10f).row();

        Table list = new Table();
        list.top().left();
        int number = 1;
        for (Tutorial.Step s : steps) {
            boolean isDone = s.isDone(kingdom);
            boolean isNext = s == next;
            Table card = new Table();
            card.setBackground(skin.newDrawable("white", isNext ? new Color(0.28f, 0.24f, 0.10f, 1f) : new Color(0.15f, 0.17f, 0.22f, 1f)));
            card.pad(10f);
            Label mark = new Label(isDone ? "[x]" : String.valueOf(number), skin);
            mark.setColor(isDone ? DONE : isNext ? NEXT : Color.GRAY);
            card.add(mark).top().width(40f);
            Table text = new Table();
            Label title = new Label(s.title + (isNext ? "   <- next" : ""), skin);
            title.setFontScale(0.85f);
            title.setColor(isDone ? DONE : isNext ? NEXT : Color.WHITE);
            text.add(title).left().row();
            if (!isDone) {
                Label why = new Label(s.why, skin);
                why.setWrap(true); why.setFontScale(0.65f); why.setColor(Color.LIGHT_GRAY);
                text.add(why).width(540f).left().row();
                Label how = new Label("How: " + s.how, skin);
                how.setWrap(true); how.setFontScale(0.65f); how.setColor(isNext ? NEXT : Color.GRAY);
                text.add(how).width(540f).left().padTop(2f).row();
            }
            card.add(text).left().expandX();
            list.add(card).growX().padBottom(6f).row();
            number++;
        }
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        add(scroll).width(640f).height(360f).row();
    }
}
