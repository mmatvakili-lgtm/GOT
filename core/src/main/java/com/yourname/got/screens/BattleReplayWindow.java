package com.yourname.got.screens;

import com.yourname.got.logic.util.GameLog;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.utils.Scaling;
import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Building.Port;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.EventTextLog;
import com.yourname.got.logic.util.EventRoundUpdate;
import com.yourname.got.logic.util.EventHeroDuel;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.ArmyManager;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.yourname.got.logic.model.Army.KingdomSoldierRegistry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.ResourceItem;
import com.yourname.got.logic.util.Result;
import com.yourname.got.logic.util.Transaction;
import com.yourname.got.mediator.GameMediator;
import java.awt.Point;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.badlogic.gdx.graphics.Color;
import com.yourname.got.mediator.GameMessageListener;
import org.jetbrains.annotations.NotNull;
import static com.yourname.got.logic.login.GameFileUtility.getBuildingTexture;
import com.badlogic.gdx.math.Vector2;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The battle replay window (moved out of GamePlayScreen unchanged). It only needs the skin, the stage, the mediator and
 * three small helpers of the screen, which are handed in.
 */
public class BattleReplayWindow {
    private final Skin skin;
    private final Stage stage;
    private final GameMediator mediator;
    private final Consumer<String> onError;
    private final Function<String, String> formatter;
    private final BiFunction<String, Integer, TextureRegionDrawable> drawables;

    public BattleReplayWindow(Skin skin, Stage stage, GameMediator mediator, Consumer<String> onError,
                              Function<String, String> formatter, BiFunction<String, Integer, TextureRegionDrawable> drawables) {
        this.skin = skin;
        this.stage = stage;
        this.mediator = mediator;
        this.onError = onError;
        this.formatter = formatter;
        this.drawables = drawables;
    }

public void open(final GameMediator.BattleHistoryInfo historyInfo) {
    if (historyInfo == null || historyInfo.replayFilePath == null || historyInfo.replayFilePath.equals("N/A")) {
        onError.accept("Replay file not found.");
        return;
    }

    ReplayData replayData = mediator.loadReplayLog(historyInfo.replayFilePath);
    if (replayData == null || replayData.getEvents() == null || replayData.getEvents().isEmpty()) {
        onError.accept("Replay data is corrupted or empty.");
        return;
    }

    final MyLinkedList<BattleEvent> events = replayData.getEvents();
    final String attackerName = (replayData.getAttackerName() != null ? replayData.getAttackerName() : "Attacker");
    final String defenderName = (replayData.getDefenderName() != null ? replayData.getDefenderName() : "Defender");

    final Dialog panel = new Dialog("Battle Replay", skin, "default");
    panel.setModal(true);
    // --- تغییر ۱: افزایش سایز کلی پنل ---
    panel.setSize(1000, 820);
    panel.setPosition((stage.getWidth() - 1000) / 2f, (stage.getHeight() - 820) / 2f);

    // --- بخش ۱: هدر و نوارهای سلامتی ---
    Table topTable = new Table();
    topTable.pad(10);

    Label lblAtk = new Label("[ATK] " + attackerName, skin);
    lblAtk.setColor(Color.ORANGE);
    lblAtk.setFontScale(1.2f); // فونت بزرگتر

    Label lblDef = new Label("[DEF] " + defenderName, skin);
    lblDef.setColor(Color.CYAN);
    lblDef.setFontScale(1.2f); // فونت بزرگتر

    ProgressBar.ProgressBarStyle barStyle = skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class);
    barStyle.background.setMinHeight(25);
    barStyle.knobBefore.setMinHeight(25);

    // --- رفع باگ: تنظیم پویا و دقیق ظرفیت نوار سلامتی بر اساس حجم واقعی ارتش ---
    long maxAtkHp = 100; // حداقل جان
    long maxDefHp = 100;

    // جستجو در رویدادها برای پیدا کردن جان اولیه دو ارتش در لحظه شروع نبرد
    for (BattleEvent e : events) {
        if (e instanceof EventRoundUpdate) {
            maxAtkHp = Math.max(100, ((EventRoundUpdate)e).attackerTotalHealth);
            maxDefHp = Math.max(100, ((EventRoundUpdate)e).defenderTotalHealth);
            break; // فقط رکورد اول مد نظر است
        }
    }

