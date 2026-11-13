package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.UserSystem;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.mediator.GameMessageListener;
import com.yourname.got.logic.util.Result;

import java.util.List;
import java.util.Properties;

public class GameJoinScreen implements Screen, GameMessageListener {

    private final GoTGame game;
    private final Stage stage;
    private final Skin skin;
    private final Texture background;
    private final GameMediator mediator;
    private final GameUser currentUser;

    private Table notInGamePanel;
    private Table playerInGamePanel;
    private Table ownerInGamePanel;
    private Label errorLabel;
    private ButtonGroup<TextButton> gameButtonGroup;
    private Timer.Task refreshTask;
    private Window activeSettingsWindow;
    // One colour for the lobby players panel. It used to be created with alpha 0.5 and re-set to a
    // different colour (alpha 0.85) on every refresh, so the lobby background visibly changed.
    private static final Color LOBBY_PANEL_COLOR = new Color(0.05f, 0.05f, 0.1f, 0.85f);

    /** Puts exactly one panel into the layout. */
    private void showPanel(Table panel) {
        contentCell.setActor(panel);
        notInGamePanel.setVisible(panel == notInGamePanel);
        playerInGamePanel.setVisible(panel == playerInGamePanel);
        ownerInGamePanel.setVisible(panel == ownerInGamePanel);
        contentCell.getTable().invalidateHierarchy();
    }

    private boolean isSettingsOpen() {
        return activeSettingsWindow != null && activeSettingsWindow.getStage() != null;
    }

    private void closeSettingsWindow() {
        if (activeSettingsWindow != null) {
            activeSettingsWindow.remove();
            activeSettingsWindow = null; // it stayed non-null after closing, which froze the lobby refresh
        }
    }

    private List<String> gameDataList;

    private com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> contentCell;
    private Table lobbyPlayersTable; // جدول نمایش بازیکنان حاضر در لابی
    // The owner panel and the player panel each need their OWN roster table: an actor has only one parent,
    // so the single shared table ended up in the owner panel and the waiting page of normal players stayed empty.
    private Table playerRosterTable;

    private Table footerTable; // جدول ثابت پایین صفحه برای دکمه‌ها

