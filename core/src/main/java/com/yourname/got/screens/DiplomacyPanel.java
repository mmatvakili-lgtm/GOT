package com.yourname.got.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.yourname.got.logic.system.Diplomacy;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

/** Extra > Diplomacy: offers, answers and cancels peace treaties and alliances. */
public class DiplomacyPanel extends Table {
    private final Skin skin;
    private final GameMediator mediator;
    private String message = "";
    private Color messageColor = Color.LIGHT_GRAY;

    public DiplomacyPanel(Skin skin, GameMediator mediator) {
        this.skin = skin;
        this.mediator = mediator;
        render();
    }

    private TextButton button(String text, Runnable action) {
        String style = (text.equals("Accept") || text.startsWith("Offer")) ? "primary"
                : (text.equals("Refuse") || text.equals("Cancel") || text.equals("Withdraw")) ? "danger" : null;
        TextButton b = (style != null && skin.has(style, TextButton.TextButtonStyle.class))
                ? new TextButton(text, skin, style) : new TextButton(text, skin);
        b.getLabel().setColor(style != null && skin.has(style, TextButton.TextButtonStyle.class) ? Color.WHITE : Color.BLACK);
        b.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        return b;
    }

    private void done(Result r) {
        message = r.getMessage();
        messageColor = r.isSuccess() ? new Color(0.4f, 0.85f, 0.5f, 1f) : new Color(1f, 0.5f, 0.45f, 1f);
        render();
    }

    private void render() {
        clearChildren();
        pad(12f);
        Label intro = new Label("Make peace or ally with other players. While an agreement is active neither side can attack the other. "
            + "A peace treaty ends by itself; an alliance lasts until someone cancels. Offers expire after "
            + Diplomacy.PROPOSAL_DAYS + " game days.", skin);
        intro.setWrap(true);
        intro.setFontScale(0.7f);
        add(intro).width(640f).left().padBottom(10f).row();

        Table list = new Table();
        list.top().left();
        for (GameMediator.DiplomacyRow row : mediator.diplomacyRows()) {
            Table line = new Table();
            line.setBackground(skin.newDrawable("white", new Color(0.15f, 0.17f, 0.22f, 1f)));
            line.pad(8f);
            Label name = new Label(row.other.getUsername() + "  (" + row.other.getKingdom().getType().name() + ")", skin);
            name.setFontScale(0.85f);
            Label status = new Label(row.status, skin);
            status.setFontScale(0.7f);
            status.setColor(row.hasPact ? new Color(0.4f, 0.85f, 0.55f, 1f) : Color.LIGHT_GRAY);
            Table info = new Table();
            info.add(name).left().row();
            info.add(status).left();
            line.add(info).left().expandX();
            if (row.hasPact) {
                line.add(button("Cancel", () -> done(mediator.diplomacyCancel(row.other)))).width(110f).height(32f);
            } else if (row.incoming) {
                line.add(button("Accept", () -> done(mediator.diplomacyRespond(row.other, true)))).width(100f).height(32f).padRight(6f);
                line.add(button("Refuse", () -> done(mediator.diplomacyRespond(row.other, false)))).width(100f).height(32f);
            } else if (row.outgoing) {
                line.add(button("Withdraw", () -> done(mediator.diplomacyCancel(row.other)))).width(110f).height(32f);
            } else {
                line.add(button("Offer peace", () -> done(mediator.diplomacyPropose(row.other, Diplomacy.Type.PEACE)))).width(130f).height(32f).padRight(6f);
                line.add(button("Offer alliance", () -> done(mediator.diplomacyPropose(row.other, Diplomacy.Type.ALLIANCE)))).width(140f).height(32f);
            }
            list.add(line).growX().padBottom(6f).row();
        }
        if (list.getChildren().size == 0) {
            Label none = new Label("No other players to talk to right now.", skin);
            list.add(none);
        }
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        add(scroll).width(640f).height(340f).row();

        Label msg = new Label(message, skin);
        msg.setWrap(true);
        msg.setFontScale(0.75f);
        msg.setColor(messageColor);
        add(msg).width(640f).left().padTop(8f);
    }
}
