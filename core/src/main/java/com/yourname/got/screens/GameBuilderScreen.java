package com.yourname.got.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameBuilder;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

public class GameBuilderScreen implements Screen {

    private final GoTGame game;
    private final GameUser currentUser;
    private final GameMediator mediator;
    private final Stage stage;
    private final Skin skin;
    private Texture background;

    private final float MARGIN_LEFT = 280f;
    private float currentY;

    private Label resultLabel;

    public GameBuilderScreen(GoTGame game, GameMediator mediator, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.currentUser = user;

        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        background = new Texture("images/background-loding.jpg");
        skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));

        createUI();
    }

    private void createUI() {
        currentY = Gdx.graphics.getHeight() - 390;

        Label titleLabel = new Label("Creating a new game", skin);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.left);
        titleLabel.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(titleLabel);
        currentY -= 60;

        TextField nameField = new TextField("", skin);
        nameField.setMessageText("Name");
        nameField.setWidth(300);
        nameField.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(nameField);
        currentY -= 60;

        TextField passField = new TextField("", skin);
        passField.setMessageText("Password (optional)");
        passField.setPasswordMode(true);
        passField.setPasswordCharacter('*');
        passField.setWidth(300);
        passField.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(passField);
        currentY -= 60;

        TextButton createBtn = new TextButton("Game creation", skin);
        createBtn.setWidth(180);
        createBtn.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(createBtn);
        currentY -= 60;

        TextButton backBtn = new TextButton("Back", skin);
        backBtn.setWidth(180);
        backBtn.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(backBtn);
        currentY -= 60;

        resultLabel = new Label("", skin);
        resultLabel.setColor(1, 0, 0, 1); // قرمز
        resultLabel.setWidth(400);
        resultLabel.setWrap(true);
        resultLabel.setAlignment(Align.left);
        resultLabel.setPosition(MARGIN_LEFT, currentY);
        stage.addActor(resultLabel);

        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleCreateGame(nameField, passField);
            }
        });

        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameLoadingScreen(game, mediator, currentUser));
            }
        });

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    handleCreateGame(nameField, passField);
                    return true;
                }
                return false;
            }
        });
    }

    private void handleCreateGame(TextField nameField, TextField passField) {
        String name = nameField.getText().trim();
        String pass = passField.getText().trim();

        Result result = mediator.createNewGame(name,pass);
        resultLabel.setText(result.getMessage());

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