    final ProgressBar attackerHealthBar = new ProgressBar(0, maxAtkHp, 1, false, barStyle);
    attackerHealthBar.setValue(maxAtkHp);
    final ProgressBar defenderHealthBar = new ProgressBar(0, maxDefHp, 1, false, barStyle);
    defenderHealthBar.setValue(maxDefHp);
    // -------------------------------------------------------------------------

    // چیدمان ردیف اول (نام‌ها)
    topTable.add(lblAtk).left().padBottom(5);
    topTable.add().expandX();
    topTable.add(lblDef).right().padBottom(5).row();

    // چیدمان ردیف دوم (نوارها)
    // عرض نوارها را به 350 افزایش دادیم
    topTable.add(attackerHealthBar).width(350).left();
    Label vsLabel = new Label(" VS ", skin);
    vsLabel.setFontScale(1.5f);
    vsLabel.setColor(Color.LIGHT_GRAY);
    topTable.add(vsLabel).pad(10);
    topTable.add(defenderHealthBar).width(350).right();

    // --- بخش ۲: ناحیه لاگ (Log Area) ---
    final Table logTable = new Table();
    logTable.top().left();

    final ScrollPane logScroll = new ScrollPane(logTable, skin);
    logScroll.setFadeScrollBars(false);

    panel.getContentTable().add(topTable).growX().padBottom(15).row();

