package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.system.Leaderboard;
import com.yourname.got.mediator.GameMediator;

import java.util.List;

/** All-time ranking of the real players (wins, best score, total score). */
public class LeaderboardScreen implements Screen {
    private final GoTGame game;
    private final GameMediator mediator;
    private final GameUser currentUser;
    private final Stage stage;
    private final Skin skin;
    private final Texture background;

    public LeaderboardScreen(GoTGame game, GameMediator mediator, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.currentUser = user;
        this.stage = new Stage(new ScreenViewport());
        this.skin = UiTheme.loadSkin();
        this.background = new Texture("images/background-loding.jpg");
        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("Leaderboard", skin, "title");
        title.setFontScale(1.5f);

        Table list = new Table(skin);
        List<Leaderboard.Entry> top = Leaderboard.top(50);
        if (top.isEmpty()) {
            list.add(new Label("No finished games yet.", skin));
        } else {
            list.add(new Label("#", skin)).width(40);
            list.add(new Label("Player", skin)).width(170).left();
            list.add(new Label("Wins", skin)).width(60);
            list.add(new Label("Games", skin)).width(70);
            list.add(new Label("Best", skin)).width(80);
            list.add(new Label("Total", skin)).width(90).row();
            int rank = 1;
            for (Leaderboard.Entry e : top) {
                boolean me = currentUser != null && e.name.equalsIgnoreCase(currentUser.getUsername());
                Color c = me ? Color.GOLD : Color.WHITE;
                add(list, String.valueOf(rank++), 40, c);
                add(list, e.name, 170, c);
                add(list, String.valueOf(e.wins), 60, c);
                add(list, String.valueOf(e.games), 70, c);
                add(list, String.valueOf(e.bestScore), 80, c);
                add(list, String.valueOf(e.totalScore), 90, c);
                list.row();
            }
        }

        java.util.List<String> badges = com.yourname.got.logic.system.Achievements.unlocked(
            currentUser == null ? null : com.yourname.got.logic.system.Achievements.find(currentUser.getUsername()));
        if (!badges.isEmpty()) {
            list.row();
            Label head = new Label("Your achievements", skin);
            head.setColor(Color.GOLD);
            list.add(head).colspan(6).left().padTop(15).row();
            for (String b : badges) list.add(new Label("* " + b, skin)).colspan(6).left().row();
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameLoadingScreen(game, mediator, currentUser));
            }
        });

        root.add(title).padBottom(20).row();
        root.add(scroll).width(550).height(400).row();
        root.add(back).width(300).padTop(20).row();
    }

    private void add(Table t, String text, float width, Color color) {
        Label l = new Label(text, skin);
        l.setColor(color);
        t.add(l).width(width).pad(3);
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().setColor(Color.WHITE);
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        background.dispose();
    }
}
