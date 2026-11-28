package com.yourname.got.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.login.PlayerResult;
import com.yourname.got.logic.model.GameRecord;
import com.yourname.got.logic.system.EndingScript;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.mediator.GameMediator;

import java.util.List;
import java.util.Random;

/**
 * The ending of the game as a short film: the player's own kingdom (its background, its words, its story), slow camera
 * moves, weather that matches the mood of every shot (blizzard, embers, ash, golden dust), cinema bars, lightning and a
 * typed caption. Click or press a key to go to the next shot; "Skip" goes straight to the final standings.
 * Anything that goes wrong while drawing falls back to the standings, so the game can always be left.
 */
public class EndingCinematicScreen implements Screen {
    private static final float FADE = 0.9f;
    private static final int PARTICLES = 220;

    private final GoTGame game;
    private final GameMediator mediator;
    private final GameRecord record;
    private final Stage stage = new Stage(new ScreenViewport());
    private final Skin skin = UiTheme.loadSkin();
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture pixel;
    private Texture background;

    private final EndingScript.Kind kind;
    private final String kingdom;
    private final String playerName;
    private final List<EndingScript.Shot> shots;

    private int shotIndex = 0;
    private float shotTime = 0f;
    private float totalTime = 0f;
    private boolean standingsShown = false;
    private boolean failed = false;
    private Table standingsRoot;

    private final Label title, caption;
    private final float[] px = new float[PARTICLES], py = new float[PARTICLES], pv = new float[PARTICLES], ps = new float[PARTICLES], pp = new float[PARTICLES];
    private final Random rng = new Random(11);
    private float flash = 0f, nextFlash = 3f;