    Table logContainer = new Table(skin);
    logContainer.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.6f)));
    // --- تغییر ۳: افزایش ارتفاع اسکرول ---
    logContainer.add(logScroll).expand().fill().pad(10).minHeight(450); // ارتفاع بیشتر

    // Visual part of the replay: a small map of the defender's kingdom next to the text log. It is fed by the
    // structured events (setup / target / destroyed) in the playback loop below.
    final BattlefieldView battlefield = new BattlefieldView(skin,
        (buildingName, buildingLevel) -> drawables.apply(buildingName, Math.max(0, buildingLevel - 1)), 22f);
    Table battlefieldBox = new Table(skin);
    battlefieldBox.setBackground(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.6f)));
    Label battlefieldTitle = new Label("Battlefield", skin);
    battlefieldTitle.setFontScale(0.8f);
    battlefieldBox.add(battlefieldTitle).padTop(6f).row();
    battlefieldBox.add(battlefield).size(battlefield.getWidth(), battlefield.getHeight()).pad(8f).row();

    // Both armies, unit by unit (soldiers left, losses, health) - fed by the army-state events of newer replays.
    final ArmyStatusPanel armyPanel = new ArmyStatusPanel(skin);
    armyPanel.add(new Label("Armies appear when the fighting starts.", skin)).left();
    ScrollPane armyScroll = new ScrollPane(armyPanel, skin);
    armyScroll.setFadeScrollBars(false);
    armyScroll.setScrollingDisabled(true, false);
    Table armyBox = new Table(skin);
    armyBox.setBackground(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.6f)));
    armyBox.add(armyScroll).width(battlefield.getWidth() + 16f).height(230f).pad(6f);

    Table leftColumn = new Table();
    leftColumn.add(battlefieldBox).row();
    leftColumn.add(armyBox).padTop(8f).row();

    Table replayBody = new Table();
    replayBody.add(leftColumn).top().padRight(10f);
    replayBody.add(logContainer).expand().fill();
    panel.getContentTable().add(replayBody).expand().fill().row();

    // --- بخش ۳: کنترلر پخش ---
    final boolean[] isPaused = {false};
    final float[] playSpeed = {1.0f};
    final float baseDelay = 0.8f;
    final int[] currentIndex = {0};
    final float[] timeToNextEvent = {0.5f};
    final boolean[] stepPending = {false}; // "Step" while paused: play until the next text event is shown

    final Slider timeline = new Slider(0, events.size(), 1, false, skin);
    final boolean[] programmatic = {false};
    final boolean[] ended = {false}; // the "END OF BATTLE" line was added (the driver stays alive so a seek can resume)
    final long[] initialHealth = {maxAtkHp, maxDefHp};

    Action replayDriver = new Action() {
        @Override
        public boolean act(float delta) {
            if (isPaused[0] && !stepPending[0]) return false;

            if (stepPending[0]) timeToNextEvent[0] = 0f;
            timeToNextEvent[0] -= delta * playSpeed[0];
            if (timeToNextEvent[0] <= 0f) {
                if (currentIndex[0] >= events.size()) {
                    // پایان
                    if (ended[0]) return false;
                    ended[0] = true;
                    Table endRow = new Table(skin);
                    endRow.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f)));
                    Label endLabel = new Label("--- END OF BATTLE ---", skin);
                    endLabel.setColor(Color.GOLD);
                    endLabel.setFontScale(1.2f);
                    endRow.add(endLabel).pad(15);

                    stepPending[0] = false;
                    logTable.add(endRow).growX().padTop(20).row();
                    logScroll.layout();
                    logScroll.setScrollPercentY(1.0f);
                    return false;
                }

                BattleEvent event = events.get(currentIndex[0]++);
                programmatic[0] = true;
                timeline.setValue(currentIndex[0]);
                programmatic[0] = false;
                battlefield.setPlaybackSpeed(playSpeed[0]);
                battlefield.handle(event);
                if (event instanceof com.yourname.got.logic.util.EventArmyState armyState) {
                    armyPanel.show(armyState);
                }
                if (event instanceof com.yourname.got.logic.util.VisualEvent visual) {
                    // Pictures only (army state / walking / blows): they have no text line; some wait a moment.
                    timeToNextEvent[0] = visual.pauseSeconds();
                    if (visual.pauseSeconds() > 0f) stepPending[0] = false;
                    return false;
                }
                if (event instanceof com.yourname.got.logic.util.EventBattleSetup
                        || event instanceof com.yourname.got.logic.util.EventBuildingFocus
                        || event instanceof com.yourname.got.logic.util.EventBuildingDestroyed) {
                    // Map-only events take no time of their own: the text event next to them sets the pace.
                    timeToNextEvent[0] = 0f;
                    return false;
                }
                addEventToLogTable(logTable, event, skin, attackerHealthBar, defenderHealthBar);
                stepPending[0] = false;

                logScroll.layout();
                logScroll.setScrollPercentY(1.0f);
                timeToNextEvent[0] = baseDelay;
            }
            return false;
        }
    };
    stage.addAction(replayDriver);

    // دکمه‌های کنترل
    Table controls = new Table();
    TextButton btnPause = new TextButton("Pause", skin);
    btnPause.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) {
        isPaused[0] = !isPaused[0];
        btnPause.setText(isPaused[0] ? "Resume" : "Pause");
    }});

    TextButton btnSpeed = new TextButton("Speed: 1x", skin);
    btnSpeed.addListener(new ChangeListener() {
        @Override public void changed(ChangeEvent e, Actor a) {
            if(playSpeed[0] == 1f) { playSpeed[0]=2f; btnSpeed.setText("Speed: 2x"); }
            else if(playSpeed[0] == 2f) { playSpeed[0]=4f; btnSpeed.setText("Speed: 4x"); }
            else if(playSpeed[0] == 4f) { playSpeed[0]=8f; btnSpeed.setText("Speed: 8x"); }
            else { playSpeed[0]=1f; btnSpeed.setText("Speed: 1x"); }
        }
    });

    TextButton btnStep = new TextButton("Step", skin);
    btnStep.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) {
        // Step works while paused (it pauses first, so one click = one event).
        if (!isPaused[0]) { isPaused[0] = true; btnPause.setText("Resume"); }
        stepPending[0] = true;
    }});

    TextButton btnSkip = new TextButton("Skip to end", skin);
    btnSkip.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) {
        isPaused[0] = false;
        btnPause.setText("Pause");
        playSpeed[0] = 1000f; // one event per frame, i.e. practically instant
        btnSpeed.setText("Speed: max");
    }});

    // Jump to any moment of the battle: the picture is rebuilt by replaying the events up to there at once.
    timeline.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) {
        if (programmatic[0]) return;
        int target = (int) timeline.getValue();
        battlefield.reset();
        armyPanel.reset();
        armyPanel.add(new Label("...", skin)).left();
        logTable.clearChildren();
        attackerHealthBar.setValue(initialHealth[0]);
        defenderHealthBar.setValue(initialHealth[1]);
        battlefield.setPlaybackSpeed(1000f);
        int i = 0;
        for (BattleEvent ev : events) {
            if (i++ >= target) break;
            battlefield.handle(ev);
            if (ev instanceof com.yourname.got.logic.util.EventArmyState st) armyPanel.show(st);
            if (!(ev instanceof com.yourname.got.logic.util.VisualEvent)
                    && !(ev instanceof com.yourname.got.logic.util.EventBattleSetup)
                    && !(ev instanceof com.yourname.got.logic.util.EventBuildingFocus)
                    && !(ev instanceof com.yourname.got.logic.util.EventBuildingDestroyed)) {
                addEventToLogTable(logTable, ev, skin, attackerHealthBar, defenderHealthBar);
            }
        }
        battlefield.act(1000f); // let every animation finish so the picture shows the final state of that moment
        logScroll.layout();
        logScroll.setScrollPercentY(1.0f);
        currentIndex[0] = target;
        ended[0] = false;
        timeToNextEvent[0] = 0.3f;
    }});
    panel.getContentTable().add(timeline).growX().padTop(6f).row();

    controls.add(btnPause).width(130).height(50).pad(8);
    controls.add(btnStep).width(100).height(50).pad(8);
    controls.add(btnSpeed).width(150).height(50).pad(8);
    controls.add(btnSkip).width(150).height(50).pad(8);

    panel.getButtonTable().add(controls).row();
    TextButton closeBtn = new TextButton("Close", skin);
    closeBtn.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
            stage.getRoot().removeAction(replayDriver);
            panel.remove();
        }
    });
    panel.getButtonTable().add(closeBtn).width(200).height(50).padTop(10);

    panel.show(stage);
}

    private void addEventToLogTable(Table logTable, BattleEvent event, Skin skin, ProgressBar atkBar, ProgressBar defBar) {
        Table row = new Table(skin);
        row.pad(5);

        if (event instanceof EventTextLog) {
            String msg = ((EventTextLog) event).message;

            // فیلتر کردن پیام‌های فنی
            if (msg.contains("calc") || msg.contains("totalDamage")) return;

            // ترجمه و تمیز کردن متن (تبدیل به انگلیسی)
            String displayMsg = formatBattleMessage(msg);

            Label lbl = new Label(displayMsg, skin);
            lbl.setWrap(true);

            // رنگ‌بندی بر اساس محتوا (انگلیسی)
            if (displayMsg.contains("Started") || displayMsg.contains("Round")) {
                row.setBackground(skin.newDrawable("white", new Color(0, 0.5f, 0, 0.3f)));
                lbl.setColor(Color.WHITE);
                lbl.setFontScale(1.0f);
            }
            else if (displayMsg.contains("Attacker") || displayMsg.contains("Attackers")) {
                lbl.setColor(new Color(1f, 0.7f, 0.7f, 1f)); // قرمز روشن
                Label tag = new Label("[ATK]", skin); // جایگزین شمشیر
                tag.setColor(Color.ORANGE);
                row.add(tag).padRight(10);
            }
            else if (displayMsg.contains("Defender") || displayMsg.contains("Defenders") || displayMsg.contains("Archers")) {
                lbl.setColor(new Color(0.7f, 0.8f, 1f, 1f)); // آبی روشن
                Label tag = new Label("[DEF]", skin); // جایگزین سپر
                tag.setColor(Color.CYAN);
                row.add(tag).padRight(10);
            }
            else if (displayMsg.contains("Victory") || displayMsg.contains("Defeated") || displayMsg.contains("Won")) {
                lbl.setColor(Color.GOLD);
                row.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0, 0.5f)));
            } else {
                lbl.setColor(Color.LIGHT_GRAY);
            }

            row.add(lbl).growX().left();
            logTable.add(row).growX().padBottom(2).row();

        } else if (event instanceof EventRoundUpdate) {
            EventRoundUpdate update = (EventRoundUpdate) event;
            atkBar.setValue(update.attackerTotalHealth);
            defBar.setValue(update.defenderTotalHealth);

        } else if (event instanceof EventHeroDuel) {
            EventHeroDuel duel = (EventHeroDuel) event;
            Label lbl = new Label(">>> HERO DUEL: " + duel.attackerHero + " vs " + duel.defenderHero, skin);
            lbl.setColor(Color.ORANGE);
            lbl.setFontScale(1.1f);
            row.add(lbl).center();
            row.setBackground(skin.newDrawable("white", new Color(0.5f, 0, 0, 0.5f)));
            logTable.add(row).growX().padTop(10).padBottom(10).row();
        }
    }
}