    public GameJoinScreen(GoTGame game, GameMediator mediator, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.currentUser = user;
        this.stage = new Stage(new ScreenViewport());
        this.background = new Texture("images/background-loding.jpg");
        this.skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));
        createUI();
    }

    @Override
    public void onMessage(String message) {
        // All messages from mediator will be appended to the errorLabel (single-line/stack)
        showError(message);
        // Also refresh view in case message implies state change
        scheduleRefreshAndModeUpdate();
    }


    private void createUI() {
        // ۱. ساخت جدول اصلی محتوا
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        stage.addActor(rootTable);

        // ۲. ساخت جدول ثابت پایین صفحه (اصلاح شده)
        footerTable = new Table();

        // ❌ این خط قبلی را پاک کنید: footerTable.setFillParent(true);
        // چون این خط باعث می‌شد سیاهی کل صفحه را بگیرد.

        // ✅ تنظیمات جدید:
        footerTable.setWidth(stage.getWidth()); // عرض جدول برابر با عرض صفحه
        footerTable.setPosition(0, 0); // مکان جدول: پایین-چپ (مختصات 0,0)
        footerTable.bottom(); // چسباندن محتویات داخل جدول به کف

        // اضافه کردن پس‌زمینه فقط به این نوار پایین
        footerTable.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.8f)));
        stage.addActor(footerTable);

        // ۳. تنظیمات محتوای وسط (بدون تغییر)
        Label titleLabel = new Label("Game Lobbies", skin);
        // ... (بقیه کدهای متد createUI بدون تغییر باقی بماند)
        titleLabel.setFontScale(1.5f);
        lobbyPlayersTable = new Table(skin);
        lobbyPlayersTable.setBackground(skin.newDrawable("white", LOBBY_PANEL_COLOR));
        playerRosterTable = new Table(skin);
        playerRosterTable.setBackground(skin.newDrawable("white", LOBBY_PANEL_COLOR));

        createNotInGamePanel();
        createPlayerInGamePanel();
        createOwnerInGamePanel();

        errorLabel = new Label("", skin);
        errorLabel.setColor(Color.RED);

        rootTable.add(titleLabel).padBottom(20).row();
        // Only the panel that is shown lives in the layout. Before, all three panels were stacked in the table and
        // the hidden ones still took space: after saving the settings the (hidden) game list was rebuilt, the
        // visible roster jumped down about 110 px and the title was pushed out of the screen.
        contentCell = rootTable.add(notInGamePanel);
        contentCell.row();
        rootTable.add(errorLabel).height(30).padTop(10).row();
        rootTable.add(new Label("", skin)).height(80);
    }

    private void createNotInGamePanel() {
        notInGamePanel = new Table();
    }


    private void createPlayerInGamePanel() {
        playerInGamePanel = new Table();
        playerInGamePanel.setVisible(false);

        Label statusLabel = new Label("⏳ Awaiting Orders from Lord Commander...", skin);
        statusLabel.setColor(Color.ORANGE);
        statusLabel.setFontScale(1.2f);
        statusLabel.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.5f, 0.8f), Actions.alpha(1f, 0.8f))));

        Table gameInfoTable = new Table(skin);
        gameInfoTable.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)));
        gameInfoTable.pad(20);

        if (currentUser.getGame() != null) {
            Properties props = mediator.getGameConfig(currentUser.getGame().getId());
            Label title = new Label("📜 Battle Decree", skin);
            title.setColor(Color.CYAN);
            gameInfoTable.add(title).colspan(2).padBottom(15).row();
            addInfoRow(gameInfoTable, "Game Speed:", props.getProperty("game.speed", "1.0") + "x");
            addInfoRow(gameInfoTable, "Difficulty:", props.getProperty("game.difficulty", "Normal"));
            addInfoRow(gameInfoTable, "Turn Duration:", props.getProperty("game.turn.duration", "30") + "s");
        }

        Table tipTable = new Table(skin);
        tipTable.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.6f)));
        Label tipLabel = new Label(getRandomTip(), skin);
        tipLabel.setColor(Color.LIGHT_GRAY);
        tipLabel.setWrap(true);
        tipLabel.setAlignment(Align.center);
        tipTable.add(tipLabel).width(500).pad(10);

        // --- چیدمان (بدون دکمه‌ها) ---
        playerInGamePanel.add(statusLabel).colspan(2).padBottom(20).row();
        Table centerLayout = new Table();
        centerLayout.add(playerRosterTable).width(450).minHeight(300).fillY().padRight(15);
        centerLayout.add(gameInfoTable).width(350).fillY();
        playerInGamePanel.add(centerLayout).colspan(2).row();
        playerInGamePanel.add(tipTable).colspan(2).padTop(25).padBottom(15).row();
    }

    // یک متد کمکی کوچک برای تمیز شدن کد بالا
    private void addInfoRow(Table table, String label, String value) {
        Label l = new Label(label, skin);
        l.setColor(Color.LIGHT_GRAY);
        table.add(l).left().padRight(10).padBottom(5);

        Label v = new Label(value, skin);
        v.setColor(Color.WHITE);
        table.add(v).right().padBottom(5).row();

        // خط نازک زیر هر ردیف
        table.add(new Image(skin.newDrawable("white", new Color(1,1,1,0.1f)))).height(1).colspan(2).fillX().padBottom(5).row();
    }


    private void createOwnerInGamePanel() {
        ownerInGamePanel = new Table();
        ownerInGamePanel.setVisible(false);

        Label statusLabel = new Label("You are the owner of this lobby.", skin);
        statusLabel.setColor(Color.GOLD);

        ownerInGamePanel.add(statusLabel).padBottom(20).row();

        // فقط لیست بازیکنان اینجا می‌ماند
        ownerInGamePanel.add(lobbyPlayersTable).width(600).minHeight(350).padBottom(20).row();
    }

    private void rebuildGameListUI() {
        notInGamePanel.clear();

        Label listTitle = new Label("Available Games:", skin);
        notInGamePanel.add(listTitle).left().padBottom(10).row();

        Table gameListTable = new Table();
        ScrollPane scrollPane = new ScrollPane(gameListTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);

        this.gameDataList = mediator.getGameList();

        if (gameDataList.isEmpty()) {
            gameListTable.add(new Label("No active games found", skin));
        } else {
            for (String gameLine : gameDataList) {
                String[] parts = gameLine.split(",");
                if (parts.length < 5) continue;

                final String name = parts[0];
                final String passwordInfo = parts[1];
                final int id = Integer.parseInt(parts[2]);
                final String playersCount = parts[4];

                Table row = new Table();
                row.add(new Label(name + " (" + playersCount + "/7)", skin)).expandX().left();
                TextButton joinButton = new TextButton("Join", skin);
                row.add(joinButton).right();

                joinButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        onJoinButtonClicked(gameDataList.indexOf(gameLine) + 1, passwordInfo);
                    }
                });

                gameListTable.add(row).width(400).padBottom(5).row();
            }
        }
        notInGamePanel.add(scrollPane).width(450).height(180).row();
    }

    private void onJoinButtonClicked(int selectedIndex, String passwordInfo) {
        boolean requiresPassword = !"-1".equals(passwordInfo);

        if (requiresPassword) {
            final TextField passwordField = new TextField("", skin);
            passwordField.setPasswordMode(true);
            passwordField.setPasswordCharacter('*');

            Dialog passwordDialog = new Dialog("Password Required", skin) {
                @Override
                protected void result(Object object) {
                    if (Boolean.TRUE.equals(object)) {
                        String enteredPassword = passwordField.getText().trim();
                        if (enteredPassword.isEmpty()) {
                            showError("Password cannot be empty.");
                            return;
                        }
                        mediator.joinGame(selectedIndex, gameDataList, enteredPassword);
                    }
                }
            };

            passwordDialog.text("This game requires a password:");
            passwordDialog.getContentTable().add(passwordField).width(200).row();
            passwordDialog.button("Confirm", true);
            passwordDialog.button("Cancel", false);
            passwordDialog.show(stage);

        } else {
            // No password needed, just join
            mediator.joinGame(selectedIndex, gameDataList, "-1");
        }
    }

    private void openReadOnlySettingsWindow(int gameId) {
        closeSettingsWindow();

        final Properties gameProps = mediator.getGameConfig(gameId);

        Table content = new Table(skin);
        content.pad(10);
        Table settingsTable = new Table(skin);
        ScrollPane scrollPane = new ScrollPane(settingsTable, skin);
        scrollPane.setScrollingDisabled(true, false);

        TextButton backButton = new TextButton("Back", skin);
        backButton.getLabel().setColor(Color.BLACK);

        settingsTable.add(new Label("--- Game Settings ---", skin, "title")).colspan(2).pad(15).row();

        String[] allKeys = {"game.speed", "game.difficulty", "game.turn.duration", "resource.generation.interval", "whitewalker.attack.interval"};
        for (String key : allKeys) {
            Label keyLabel = new Label(key + ":", skin);
            Label valueLabel = new Label(gameProps.getProperty(key, "N/A"), skin);
            keyLabel.setColor(Color.BLACK);
            valueLabel.setColor(Color.BLACK);

            settingsTable.add(keyLabel).left().pad(5);
            settingsTable.add(valueLabel).left().pad(5).row();
        }

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeSettingsWindow();
            }
        });

        content.add(new Label("View Settings for: " + (currentUser.getGame().getName() != null ? currentUser.getGame().getName() : "Current Game"), skin)).row();
        content.add(scrollPane).width(600).height(350).pad(10).row();
        content.add(backButton).padTop(10);

        activeSettingsWindow = new Window("Game Settings (View Only)", skin);
        activeSettingsWindow.setModal(true);
        activeSettingsWindow.add(content);
        activeSettingsWindow.pack();
        activeSettingsWindow.setPosition((stage.getWidth() - activeSettingsWindow.getWidth()) / 2, (stage.getHeight() - activeSettingsWindow.getHeight()) / 2);
        stage.addActor(activeSettingsWindow);
    }


    private void showLeaveConfirmationDialog() {
        Dialog dialog = new Dialog("Leave Game", skin) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    mediator.leaveCurrentGame();
                }
            }
        };
        dialog.text("Are you sure you want to leave this game lobby?");
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(stage);
    }

    private void showError(String message) {
        if (message == null) return;
        errorLabel.setText(message);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if(message.equals(errorLabel.getText().toString())) {
                    errorLabel.setText("");
                }
            }
        }, 5);
    }

    private void scheduleRefreshAndModeUpdate() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                rebuildGameListUI();
                updateViewMode();
            }
        }, 0.3f);
    }

