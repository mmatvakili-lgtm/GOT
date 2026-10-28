package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.yourname.got.GoTGame;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.mediator.GameMessageListener;

public class RegisterScreen extends ScreenAdapter implements GameMessageListener {

    private Stage stage;
    private Skin skin;
    private Texture background;
    private final GoTGame game;

    private Label messageLabel;
    private TextField usernameField;
    private TextField passwordField;
    private TextField confirmField;
    private final GameMediator mediator;

    public RegisterScreen(GoTGame game, GameMediator mediator) {
        this.game = game;
        this.mediator = mediator;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        background = new Texture("images/background2.jpg");
        skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Label usernameLabel = new Label("Username:", skin);
        usernameField = new TextField("", skin);

        Label passwordLabel = new Label("Password:", skin);
        passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        Label confirmLabel = new Label("Confirm Password:", skin);
        confirmField = new TextField("", skin);
        confirmField.setPasswordMode(true);
        confirmField.setPasswordCharacter('*');

        TextButton registerButton = new TextButton("Register", skin);
        TextButton backButton = new TextButton("Back", skin);

        messageLabel = new Label("", skin);
        messageLabel.setWrap(true);
        messageLabel.setColor(1, 0, 0, 1);
        mediator.setMessageListener(this);

        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean success = mediator.register(
                    usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    confirmField.getText().trim()
                );

                if (success) {
                    mediator.goToLoadingScreen();
                }
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LoginScreen(game, mediator));
            }
        });

        table.add(usernameLabel).left().padBottom(5);
        table.row();
        table.add(usernameField).width(300).padBottom(10);
        table.row();

        table.add(passwordLabel).left().padBottom(5);
        table.row();
        table.add(passwordField).width(300).padBottom(10);
        table.row();

        table.add(confirmLabel).left().padBottom(5);
        table.row();
        table.add(confirmField).width(300).padBottom(20);
        table.row();

        table.add(registerButton).width(140).padBottom(10);
        table.row();
        table.add(backButton).width(140).padBottom(20);
        table.row();

        table.add(messageLabel).width(300).colspan(2).padTop(10).row();

        stage.addActor(table);
    }

    @Override
    public void onMessage(String message) {
        messageLabel.setText(message);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        background.dispose();
    }
}
