package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.util.UserSettings;
import com.yourname.got.mediator.GameMediator;

/** Music and effects volume; applied immediately and saved when leaving the screen. */
public class SettingsScreen implements Screen {
    private final GoTGame game;
    private final GameMediator mediator;
    private final GameUser currentUser;
    private final Stage stage;
    private final Skin skin;
    private final Texture background;

    public SettingsScreen(GoTGame game, GameMediator mediator, GameUser user) {
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

        Label title = new Label("Settings", skin, "title");
        title.setFontScale(1.5f);

        final Slider music = new Slider(0f, 1f, 0.05f, false, skin);
        music.setValue(UserSettings.getMusicVolume());
        music.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                UserSettings.setMusicVolume(music.getValue());
                if (game.getMusicManager() != null) game.getMusicManager().setVolume(music.getValue());
            }
        });

        final Slider sfx = new Slider(0f, 1f, 0.05f, false, skin);
        sfx.setValue(UserSettings.getSfxVolume());
        sfx.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                UserSettings.setSfxVolume(sfx.getValue());
            }
        });

        final TextButton effects = new TextButton("Visual effects: " + (UserSettings.isEffectsEnabled() ? "On" : "Off"), skin);
        effects.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                UserSettings.setEffectsEnabled(!UserSettings.isEffectsEnabled());
                effects.setText("Visual effects: " + (UserSettings.isEffectsEnabled() ? "On" : "Off"));
            }
        });

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                UserSettings.save();
                game.setScreen(new GameLoadingScreen(game, mediator, currentUser));
            }
        });

        root.add(title).colspan(2).padBottom(30).row();
        root.add(new Label("Music volume", skin)).left().padRight(20);
        root.add(music).width(250).row();
        root.add(new Label("Effects volume", skin)).left().padRight(20).padTop(15);
        root.add(sfx).width(250).padTop(15).row();
        root.add(effects).colspan(2).width(300).padTop(25).row();
        root.add(back).colspan(2).width(300).padTop(15).row();
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