// در کلاس GameJoinScreen.java، تغییر در متد updateViewMode:

    private void updateViewMode() {
        errorLabel.setText("");

        if (currentUser.getInGame() && currentUser.getGame() != null) {
            // --- رفع باگ شیفت شدن: اگر تنظیمات باز است، رفرش کل صفحه را متوقف کن تا پرش رخ ندهد ---
            if (isSettingsOpen()) {
                return;
            }

            Result result = mediator.getGameDetails(currentUser.getGame().getName(), currentUser.getGame().getId());
            if (result.isSuccess()) {
                GameOfThrones userGame = (GameOfThrones) result.getGameInstance();
                mediator.setCurrentGame(userGame);

                if (userGame.isGameReady()) {
                    // The owner started the game: the waiting players used to stay on this page for ever.
                    closeSettingsWindow();
                    mediator.enterStartedGame(userGame);
                    return;
                }

                // این تابع جدول بازیکنان را پاک کرده و دوباره پر می‌کند (بدون اضافه کردن فریم جدید به stage)
                updateLobbyPlayerList(userGame);

                boolean isOwner = userGame.getOwner().getUsername().equals(currentUser.getUsername());

                showPanel(isOwner ? ownerInGamePanel : playerInGamePanel);

                updateFooterBar(isOwner, true);

            } else {
                currentUser.setInGame(false);
                currentUser.setGame(null);
                com.yourname.got.logic.login.UserSystem.updateUser(currentUser);
                updateViewMode();
            }
        } else {
            showPanel(notInGamePanel);

            // --- جلوگیری از تلمبار شدن بازی‌های موجود ---
            if (!isSettingsOpen()) {
                rebuildGameListUI();
            }
            updateFooterBar(false, false);
        }
    }
    private void openGameSettingsWindow(int gameId) {
        closeSettingsWindow();

        final Properties gameProps = mediator.getGameConfig(gameId);
        final Properties globalProps = mediator.adminGetGlobalConfig();
        final java.util.Map<String, Actor> editorWidgets = new java.util.HashMap<>();

        Table content = new Table(skin);
        content.pad(10);
        Table settingsTable = new Table(skin);
        ScrollPane scrollPane = new ScrollPane(settingsTable, skin);
        scrollPane.setScrollingDisabled(true, false);

        TextButton saveButton = new TextButton("Save Changes", skin);
        TextButton backButton = new TextButton("Back", skin);
        saveButton.getLabel().setColor(Color.BLACK);
        backButton.getLabel().setColor(Color.BLACK);

        settingsTable.add(new Label("--- Game Settings ---", skin, "title")).colspan(2).pad(15).row();

        Label speedLabel = new Label("Game Speed:", skin);
        SelectBox<String> speedSelect = new SelectBox<>(skin);
        speedSelect.setItems(globalProps.getProperty("allowedSpeeds", "1.0").split(","));
        speedSelect.setSelected(gameProps.getProperty("game.speed", "1.0"));
        speedLabel.setColor(Color.BLACK);
        editorWidgets.put("game.speed", speedSelect);
        settingsTable.add(speedLabel).left().pad(5);
        settingsTable.add(speedSelect).width(250).left().pad(5).row();

        Label diffLabel = new Label("Difficulty:", skin);
        SelectBox<String> diffSelect = new SelectBox<>(skin);
        diffSelect.setItems(globalProps.getProperty("allowedDifficulties", "Normal").split(","));
        diffSelect.setSelected(gameProps.getProperty("game.difficulty", "Normal"));
        diffLabel.setColor(Color.BLACK);
        editorWidgets.put("game.difficulty", diffSelect);
        settingsTable.add(diffLabel).left().pad(5);
        settingsTable.add(diffSelect).width(250).left().pad(5).row();

        String[] numericKeys = {"game.turn.duration", "resource.generation.interval", "whitewalker.attack.interval"};
        for (String key : numericKeys) {
            Label keyLabel = new Label(key + ":", skin);
            TextField valueField = new TextField(gameProps.getProperty(key, "0"), skin);
            keyLabel.setColor(Color.BLACK);
            TextField.TextFieldStyle blackStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
            blackStyle.fontColor = Color.BLACK;      // own copy: changing getStyle() altered the style shared by the whole skin
            valueField.setStyle(blackStyle);
            editorWidgets.put(key, valueField);
            settingsTable.add(keyLabel).left().pad(5);
            settingsTable.add(valueField).width(250).left().pad(5).row();
        }

        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int changes = 0;
                for (String key : editorWidgets.keySet()) {
                    String originalValue = gameProps.getProperty(key, "");
                    String newValue = "";
                    Actor widget = editorWidgets.get(key);
                    if (widget instanceof TextField) newValue = ((TextField) widget).getText().trim();
                    else if (widget instanceof SelectBox) newValue = ((SelectBox<String>) widget).getSelected();

                    if (!originalValue.equals(newValue)) {
                        Result result = mediator.updateGameConfig(gameId, key, newValue);
                        if (result.isSuccess()) changes++;
                    }
                }
                showError(changes + " setting(s) updated successfully.");
                closeSettingsWindow();
                scheduleRefreshAndModeUpdate();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeSettingsWindow();
            }
        });

        Table buttonTable = new Table();
        buttonTable.add(saveButton).pad(10);
        buttonTable.add(backButton).pad(10);

        content.add(new Label("Edit Settings for: " + currentUser.getGame().getName(), skin)).row();
        content.add(scrollPane).width(600).height(350).pad(10).row();
        content.add(buttonTable).padTop(10);

        activeSettingsWindow = new Window("Game Settings", skin);
        activeSettingsWindow.setModal(true);
        activeSettingsWindow.add(content);
        activeSettingsWindow.pack();
        activeSettingsWindow.setPosition((stage.getWidth() - activeSettingsWindow.getWidth()) / 2,
            (stage.getHeight() - activeSettingsWindow.getHeight()) / 2);
        stage.addActor(activeSettingsWindow);
    }


    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        mediator.setMessageListener(this);
        updateViewMode();

        refreshTask = new Timer.Task() {
            @Override
            public void run() {
                updateViewMode();
            }
        };
        Timer.schedule(refreshTask, 2, 3); // 10 s felt like the lobby never updated
    }

    @Override
    public void hide() {
        mediator.setMessageListener(game);
        if (refreshTask != null) refreshTask.cancel();
    }

    @Override
    public void dispose() {
        if (refreshTask != null) refreshTask.cancel();
        closeSettingsWindow();
        stage.dispose();
        skin.dispose();
        background.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        // The batch keeps the colour of the last actor drawn in the previous frame (e.g. the green START button):
        // without this the background was tinted green, and orange again whenever a white window was drawn last.
        stage.getBatch().setColor(com.badlogic.gdx.graphics.Color.WHITE);
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }

    // متد اصلاح شده برای لیست بازیکنان (خواناتر و زیباتر)
    private void updateLobbyPlayerList(GameOfThrones game) {
        fillRoster(lobbyPlayersTable, game);   // owner panel
        fillRoster(playerRosterTable, game);   // waiting page of the other players
    }

    private void fillRoster(Table lobbyPlayersTable, GameOfThrones game) {
        lobbyPlayersTable.clear();
        lobbyPlayersTable.top();

        // تغییر ۱: پس‌زمینه تیره‌تر (0.8f) و کمی مایل به سرمه‌ای برای شیک شدن
        lobbyPlayersTable.setBackground(skin.newDrawable("white", LOBBY_PANEL_COLOR));
        lobbyPlayersTable.pad(10); // فاصله داخلی

        // هدر جدول با رنگ طلایی
        Label headerLabel = new Label("⚔️ Active Roster (" + game.getNumberOfPlayers() + "/7)", skin);
        headerLabel.setColor(Color.GOLD);
        headerLabel.setFontScale(1.1f);

        lobbyPlayersTable.add(headerLabel).colspan(2).padBottom(10).row();

        // خط جداکننده
        lobbyPlayersTable.add(new Image(skin.newDrawable("white", Color.DARK_GRAY))).height(2).colspan(2).fillX().padBottom(10).row();

        GameUser[] users = game.getUsers();
        if (users != null) {
            for (int i = 0; i < users.length; i++) {
                GameUser u = users[i];
                if (u != null) {
                    boolean isOwner = u.getUsername().equals(game.getOwner().getUsername());
                    String role = isOwner ? " [LORD]" : " [Soldier]";

                    // آیکون کوچک (متنی) برای زیبایی
                    String prefix = isOwner ? "👑 " : "🛡️ ";

                    Label nameLabel = new Label(prefix + u.getUsername(), skin);
                    Label roleLabel = new Label(role, skin);

                    // رنگ‌بندی: مالک طلایی، بازیکن خودمان سبز، بقیه سفید
                    if (isOwner) {
                        nameLabel.setColor(Color.GOLD);
                        roleLabel.setColor(Color.GOLD);
                    } else if (u.getId() == currentUser.getId()) {
                        nameLabel.setColor(Color.GREEN);
                        roleLabel.setColor(Color.GREEN);
                    } else {
                        nameLabel.setColor(Color.WHITE);
                        roleLabel.setColor(Color.LIGHT_GRAY);
                    }

                    lobbyPlayersTable.add(nameLabel).left().padLeft(10).expandX();
                    lobbyPlayersTable.add(roleLabel).right().padRight(10).row();

                    // خط جداکننده کمرنگ بین بازیکنان
                    lobbyPlayersTable.add(new Image(skin.newDrawable("white", new Color(1,1,1,0.1f)))).height(1).colspan(2).fillX().pad(5).row();
                }
            }
        }

        // پر کردن فضای خالی
        if (game.getNumberOfPlayers() < 7) {
            Label waitLabel = new Label("Waiting for warriors...", skin);
            waitLabel.setColor(Color.GRAY);
            waitLabel.setAlignment(Align.center);
            lobbyPlayersTable.add(waitLabel).colspan(2).expandY().top().padTop(20).row();
        }
    }
    // یک لیست از جملات جذاب یا آموزشی برای نمایش در لابی
    private String getRandomTip() {
        String[] tips = {
            "\"Winter is Coming... prepare your stockpiles.\"",
            "Tip: Spies can reveal the enemy's resources before you attack.",
            "\"Chaos isn't a pit. Chaos is a ladder.\"",
            "Tip: Upgrading your Castle allows for more buildings.",
            "\"The man who passes the sentence should swing the sword.\"",
            "Tip: Don't forget to repair your Wall after a siege.",
            "\"A Lannister always pays his debts.\"",
            "Tip: The market prices fluctuate based on supply and demand."
        };
        return tips[(int) (Math.random() * tips.length)];
    }

    private void updateFooterBar(boolean isOwner, boolean inGame) {
        footerTable.clear();
        footerTable.pad(15);

        // 1. دکمه Back
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { mediator.goToGameLoading(); }
        });
        footerTable.add(backButton).width(150).height(50).left();

        footerTable.add().expandX();

        // 2. دکمه‌های سمت راست
        if (inGame) {
            if (isOwner) {
                TextButton settingsBtn = new TextButton("Settings", skin);
                TextButton deleteBtn = new TextButton("Delete Lobby", skin);
                TextButton startBtn = new TextButton("START GAME", skin);
                startBtn.setColor(Color.GREEN);
                deleteBtn.setColor(Color.FIREBRICK);

                settingsBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { if(currentUser.getGame() != null) openGameSettingsWindow(currentUser.getGame().getId()); } });
                deleteBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { mediator.deleteCurrentGame(); } });

                startBtn.addListener(new ChangeListener() {
                    @Override public void changed(ChangeEvent e, Actor a) {
                        if(currentUser.getGame() != null) {
                            Result r = mediator.startGameFromUI(currentUser, currentUser.getGame().getId());
                            errorLabel.setText(r.getMessage());
                        }
                    }
                });

                // *** اصلاح حیاتی: استفاده از mediator.getCurrentGame() برای دریافت تعداد واقعی ***
                GameOfThrones freshGame = mediator.getCurrentGame();
                // شرط را روی 1 گذاشتیم تا با ۱ نفر هم بتوانید تست کنید
                if (freshGame != null && freshGame.getNumberOfPlayers() < 1) {
                    startBtn.setDisabled(true);
                    startBtn.setColor(Color.GRAY);
                }

                footerTable.add(deleteBtn).width(175).height(50).padRight(10);
                footerTable.add(settingsBtn).width(150).height(50).padRight(10);
                footerTable.add(startBtn).width(200).height(60);

            } else {
                TextButton leaveBtn = new TextButton("Abandon Lobby", skin);
                leaveBtn.setColor(Color.FIREBRICK);
                leaveBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { showLeaveConfirmationDialog(); } });

                footerTable.add(leaveBtn).width(200).height(50);
            }
        }
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
}