    public EndingCinematicScreen(GoTGame game, GameMediator mediator, GameRecord record, GameUser user) {
        this.game = game;
        this.mediator = mediator;
        this.record = record;
        this.playerName = user == null ? "" : user.getUsername();
        String k = null;
        try { if (user != null && user.getKingdom() != null) k = user.getKingdom().getType().name(); } catch (RuntimeException ignored) {}
        this.kingdom = k;

        PlayerResult mine = null;
        int survivors = 0, players = 0;
        for (PlayerResult r : record.getPlayerResults()) {
            players++;
            if (r.getStatus() != PlayerResult.PlayerStatus.DEFEATED) survivors++;
            if (r.getUsername().equals(playerName)) mine = r;
        }
        this.kind = EndingScript.kindFor(record.getFinalGameResult(), mine == null ? null : mine.getStatus());
        this.shots = EndingScript.build(kind, kingdom, playerName, mine == null ? players : mine.getFinalRank(), survivors, players);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        loadBackground();

        title = new Label("", skin, "title");
        title.setAlignment(Align.center);
        caption = new Label("", skin);
        caption.setWrap(true);
        caption.setAlignment(Align.center);
        caption.setFontScale(1.15f);
        stage.addActor(title);
        stage.addActor(caption);

        TextButton skip = new TextButton("Skip  >>", skin);
        skip.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { showStandings(); }
        });
        skip.setSize(120f, 36f);
        skip.setName("skip");
        stage.addActor(skip);

        for (int i = 0; i < PARTICLES; i++) respawn(i, true);
        try {
            boolean good = kind == EndingScript.Kind.DAWN || kind == EndingScript.Kind.THRONE;
            if (game.getMusicManager() != null) game.getMusicManager().playMusic(good ? "music_victory" : "music_game_over");
        } catch (RuntimeException e) {
            GameLog.error("Ending music could not start: " + e);
        }
    }

    private void loadBackground() {
        String name;
        String k = kingdom == null ? "" : kingdom;
        switch (k) {
            case "WINTERFELL": name = "snowy"; break;
            case "EYRIE": name = "arryn"; break;
            case "IRON_ISLANDS": name = "greyjoy"; break;
            case "CASTERLY_ROCK": name = "lannister"; break;
            case "STORMS_END": name = "bratheon"; break;
            case "HIGHGARDEN": name = "tyrell"; break;
            case "DRAGONSTONE": name = "targaryen"; break;
            default: name = null;
        }
        if (name == null) return;
        boolean good = kind == EndingScript.Kind.DAWN || kind == EndingScript.Kind.THRONE;
        try {
            background = new Texture(Gdx.files.internal("images/" + name + (good ? "-background-safe.jpg" : "-background-destroyed.jpg")));
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (RuntimeException e) {
            background = null; // plain colour instead
        }
    }

    private EndingScript.Shot shot() { return shots.get(Math.min(shotIndex, shots.size() - 1)); }

    private void respawn(int i, boolean anywhere) {
        float w = Math.max(1, Gdx.graphics.getWidth()), h = Math.max(1, Gdx.graphics.getHeight());
        px[i] = rng.nextFloat() * w;
        py[i] = anywhere ? rng.nextFloat() * h : h + 10f;
        pv[i] = 0.4f + rng.nextFloat();
        ps[i] = 1.5f + rng.nextFloat() * 3.5f;
        pp[i] = rng.nextFloat() * 6.28f;
    }

    private void nextShot() {
        if (standingsShown) return;
        if (shotIndex + 1 >= shots.size()) { showStandings(); return; }
        shotIndex++;
        shotTime = 0f;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        Gdx.gl.glClearColor(0.01f, 0.01f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        try {
            if (!standingsShown && !failed) {
                shotTime += delta;
                totalTime += delta;
                if (shotTime >= shot().seconds) nextShot();
                if (!standingsShown) drawFilm(delta);
            }
        } catch (RuntimeException e) {
            GameLog.error("The ending film failed, showing the standings instead: " + e);
            failed = true;
            showStandings();
        }
        stage.act(delta);
        stage.draw();
    }

    private void drawFilm(float delta) {
        EndingScript.Shot s = shot();
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float p = Math.min(1f, shotTime / s.seconds);
        float fade = Math.min(Math.min(shotTime / FADE, (s.seconds - shotTime) / FADE), 1f);
        fade = Math.max(0f, fade);

        float[] tint = tintFor(s.mood);
        batch.begin();
        if (background != null) {
            float zoom = 1.06f + 0.10f * p;
            float bw = Math.max(w, h * background.getWidth() / (float) background.getHeight()) * zoom;
            float bh = bw * background.getHeight() / background.getWidth();
            float driftX = (p - 0.5f) * 0.04f * w;
            batch.setColor(tint[0] * fade, tint[1] * fade, tint[2] * fade, 1f);
            batch.draw(background, (w - bw) / 2f + driftX, (h - bh) / 2f, bw, bh);
        } else {
            batch.setColor(tint[0] * 0.5f * fade, tint[1] * 0.5f * fade, tint[2] * 0.5f * fade, 1f);
            batch.draw(pixel, 0, 0, w, h);
        }

        drawWeather(s.mood, delta, w, h, fade);

        // lightning in the dark shots
        if (s.mood == EndingScript.Mood.STORM || s.mood == EndingScript.Mood.NIGHT) {
            nextFlash -= delta;
            if (nextFlash <= 0f) { flash = 0.55f; nextFlash = 2.5f + rng.nextFloat() * 4f; }
        }
        if (flash > 0f) {
            batch.setColor(0.85f, 0.92f, 1f, flash * fade);
            batch.draw(pixel, 0, 0, w, h);
            flash = Math.max(0f, flash - delta * 1.8f);
        }

        // vignette and cinema bars
        float bar = h * 0.11f * Math.min(1f, totalTime / 1.2f);
        batch.setColor(0f, 0f, 0f, 0.35f);
        batch.draw(pixel, 0, 0, w * 0.06f, h);
        batch.draw(pixel, w * 0.94f, 0, w * 0.06f, h);
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(pixel, 0, 0, w, bar);
        batch.draw(pixel, 0, h - bar, w, bar);
        batch.setColor(Color.WHITE);
        batch.end();

        // typed caption
        int chars = (int) Math.min(s.text.length(), shotTime * 38f);
        title.setText(shotTime > 0.4f ? s.title : "");
        title.setColor(titleColor(s.mood, Math.min(1f, Math.max(0f, (shotTime - 0.4f) / 0.8f)) * fade));
        title.setFontScale(1.35f);
        caption.setText(s.text.substring(0, chars));
        caption.setColor(1f, 1f, 1f, fade);
        float capW = w * 0.66f;
        caption.setWidth(capW);
        caption.setPosition((w - capW) / 2f, bar + h * 0.06f);
        caption.setHeight(h * 0.16f);
        caption.setAlignment(Align.top, Align.center);
        title.pack();
        title.setPosition((w - title.getWidth()) / 2f, bar + h * 0.26f);
        stage.getRoot().findActor("skip").setPosition(w - 140f, bar + 12f);
    }

    private float[] tintFor(EndingScript.Mood m) {
        switch (m) {
            case NIGHT: return new float[]{0.28f, 0.40f, 0.65f};
            case STORM: return new float[]{0.38f, 0.44f, 0.58f};
            case FIRE:  return new float[]{0.95f, 0.50f, 0.30f};
            case ASH:   return new float[]{0.34f, 0.32f, 0.34f};
            case DAWN:  return new float[]{1.00f, 0.86f, 0.62f};
            case GOLD:
            default:    return new float[]{1.00f, 0.90f, 0.66f};
        }
    }

    private Color titleColor(EndingScript.Mood m, float a) {
        switch (m) {
            case NIGHT: case STORM: return new Color(0.75f, 0.88f, 1f, a);
            case FIRE: return new Color(1f, 0.72f, 0.40f, a);
            case ASH: return new Color(0.78f, 0.76f, 0.78f, a);
            default: return new Color(0.98f, 0.82f, 0.38f, a);
        }
    }

    private void drawWeather(EndingScript.Mood mood, float delta, float w, float h, float fade) {
        boolean snow = mood == EndingScript.Mood.NIGHT || mood == EndingScript.Mood.STORM;
        boolean embers = mood == EndingScript.Mood.FIRE;
        boolean ash = mood == EndingScript.Mood.ASH;
        for (int i = 0; i < PARTICLES; i++) {
            float sway = (float) Math.sin(totalTime * 0.9f + pp[i]) * 25f;
            if (snow) {
                py[i] -= (60f + 140f * pv[i]) * delta * (mood == EndingScript.Mood.STORM ? 1.8f : 1f);
                px[i] += (mood == EndingScript.Mood.STORM ? 160f : 30f) * delta + sway * delta;
            } else if (embers) {
                py[i] += (50f + 120f * pv[i]) * delta;
                px[i] += sway * delta;
            } else if (ash) {
                py[i] -= (20f + 40f * pv[i]) * delta;
                px[i] += sway * 0.5f * delta;
            } else { // dawn and gold: slow dust of light rising
                py[i] += (12f + 25f * pv[i]) * delta;
                px[i] += sway * 0.3f * delta;
            }
            if (py[i] < -12f || py[i] > h + 12f || px[i] > w + 12f || px[i] < -12f) {
                respawn(i, false);
                if (!(embers || mood == EndingScript.Mood.DAWN || mood == EndingScript.Mood.GOLD)) py[i] = h + 10f;
                else py[i] = -10f;
            }
            if (snow) batch.setColor(0.93f, 0.97f, 1f, 0.85f * fade);
            else if (embers) batch.setColor(1f, 0.55f + 0.3f * pv[i], 0.15f, 0.9f * fade);
            else if (ash) batch.setColor(0.7f, 0.68f, 0.7f, 0.6f * fade);
            else batch.setColor(1f, 0.9f, 0.55f, 0.55f * fade);
            batch.draw(pixel, px[i], py[i], ps[i], ps[i]);
        }
    }

    // ------------------------------------------------------------------ standings
    private void showStandings() {
        if (standingsShown) return;
        standingsShown = true;
        title.remove();
        caption.remove();
        Actor skipBtn = stage.getRoot().findActor("skip");
        if (skipBtn != null) skipBtn.remove();

        Table root = new Table();
        standingsRoot = root;
        root.setFillParent(true);
        root.setBackground(skin.newDrawable("white", new Color(0.03f, 0.04f, 0.08f, 0.94f)));
        Label head = new Label(kind.headline, skin, "title");
        head.setFontScale(1.6f);
        head.setColor(kind == EndingScript.Kind.DAWN || kind == EndingScript.Kind.THRONE ? UiTheme.GOLD : new Color(0.75f, 0.88f, 1f, 1f));
        root.add(head).padTop(30f).row();
        Label outcome = new Label("Outcome: " + record.getFinalGameResult() + "     Duration: " + record.getGameDuration(), skin);
        outcome.setFontScale(0.8f);
        outcome.setColor(UiTheme.MUTED);
        root.add(outcome).padBottom(18f).row();

        Table board = new Table(skin);
        board.setBackground(skin.newDrawable("white", new Color(0.10f, 0.12f, 0.17f, 1f)));
        board.add(new Label("Rank", skin)).pad(8f).expandX();
        board.add(new Label("Player", skin)).pad(8f).expandX();
        board.add(new Label("Score", skin)).pad(8f).expandX();
        board.add(new Label("Status", skin)).pad(8f).expandX().row();
        for (PlayerResult r : record.getPlayerResults()) {
            Label status = new Label(r.getStatus().toString(), skin);
            status.setColor(r.getStatus() == PlayerResult.PlayerStatus.WINNER ? UiTheme.GOLD
                : r.getStatus() == PlayerResult.PlayerStatus.SURVIVED ? UiTheme.GOOD : UiTheme.BAD);
            Label name = new Label(r.getUsername(), skin);
            if (r.getUsername().equals(playerName)) name.setColor(UiTheme.GOLD);
            board.add(new Label(String.valueOf(r.getFinalRank()), skin)).pad(6f);
            board.add(name).pad(6f);
            board.add(new Label(String.valueOf(r.getScore()), skin)).pad(6f);
            board.add(status).pad(6f).row();
        }
        ScrollPane scroll = new ScrollPane(board, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(Math.min(760f, Gdx.graphics.getWidth() * 0.8f)).height(Gdx.graphics.getHeight() * 0.5f).padBottom(20f).row();

        TextButton replay = new TextButton("Watch again", skin);
        replay.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { restart(); }
        });
        TextButton leave = new TextButton("Return to Main Menu", skin, skin.has("primary", TextButton.TextButtonStyle.class) ? "primary" : "default");
        leave.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { mediator.finishGameForUser(); }
        });
        Table buttons = new Table();
        buttons.add(replay).width(200f).height(44f).padRight(14f);
        buttons.add(leave).width(280f).height(44f);
        root.add(buttons);
        stage.addActor(root);
        UiTheme.fadeIn(root);
    }

    /** Plays the film again from the first shot (no new screen: this is called from a button of this screen). */
    private void restart() {
        if (standingsRoot != null) { standingsRoot.remove(); standingsRoot = null; }
        shotIndex = 0;
        shotTime = 0f;
        totalTime = 0f;
        failed = false;
        standingsShown = false;
        stage.addActor(title);
        stage.addActor(caption);
        TextButton skip = new TextButton("Skip  >>", skin);
        skip.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { showStandings(); }
        });
        skip.setSize(120f, 36f);
        skip.setName("skip");
        stage.addActor(skip);
    }

    // ------------------------------------------------------------------ screen plumbing
    @Override
    public void show() {
        InputMultiplexer mux = new InputMultiplexer(stage, new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (standingsShown) return false;
                if (keycode == Input.Keys.ESCAPE) { showStandings(); return true; }
                nextShot();
                return true;
            }
            @Override public boolean touchDown(int x, int y, int pointer, int button) {
                if (standingsShown) return false;
                nextShot();
                return true;
            }
        });
        Gdx.input.setInputProcessor(mux);
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { if (Gdx.input.getInputProcessor() != null) Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        pixel.dispose();
        if (background != null) background.dispose();
    }
}
