package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.mediator.GameMediator;

public class GameLoadingScreen implements Screen {

    private final GoTGame game;
    private final GameMediator mediator;
    private final GameUser user;
    private final Stage stage;
    private Texture background;
    private final Skin skin;

    public GameLoadingScreen(GoTGame game, GameMediator mediator, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.user = user;

        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        background = new Texture("images/background-loding.jpg");
        skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));

        createUI();
    }

    private void createUI() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // تعریف متغیرها برای چیدمان بهتر
        float marginLeft = 280;
        float marginTop = 380; // کمی بالاتر بردیم تا دکمه جدید جا شود
        float buttonWidth = 300;
        float buttonHeight = 60;
        float verticalSpacing = 20; // فاصله عمودی بین دکمه‌ها

        // --- ایجاد دکمه‌ها ---
        TextButton createGameButton = new TextButton("Create a new game", skin);
        TextButton joinGameButton = new TextButton("Join to existing games", skin);
        TextButton historyButton = new TextButton("Game History", skin); // <<<< دکمه جدید
        TextButton logoutButton = new TextButton("Logout", skin);

        // --- تعیین اندازه دکمه‌ها ---
        createGameButton.setSize(buttonWidth, buttonHeight);
        joinGameButton.setSize(buttonWidth, buttonHeight);
        historyButton.setSize(buttonWidth, buttonHeight); // <<<< تعیین اندازه دکمه جدید
        logoutButton.setSize(buttonWidth, buttonHeight);

        // --- تعیین موقعیت دکمه‌ها به صورت پله‌ای ---
        float currentY = screenHeight - marginTop;
        createGameButton.setPosition(marginLeft, currentY);

        currentY -= (buttonHeight + verticalSpacing);
        joinGameButton.setPosition(marginLeft, currentY);

        currentY -= (buttonHeight + verticalSpacing);
        historyButton.setPosition(marginLeft, currentY); // <<<< تعیین موقعیت دکمه جدید

        currentY -= (buttonHeight + verticalSpacing);
        logoutButton.setPosition(marginLeft, currentY);

        // --- افزودن Listener برای دکمه‌ها ---
        createGameButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                mediator.goToGameBuilder();
            }
        });

        joinGameButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                mediator.goToGameJoin();
            }
        });

        // <<<< افزودن Listener برای دکمه جدید >>>>
        historyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // با کلیک روی این دکمه، به صفحه سوابق بازی می‌رویم
                game.setScreen(new GameHistoryScreen(game, mediator, user));
            }
        });

        logoutButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.goToLoginScreen();
            }
        });

        // --- افزودن دکمه‌ها به Stage ---
        stage.addActor(createGameButton);
        stage.addActor(joinGameButton);
        stage.addActor(historyButton); // <<<< افزودن دکمه جدید به صحنه
        stage.addActor(logoutButton);
    }

    @Override public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
        background.dispose();
    }
}
