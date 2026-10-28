package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.mediator.GameMessageListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import java.util.Properties;

public class LoginScreen extends ScreenAdapter implements GameMessageListener {

    private Stage stage;
    private Skin skin;
    private Texture background;
    private final GoTGame game;

    private Label messageLabel;
    private TextField usernameField;
    private TextField passwordField;
    private final GameMediator mediator;
    private Window activeAdminWindow;

    public LoginScreen(GoTGame game, GameMediator mediator) {
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


        messageLabel = new Label("", skin);
        messageLabel.setColor(1, 0, 0, 1);


        TextButton loginButton = new TextButton("Login", skin);


        TextButton registerButton = new TextButton("Register", skin);
        mediator.setMessageListener(this);


        TextButton adminButton = new TextButton("Admin", skin);

        this.game.getMusicManager().playMusic("login_intro");

        adminButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Instead of opening the panel directly, we now open the password dialog first.
                openAdminLoginDialog();
            }
        });



        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mediator.login(
                    usernameField.getText().trim(),
                    passwordField.getText().trim()
                );

            }
        });

        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new RegisterScreen(game, mediator));
            }
        });

        table.add(usernameLabel).left().padBottom(5);
        table.row();
        table.add(usernameField).width(300).padBottom(20);
        table.row();

        table.add(passwordLabel).left().padBottom(5);
        table.row();
        table.add(passwordField).width(300).padBottom(20);
        table.row();

        table.add(loginButton).width(140).padBottom(10);
        table.row();
        table.add(registerButton).width(140).padBottom(20);
        table.row();

        table.add(messageLabel).colspan(2).center().padTop(10);
        table.row();

        table.add(adminButton).bottom().width(80).height(50).pad(10);
        table.row();
        stage.addActor(table);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    mediator.login(usernameField.getText().trim(), passwordField.getText().trim());

                    if (mediator.getCurrentUser() != null) {
                        if(mediator.getCurrentUser().isInGame && mediator.getCurrentUser().getGame().isGameReady()){
                            mediator.goToGamePlayScreen();
                        }
                        mediator.goToGameLoading();
                    }
                    return true;
                }
                return false;
            }
        });
    }


    private void openAdminLoginDialog() {
        if (activeAdminWindow != null) activeAdminWindow.remove();

        // Using a Window for consistency with the rest of the application.
        final Window loginWindow = new Window("Admin Access", skin);
        loginWindow.setModal(true);
        loginWindow.setMovable(true);
        loginWindow.padTop(40); // Standard padding for title

        Table content = new Table(skin);
        content.pad(20);

        Label infoLabel = new Label("Please enter the admin password:", skin);
        infoLabel.setColor(Color.BLACK);

        final TextField passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.getStyle().fontColor = Color.BLACK;

        content.add(infoLabel).row();
        content.add(passwordField).width(300).padTop(10).row();

        // --- Buttons ---
        TextButton confirmButton = new TextButton("Confirm", skin);
        confirmButton.getLabel().setColor(Color.BLACK);

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.getLabel().setColor(Color.BLACK);

        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Check if the password is correct
                if ("adminGot".equals(passwordField.getText())) {
                    loginWindow.remove(); // Close this login window
                    openAdminPanel();     // Open the main admin panel
                } else {
                    // Show an error message. For simplicity, we print it.
                    // In a real UI, you might add a temporary error label to the dialog.
                    System.out.println("Login Failed: Incorrect Admin Password!");
                    // You could also update a label: errorLabel.setText("Incorrect Password");
                }
            }
        });

        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loginWindow.remove(); // Just close the window
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(confirmButton).pad(10);
        buttonTable.add(cancelButton).pad(10);
        content.add(buttonTable).padTop(20);

        loginWindow.add(content);
        loginWindow.pack();
        loginWindow.setPosition(
            (stage.getWidth() - loginWindow.getWidth()) / 2,
            (stage.getHeight() - loginWindow.getHeight()) / 2
        );

        // Storing the reference so other methods can close it if needed
        activeAdminWindow = loginWindow;
        stage.addActor(loginWindow);
    }

    private void openAdminPanel() {
        if (activeAdminWindow != null) activeAdminWindow.remove();

        Table content = new Table(skin);
        content.pad(10);

        // --- Main Menu Buttons ---
        TextButton addNoticeBtn = new TextButton("Add New Notice", skin);
        TextButton deleteNoticeBtn = new TextButton("Delete Notice", skin);
        TextButton configBtn = new TextButton("Manage Global Config", skin);
        TextButton backBtn = new TextButton("Back", skin); // Back button for the main panel

        // --- Set Text Color to Black ---
        addNoticeBtn.getLabel().setColor(Color.BLACK);
        deleteNoticeBtn.getLabel().setColor(Color.BLACK);
        configBtn.getLabel().setColor(Color.BLACK);
        backBtn.getLabel().setColor(Color.BLACK);

        // --- Add Listeners ---
        addNoticeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAddNoticeWindow();
            }
        });
        deleteNoticeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openDeleteNoticeWindow();
            }
        });
        configBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openGlobalConfigWindow();
            }
        });
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (activeAdminWindow != null) activeAdminWindow.remove();
            }
        });

        content.add(addNoticeBtn).width(400).pad(10).row();
        content.add(deleteNoticeBtn).width(400).pad(10).row();
        content.add(configBtn).width(400).pad(10).row();
        content.add(backBtn).width(200).padTop(20).row();

        activeAdminWindow = new Window("Admin Panel", skin);
        activeAdminWindow.setModal(true);
        activeAdminWindow.add(content);
        activeAdminWindow.pack();
        activeAdminWindow.setPosition(
            (stage.getWidth() - activeAdminWindow.getWidth()) / 2,
            (stage.getHeight() - activeAdminWindow.getHeight()) / 2
        );
        stage.addActor(activeAdminWindow);
    }

    private void openAddNoticeWindow() {
        if (activeAdminWindow != null) activeAdminWindow.remove();

        Table content = new Table(skin);
        content.pad(20);

        final TextField dayField = new TextField("", skin);
        final TextField titleField = new TextField("", skin);
        final TextArea bodyArea = new TextArea("", skin);
        TextButton saveButton = new TextButton("Save Notice", skin);
        TextButton backButton = new TextButton("Back", skin);

        dayField.getStyle().fontColor = Color.BLACK;
        titleField.getStyle().fontColor = Color.BLACK;
        bodyArea.getStyle().fontColor = Color.BLACK;
        saveButton.getLabel().setColor(Color.BLACK);
        backButton.getLabel().setColor(Color.BLACK);

        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int day = Integer.parseInt(dayField.getText());
                    String title = titleField.getText();
                    String body = bodyArea.getText();
                    Result result = mediator.adminAddNotice(day, title, body);
                    System.out.println("Admin Action: " + result.getMessage()); // For developer log
                    openAdminPanel(); // Go back to the main admin panel
                } catch (NumberFormatException e) {
                    System.out.println("Admin Action Error: Day must be a number.");
                }
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAdminPanel();
            }
        });

        content.add(new Label("Day (1-100):", skin)).left().getActor().setColor(Color.BLACK);
        content.add(dayField).width(100).row();
        content.add(new Label("Title:", skin)).left().getActor().setColor(Color.BLACK);
        content.add(titleField).width(300).row();
        content.add(new Label("Body:", skin)).left().top().getActor().setColor(Color.BLACK);
        content.add(bodyArea).width(300).height(150).row();

        Table buttonTable = new Table();
        buttonTable.add(saveButton).pad(10);
        buttonTable.add(backButton).pad(10);
        content.add(buttonTable).colspan(2).padTop(20);

        activeAdminWindow = new Window("Add New Notice", skin);
        activeAdminWindow.setModal(true);
        activeAdminWindow.add(content);
        activeAdminWindow.pack();
        activeAdminWindow.setPosition( (stage.getWidth() - activeAdminWindow.getWidth()) / 2, (stage.getHeight() - activeAdminWindow.getHeight()) / 2 );
        stage.addActor(activeAdminWindow);
    }


    private void openDeleteNoticeWindow() {
        if (activeAdminWindow != null) activeAdminWindow.remove();

        Table content = new Table(skin);
        content.pad(20);

        final List<String> noticeListWidget = new List<>(skin);
        noticeListWidget.getStyle().fontColorSelected = Color.BLACK; // Ensure text is readable

        MyLinkedList<String> titlesFromLogic = mediator.adminGetAllNoticeTitles();

        String[] noticeTitlesArray = titlesFromLogic.toArray(new String[0]);

        noticeListWidget.setItems(noticeTitlesArray);

        ScrollPane scrollPane = new ScrollPane(noticeListWidget, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);


        TextButton deleteButton = new TextButton("Delete Selected", skin);
        TextButton backButton = new TextButton("Back", skin);
        deleteButton.getLabel().setColor(Color.BLACK);
        backButton.getLabel().setColor(Color.BLACK);

        deleteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int selectedIndex = noticeListWidget.getSelectedIndex();
                if (selectedIndex != -1) {
                    Result result = mediator.adminDeleteNotice(selectedIndex);
                    System.out.println("Admin Action: " + result.getMessage());

                    openDeleteNoticeWindow();
                } else {
                    System.out.println("Admin Action: No notice selected.");
                }
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAdminPanel();
            }
        });

        content.add(new Label("Select a notice to delete:", skin)).left().getActor().setColor(Color.BLACK);
        content.row();
        content.add(scrollPane).width(400).height(200).padBottom(10).row();

        Table buttonTable = new Table();
        buttonTable.add(deleteButton).pad(10);
        buttonTable.add(backButton).pad(10);
        content.add(buttonTable);

        activeAdminWindow = new Window("Delete Notice", skin);
        activeAdminWindow.setModal(true);
        activeAdminWindow.add(content);
        activeAdminWindow.pack();
        activeAdminWindow.setPosition( (stage.getWidth() - activeAdminWindow.getWidth()) / 2, (stage.getHeight() - activeAdminWindow.getHeight()) / 2 );
        stage.addActor(activeAdminWindow);
    }

    private void openGlobalConfigWindow() {
        if (activeAdminWindow != null) activeAdminWindow.remove();

        final Properties props = mediator.adminGetGlobalConfig();
        final java.util.Map<String, Actor> editorWidgets = new java.util.HashMap<>();

        Table content = new Table(skin);
        content.pad(10);

        Table settingsTable = new Table(skin);
        ScrollPane scrollPane = new ScrollPane(settingsTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true,false);

        TextButton saveButton = new TextButton("Save Changes", skin);
        TextButton backButton = new TextButton("Back", skin);
        saveButton.getLabel().setColor(Color.BLACK);
        backButton.getLabel().setColor(Color.BLACK);


        settingsTable.add(new Label("--- Allowed Values ---", skin, "title")).colspan(2).pad(15).row();

        Label speedsLabel = new Label("Allowed Speeds (comma-separated):", skin);
        TextField speedsField = new TextField(props.getProperty("allowedSpeeds", ""), skin);
        speedsLabel.setColor(Color.BLACK);
        speedsField.getStyle().fontColor = Color.BLACK;
        editorWidgets.put("allowedSpeeds", speedsField);
        settingsTable.add(speedsLabel).left().pad(5);
        settingsTable.add(speedsField).width(250).left().pad(5).row();

        Label diffLabel = new Label("Allowed Difficulties (comma-separated):", skin);
        TextField diffField = new TextField(props.getProperty("allowedDifficulties", ""), skin);
        diffLabel.setColor(Color.BLACK);
        diffField.getStyle().fontColor = Color.BLACK;
        editorWidgets.put("allowedDifficulties", diffField);
        settingsTable.add(diffLabel).left().pad(5);
        settingsTable.add(diffField).width(250).left().pad(5).row();

        settingsTable.add(new Label("--- Default Values ---", skin, "title")).colspan(2).pad(15).row();

        Label defSpeedLabel = new Label("Default Speed:", skin);
        SelectBox<String> defSpeedSelect = new SelectBox<>(skin);
        defSpeedSelect.setItems(props.getProperty("allowedSpeeds", "").split(","));
        defSpeedSelect.setSelected(props.getProperty("defaultSpeed", ""));
        defSpeedLabel.setColor(Color.BLACK);
        editorWidgets.put("defaultSpeed", defSpeedSelect);
        settingsTable.add(defSpeedLabel).left().pad(5);
        settingsTable.add(defSpeedSelect).width(250).left().pad(5).row();

        Label defDiffLabel = new Label("Default Difficulty:", skin);
        SelectBox<String> defDiffSelect = new SelectBox<>(skin);
        defDiffSelect.setItems(props.getProperty("allowedDifficulties", "").split(","));
        defDiffSelect.setSelected(props.getProperty("defaultDifficulty", ""));
        defDiffLabel.setColor(Color.BLACK);
        editorWidgets.put("defaultDifficulty", defDiffSelect);
        settingsTable.add(defDiffLabel).left().pad(5);
        settingsTable.add(defDiffSelect).width(250).left().pad(5).row();

        settingsTable.add(new Label("--- Game Parameters ---", skin, "title")).colspan(2).pad(15).row();

        String[] numericKeys = {"game.turn.duration", "resource.generation.interval", "whitewalker.attack.interval", "game.speed"};
        for (String key : numericKeys) {
            Label keyLabel = new Label(key + ":", skin);
            TextField valueField = new TextField(props.getProperty(key, "0"), skin);
            keyLabel.setColor(Color.BLACK);
            valueField.getStyle().fontColor = Color.BLACK;
            editorWidgets.put(key, valueField);
            settingsTable.add(keyLabel).left().pad(5);
            settingsTable.add(valueField).width(250).left().pad(5).row();
        }

        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int changes = 0;
                for (String key : editorWidgets.keySet()) {
                    String originalValue = props.getProperty(key, "");
                    String newValue = "";

                    Actor widget = editorWidgets.get(key);
                    if (widget instanceof TextField) {
                        newValue = ((TextField) widget).getText().trim();
                    } else if (widget instanceof SelectBox) {
                        newValue = ((SelectBox<String>) widget).getSelected();
                    }

                    if (!originalValue.equals(newValue)) {
                        Result update = mediator.adminUpdateGlobalConfig(key, newValue);
                        System.out.println(update.getMessage());
                        if (!update.isSuccess()){
                            return;
                        }
                        changes++;
                    }
                }
                System.out.println("Admin Action: " + changes + " config value(s) updated.");
                openGlobalConfigWindow(); // Refresh panel to show saved values
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAdminPanel();
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(saveButton).pad(10);
        buttonTable.add(backButton).pad(10);

        content.add(new Label("Global Game Configuration", skin, "title")).row();
        content.add(scrollPane).width(800).height(350).pad(10).row();
        content.add(buttonTable).padTop(10);

        activeAdminWindow = new Window("Global Config", skin);
        activeAdminWindow.setModal(true);
        activeAdminWindow.add(content);
        activeAdminWindow.pack();
        activeAdminWindow.setPosition((stage.getWidth() - activeAdminWindow.getWidth()) / 2, (stage.getHeight() - activeAdminWindow.getHeight()) / 2);
        stage.addActor(activeAdminWindow);
    }

    @Override
    public void onMessage(String text) {
        messageLabel.setText(text);
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
