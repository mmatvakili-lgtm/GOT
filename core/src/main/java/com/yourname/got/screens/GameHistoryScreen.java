// In file: GameHistoryScreen.java (Replace the old content with this)

package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.PlayerResult;
import com.yourname.got.logic.login.UserSystem;
import com.yourname.got.logic.model.GameRecord;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import com.yourname.got.mediator.GameMediator;

public class GameHistoryScreen implements Screen {

    private final GoTGame game;
    private final GameMediator mediator;
    private final GameUser currentUser;
    private final Stage stage;
    private final Skin skin;
    private final Texture background;

    public GameHistoryScreen(GoTGame game, GameMediator mediator, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.currentUser = user;
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));
        this.background = new Texture("images/background-loding.jpg");

        createUI();
    }

    private void createUI() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label titleLabel = new Label("Game History", skin, "title");
        titleLabel.setFontScale(1.5f);

        // *** START OF CORRECTION ***
        // از متد موجود در UserSystem شما برای بارگذاری سوابق استفاده می‌کنیم
        Result recordsResult = UserSystem.loadAllUserRecords(currentUser);
        MyLinkedList<GameRecord> gameRecords = new MyLinkedList<>();

        if (recordsResult.isSuccess()) {
            gameRecords = (MyLinkedList<GameRecord>) recordsResult.getGameInstance();
        }
        // *** END OF CORRECTION ***

        Table listTable = new Table(skin);
        if (gameRecords == null || gameRecords.isEmpty()) {
            listTable.add(new Label("No game history found.", skin));
        } else {
            for (final GameRecord record : gameRecords) {
                Table row = new Table();
                row.pad(5);

                String resultText = record.getFinalGameResult().toString().replace("_", " ");
                Label gameNameLabel = new Label(record.getGameName() + " (" + resultText + ")", skin);
                TextButton detailsButton = new TextButton("View Details", skin);

                detailsButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        showGameRecordDetailsDialog(record);
                    }
                });

                row.add(gameNameLabel).expandX().left().padLeft(10);
                row.add(detailsButton).right().padRight(10);
                listTable.add(row).width(500).padBottom(8).row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);

        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // نام این کلاس باید با نام فایل صفحه لابی شما یکی باشد
                game.setScreen(new GameLoadingScreen(game, mediator, currentUser));
            }
        });

        rootTable.add(titleLabel).padBottom(20).row();
        rootTable.add(scrollPane).width(550).height(400).row();
        rootTable.add(backButton).width(300).padTop(20).row();
    }

    // In file: GameHistoryScreen.java
// Replace the entire showGameRecordDetailsDialog method with this one

    private void showGameRecordDetailsDialog(GameRecord record) {
        Dialog dialog = new Dialog("Game Details: " + record.getGameName(), skin, "default");
        dialog.setModal(true);
        dialog.pad(20);

        Table contentTable = new Table(skin);
        contentTable.defaults().pad(5).align(Align.left);

        // --- START OF FIX: Removed "default-bold" from all Labels ---
        contentTable.add(new Label("Final Outcome:", skin)).left(); // No more "default-bold"
        contentTable.add(new Label(record.getFinalGameResult().toString().replace("_", " "), skin)).left().row();

        contentTable.add(new Label("Game Duration:", skin)).left(); // No more "default-bold"
        contentTable.add(new Label(record.getGameDuration(), skin)).left().row();

        contentTable.add(new Label("Player Rankings:", skin, "title")).colspan(2).padTop(15).padBottom(5).row();

        Table playersTable = new Table(skin);
        playersTable.defaults().pad(3).align(Align.center);
        playersTable.add(new Label("Rank", skin)).padRight(15);      // No more "default-bold"
        playersTable.add(new Label("Player", skin)).width(150);     // No more "default-bold"
        playersTable.add(new Label("Status", skin)).width(120).row(); // No more "default-bold"
        // --- END OF FIX ---

        int rank = 1;
        MyLinkedList<PlayerResult> playerResults = record.getPlayerResults();
        if(playerResults != null && !playerResults.isEmpty()) {
            for (PlayerResult result : playerResults) {
                playersTable.add(new Label(String.valueOf(rank++), skin)).padRight(15);
                playersTable.add(new Label(result.getUsername(), skin));
                Label statusLabel = new Label(result.getStatus().toString(), skin);
                if (result.getStatus() == PlayerResult.PlayerStatus.WINNER) statusLabel.setColor(Color.GOLD);
                if (result.getStatus() == PlayerResult.PlayerStatus.SURVIVED) statusLabel.setColor(Color.GREEN);
                if (result.getStatus() == PlayerResult.PlayerStatus.DEFEATED) statusLabel.setColor(Color.FIREBRICK);
                playersTable.add(statusLabel).row();
            }
        }

        ScrollPane playersScrollPane = new ScrollPane(playersTable, skin);
        contentTable.add(playersScrollPane).colspan(2).height(200).width(450).row();

        dialog.getContentTable().add(contentTable);
        dialog.button("Close", true);
        dialog.show(stage);
    }

    @Override
    public void show() { Gdx.input.setInputProcessor(stage); }
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }
    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override
    public void pause() { }
    @Override
    public void resume() { }
    @Override
    public void hide() { }
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        background.dispose();
    }
}
