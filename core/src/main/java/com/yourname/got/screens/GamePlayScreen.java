package com.yourname.got.screens;

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
//import java.awt.*;
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

//import javax.swing.*;
import static com.yourname.got.logic.login.GameFileUtility.getBuildingTexture;
import com.badlogic.gdx.math.Vector2;

public class GamePlayScreen implements Screen, GameMessageListener , GameMediator.GamePanelPresenter {

    private final GoTGame game;
    private final GameMediator mediator;
    private final GameUser currentUser;
    private final GameOfThrones currentGame;

    private final Stage stage;
    private final Skin skin;
    private final Texture background;
    private final Texture tileTexture;
    private Table topRightButtons; // <-- این فیلد را اضافه کنید

    // left panel widgets
    private Label lblUsername, lblKingdom, lblDaySeason, lblWallStatus;
    private Label lblWood, lblFood, lblCoin, lblStone;
    private Label lblInfantry, lblArchers, lblCavalry, lblSpies ,lblHero;
    private Label lblTotalSoldiers;

    // message log
    private final LinkedList<String> messageLog = new LinkedList<>();
    private final List<Label> messageLabels = new ArrayList<>();
    private ScrollPane messagesScroll;
    private Table messagesTable;
    private Table rightPanelButtons; // <<-- NEW FIELD: To disable buttons later

    private String currentMusicTheme = ""; // <-- این متغیر جدید را اضافه کنید

    // center grid
    private final int GRID_SIZE = 11;
    private final ImageButton[][] tileButtons = new ImageButton[GRID_SIZE][GRID_SIZE];
    private final boolean[][] tileSelected = new boolean[GRID_SIZE][GRID_SIZE];
    private final Table gridTable = new Table();
    private int lastSelectedRow = -1;
    private int lastSelectedCol = -1;


    // right menu
    private Window activeDialog;
    private String currentMessageFilter = null;
    private float timeSinceLastUpdate = 0f;
    private final float REFRESH_INTERVAL = 1.0f;

    // small status/error
    private final Label statusLabel;
    private String buildingTypeToPlace = null;
    private String resourceTypeToPlace = null;

    private Building buildingToMove = null;
    private int oldRow = -1, oldCol = -1;

    // در قسمت تعریف متغیرهای کلاس
    private Window buildingInfoBubble;// پنجره شناور اطلاعات
    // متغیرهای نوار سلامتی (برای جلوگیری از ساخت مکرر و کرش کردن)
    private Drawable barBackground, barGreen, barOrange, barRed;
    private Texture tBlack, tGreen, tOrange, tRed;
    // در ابتدای کلاس GamePlayScreen
    private Texture whiteTexture; // برای رسم لایه رنگی روی صفحه
    private Color currentAtmosphereColor = new Color(0, 0, 0, 0f); // رنگ فعلی محیط
    // --- متغیرهای جدید برای جلوگیری از کرش ---
    private final Map<String, TextureRegionDrawable> buildingDrawableCache = new HashMap<>(); // کش ساختمان‌ها
    private Texture mapVisorTexture; // برای نقشه جهان
    private Texture shipTexture;     // برای کشتی

    // ... سایر متغیرها ...
    private Building currentSelectedBuilding = null; // ساختمان انتخاب شده فعلی
    // ...

    public GamePlayScreen(GoTGame game, GameMediator mediator, GameUser user, GameOfThrones gameData) {
        this.game = game;
        this.mediator = mediator;
        this.currentUser = user;
        this.currentGame = gameData;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // choose background
        String kingdomName = "No Kingdom";
        if (user != null && user.getKingdom() != null && user.getKingdom().getType() != null) {
            kingdomName = user.getKingdom().getType().toString();
        }
        String backgroundPath;
        switch (kingdomName) {
            case "WINTERFELL": backgroundPath = "images/snowy-background-safe.jpg"; break;
            case "STORMS_END": backgroundPath = "images/bratheon-background-safe.jpg"; break;
            case "IRON_ISLANDS": backgroundPath = "images/greyjoy-background-safe.jpg"; break;
            case "DRAGONSTONE": backgroundPath = "images/targaryen-background-safe.jpg"; break;
            case "HIGHGARDEN": backgroundPath = "images/tyrell-background-safe.jpg"; break;
            case "EYRIE": backgroundPath = "images/arryn-background-safe.jpg"; break;
            case "CASTERLY_ROCK": backgroundPath = "images/lannister-background-safe.jpg"; break;
            default: backgroundPath = "images/gameplay-background.jpg"; break;
        }

        this.background = safeLoad(backgroundPath);
        this.tileTexture = safeLoad("icon/tile_default.png");

        this.skin = new Skin(Gdx.files.internal("ui/plain-james-ui.json"));
        Drawable roundGray = skin.getDrawable("round-gray");
        skin.add("default-round", roundGray, Drawable.class);

        this.statusLabel = new Label("", skin);

        createUI();
    }

    // در GamePlayScreen.java
    private Texture safeLoad(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            System.err.println("Failed to load texture: " + path); // لاگ کردن فایل خراب
            // ساخت یک بافت ساده موقت برای جلوگیری از کرش
            Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            p.setColor(Color.GRAY);
            p.fill();
            Texture t = new Texture(p);
            p.dispose();
            return t;
        }
    }
    private void createUI() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        this.topRightButtons = new Table(); // <-- و با این خط جایگزین کنید

        Table root = new Table();
        root.setFillParent(true);
        root.pad(10);
        stage.addActor(root);

        // ساخت لایه اتمسفر (یک تصویر سفید ساده که رنگش رو تغییر میدیم)
        shipTexture = safeLoad("icon/ship-1.png");
        mapVisorTexture = safeLoad("images/map_visor.jpg");

        // *** خط حیاتی جدید: ساخت تکسچر سفید برای جلوگیری از کرش ***
        whiteTexture = createSinglePixelTexture(Color.WHITE);
        // ********************************************************

        // --- کد جدید: ساخت تکسچرهای نوار سلامتی (فقط یک بار) ---
        tBlack = createSinglePixelTexture(Color.BLACK);
        tGreen = createSinglePixelTexture(Color.GREEN);
        tOrange = createSinglePixelTexture(Color.ORANGE);
        tRed = createSinglePixelTexture(Color.RED);

        barBackground = new TextureRegionDrawable(new TextureRegion(tBlack));
        barGreen = new TextureRegionDrawable(new TextureRegion(tGreen));
        barOrange = new TextureRegionDrawable(new TextureRegion(tOrange));
        barRed = new TextureRegionDrawable(new TextureRegion(tRed));
        // -------------------------------------------------------
// اضافه کردن به استیج (باید روی بک‌گراند ولی زیر دکمه‌ها باشه، اما چون بعدا add میکنیم میاد رو)
// فعلا add میکنیم، در متد render ترتیب رو مدیریت میکنیم یا همینجا میذاریم آخر همه

        Table left = new Table(skin);
        left.top().left(); // چیدمان از بالا به چپ

        lblUsername = new Label("< " + safeUsername() + " >", skin);
        lblUsername.setFontScale(1.4f);
        lblKingdom = new Label("Lord of " + safeKingdomName(), skin);
        lblKingdom.setFontScale(0.9f);
        Table headerTable = new Table(skin);
        headerTable.add(lblUsername).padBottom(15).row();
        headerTable.add(lblKingdom);
        left.add(headerTable).padTop(120).padLeft(100).expandX().fillX().row();

        Table resourcesTable = new Table(skin);
        resourcesTable.defaults().pad(5);
        lblCoin = new Label("00co0", skin);
        lblWood = new Label("00wo0", skin);
        lblFood = new Label("00fo0", skin);
        lblStone = new Label("00st0", skin);
        lblCoin.setFontScale(0.8f);
        lblWood.setFontScale(0.8f);
        lblFood.setFontScale(0.8f);
        lblStone.setFontScale(0.8f);
        resourcesTable.add(lblCoin).expandX().left();
        resourcesTable.add(lblStone).expandX().left().padLeft(-40).row();
        resourcesTable.add(lblWood).expandX().left().padTop(13);
        resourcesTable.add(lblFood).expandX().left().padLeft(-40).padTop(13);
        left.add(resourcesTable).padTop(20).padLeft(42).expandX().fillX().row();

        Table stateTable = new Table(skin);
        lblDaySeason = new Label("0 -\n 0",skin);
        lblWallStatus = new Label("0 -\n 0",skin);
        stateTable.add(lblWallStatus).expandX().right().row();
        stateTable.add(lblDaySeason).expandX().padLeft(50).padTop(10).left();
        left.add(stateTable).padTop(12).expandX().fillX().row();

        Table soldiersTable = new Table(skin);
        soldiersTable.defaults().left().padLeft(10).padBottom(2);
        lblInfantry = new Label("Infantry: 0", skin);
        lblArchers = new Label("Archers: 0", skin);
        lblCavalry = new Label("Cavalry: 0", skin);
        lblSpies = new Label("Spies: 0", skin);
        lblHero = new Label("Hero: 0",skin);
        lblTotalSoldiers = new Label("Total: 0 / 0", skin);
        lblTotalSoldiers.setColor(Color.YELLOW);
        lblTotalSoldiers.setFontScale(1.1f);
        soldiersTable.add(lblHero).pad(5).row();
        soldiersTable.add(lblInfantry).pad(5).row();
        soldiersTable.add(lblArchers).pad(5).row();
        soldiersTable.add(lblCavalry).pad(5).row();
        soldiersTable.add(lblSpies).pad(5).row();
        soldiersTable.add(lblTotalSoldiers).padTop(8).row();
        left.add(soldiersTable).padTop(25).padLeft(2).expandX().fillX().row();

        left.add(statusLabel).pad(10).expandX().fillX().row();
        Table filters = new Table(skin);
        TextButton btnAll = new TextButton(" All ", skin);
        TextButton btnSystem = new TextButton(" System ", skin);
        TextButton btnError = new TextButton(" Error ", skin);
        TextButton btnMsg = new TextButton(" Message ", skin);

        filters.add(btnAll).padRight(4);
        filters.add(btnSystem).padRight(4);
        filters.add(btnError).padRight(4);
        filters.add(btnMsg).padRight(4);
        left.add(filters).pad(7).left().row();

        messagesTable = new Table(skin);
        messagesTable.left();
        messagesScroll = new ScrollPane(messagesTable, skin);
        messagesScroll.setFadeScrollBars(false);
        messagesScroll.setScrollingDisabled(true, false);
        left.add(messagesScroll).expandY().padLeft(3).fill().width(380).height(500).row();


        // center: map area
        Table center = new Table();
        center.pad(10);
        renderTilesAndUpdate();
        addPortIconsToStage();
        updateShipOnStage();


        Table topRightButtons = new Table();
        topRightButtons.top().right().padTop(22).padRight(11);
        topRightButtons.setFillParent(true);

        ImageButton.ImageButtonStyle squareStyle = new ImageButton.ImageButtonStyle();
        squareStyle.up = skin.getDrawable("round-gray");
        squareStyle.over = skin.getDrawable("round-light-gray");
        squareStyle.down = skin.getDrawable("round-dark-gray");

        ImageButton hamburgerBtn = new ImageButton(squareStyle);
        ImageButton mapBtn = new ImageButton(squareStyle);
        ImageButton messagesBtn = new ImageButton(squareStyle);
        ImageButton extraBtn = new ImageButton(squareStyle);

        float btnSize = 75f;
        hamburgerBtn.setSize(btnSize, btnSize);
        messagesBtn.setSize(btnSize, btnSize);
        mapBtn.setSize(btnSize, btnSize);
        extraBtn.setSize(btnSize, btnSize);

        topRightButtons.add(extraBtn).size(btnSize).padLeft(6);
        topRightButtons.add(mapBtn).size(btnSize).padLeft(6);
        topRightButtons.add(messagesBtn).size(btnSize).padLeft(6);
        topRightButtons.add(hamburgerBtn).size(btnSize).padLeft(6);
        stage.addActor(topRightButtons);

        Texture overlayTex = new Texture(Gdx.files.internal("icon/icons_overlay.png"));
        Image overlay = new Image(new TextureRegionDrawable(new TextureRegion(overlayTex)));
        overlay.setPosition(stage.getWidth() - overlay.getWidth() - 1,
            stage.getHeight() - overlay.getHeight() - 11);
        overlay.setTouchable(Touchable.disabled);
        stage.addActor(overlay);
        overlay.toFront();

        hamburgerBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Table content = new Table(skin);
                content.defaults().pad(20);

                TextButton changePasswordBtn = new TextButton("Change Password", skin);
                changePasswordBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (activeDialog != null) { activeDialog.remove(); activeDialog = null; }
                        openChangePasswordPanel();
                    }
                });
                content.add(changePasswordBtn).row();


                TextButton exitBtn = new TextButton("Exit", skin);
                exitBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.setScreen(new GameLoadingScreen(game, mediator, currentUser));
                    }
                });
                content.add(exitBtn).row();
                openCenteredWindow("Menu", content);
            }
        });

        mapBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openWorldMapPanel(); // فراخوانی متد جدید و زیباتر ما
            }
        });

        messagesBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openMessagesPanel();
            }
        });

        extraBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openExtraPanel();
            }
        });


        Table right = new Table(skin);
        right.defaults().padRight(0);
        this.rightPanelButtons = right;

        right.add(new Label("", skin)).padTop(61).row();
        addMenuButton(right, "New Building", this::openNewBuildingPanel);
        addMenuButton(right, "Overhaul", this::openOverhaulPanel);
        addMenuButton(right, "Destroy", this::openDestroyPanel);
        addMenuButton(right, "Move", this::openMovePanel);
        right.row().padTop(6);

        right.add(new Label("", skin)).pad(25).row();
        addMenuButton(right, "Train Soldiers", this::openTrainSoldiersPanel);
        addMenuButton(right, "Attack", this::openAttackPanel);

        addMenuButton(right, "History", this::openHistoryMainMenu);        addMenuButton(right, "Shipping", this::openShippingPanel);


        right.row().padTop(6);
        right.add(new Label("", skin)).pad(13).row();
        addMenuButton(right, "Kingdom", this::openKingdomPanel);
        addMenuButton(right, "WhiteWalkers", this::openWhiteWalkerMainMenuPanel);
        right.row().padTop(6);

        right.add(new Label("", skin)).pad(6).row();
        addMenuButton(right, "Buy", this::openBuyPanel);
        addMenuButton(right, "Sell", this::openSellPanel);
        right.row().padTop(6);

        right.add(new Label("", skin)).pad(5).row();
        addMenuButton(right, "Help", this::openHelpPanel);

        // layout root: left | center | right
        root.add(left).width(360).fillY().top().padRight(8);
        root.add(center).expand().fill().padRight(8);
        root.add(right).width(300).fillY().top();

        // initial update
        refreshDynamicInfo();

        btnAll.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                currentMessageFilter = null;
                refreshMessages();
            }
        });
        btnSystem.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                currentMessageFilter = "SYSTEM";
                refreshMessages();
            }
        });
        btnError.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                currentMessageFilter = "ERROR";
                refreshMessages();
            }
        });
        btnMsg.setText("Notifs");
        btnMsg.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                currentMessageFilter = "NOTIF";
                refreshMessages();
            }
        });

        addNotif("Welcome to the game!");
    }

    private void addMenuButton(Table container, String text, Runnable onClick) {
        TextButton b = new TextButton(text, skin);
        b.setSize(260, 29);
        b.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { onClick.run(); } });
        container.add(b).width(260).padRight(36).padTop(5).row();
    }

    private void openChangePasswordPanel() {
        final Dialog panel = new Dialog("Change Password", skin, "default");
        panel.setModal(true);
        panel.setSize(Gdx.graphics.getWidth() * 0.4f, Gdx.graphics.getHeight() * 0.5f);

        Table content = new Table(skin);
        content.pad(20).defaults().pad(10).left();

        final TextField currentPasswordField = new TextField("", skin);
        currentPasswordField.setPasswordMode(true);
        currentPasswordField.setPasswordCharacter('*');

        final TextField newPasswordField = new TextField("", skin);
        newPasswordField.setPasswordMode(true);
        newPasswordField.setPasswordCharacter('*');

        final TextField confirmNewPasswordField = new TextField("", skin);
        confirmNewPasswordField.setPasswordMode(true);
        confirmNewPasswordField.setPasswordCharacter('*');

        content.add(new Label("Current Password:", skin)).right();
        content.add(currentPasswordField).width(250).row();
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));


        content.add(new Label("New Password:", skin)).right();
        content.add(newPasswordField).width(250).row();

        content.add(new Label("Confirm New Password:", skin)).right();
        content.add(confirmNewPasswordField).width(250).row();

        TextButton confirmButton = new TextButton("Confirm Change", skin);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String current = currentPasswordField.getText();
                String newPass = newPasswordField.getText();
                String confirmPass = confirmNewPasswordField.getText();

                Result result = mediator.changeUserPassword(current, newPass, confirmPass);
                if (result.isSuccess()) {
                    addSystem(result.getMessage());
                    panel.remove();
                } else {
                    addError(result.getMessage());
                }
            }
        });

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panel.remove();
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.defaults().pad(5).width(120).height(40);
        buttonTable.add(confirmButton);
        buttonTable.add(cancelButton);

        panel.getContentTable().add(content).expandX().fillX().row();
        panel.getButtonTable().add(buttonTable).padTop(15).row();

        panel.setPosition(
            (stage.getWidth() - panel.getWidth()) / 2f,
            (stage.getHeight() - panel.getHeight()) / 2f
        );
        stage.addActor(panel);
    }


    private void clearSelection() {
        // 1. بازگرداندن رنگ دکمه قبلی به حالت عادی (اگر وجود داشته باشد)
        if (lastSelectedRow != -1 && lastSelectedCol != -1) {
            ImageButton prevBtn = tileButtons[lastSelectedRow][lastSelectedCol];
            if (prevBtn != null) {
                prevBtn.getImage().clearActions(); // <--- توقف انیمیشن
                prevBtn.getImage().setColor(Color.WHITE); // برگشت به رنگ عادی
            }
        }

        // 2. بستن پنجره شناور اطلاعات (اگر باز است)
        if (buildingInfoBubble != null) {
            buildingInfoBubble.clearActions(); // حذف انیمیشن‌های قبلی
            buildingInfoBubble.remove();       // حذف از روی صفحه
            buildingInfoBubble = null;
        }

        // 3. ریست کردن متغیرهای وضعیت
        setStatus("");
        lastSelectedRow = -1;
        lastSelectedCol = -1;

        this.currentSelectedBuilding = null;
    }


//    private void toggleTileSelection(int r, int c) {
//        // ۱. محاسبه آفست (دقیقاً مثل متد render)
//        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
//        if (userInGame == null || userInGame.getKingdom() == null) return;
//
//        int mapRows = userInGame.getKingdom().getUserMap().getLength();
//        int mapCols = userInGame.getKingdom().getUserMap().getWidth();
//
//        // محاسبه فاصله برای وسط‌چین کردن
//        int offsetRow = (mapRows - GRID_SIZE) / 2;
//        int offsetCol = (mapCols - GRID_SIZE) / 2;
//
//        // ۲. محاسبه مختصات واقعی در نقشه
//        // این مختصات دقیقاً همان چیزی است که در دیتابیس/آرایه اصلی ذخیره می‌شود
//        int realRow = r + offsetRow;
//        int realCol = c + offsetCol;
//
//        // --- مدیریت ساخت‌وساز ---
//        if (buildingTypeToPlace != null) {
//            // اصلاح: حذف +1. آفست خودش محاسبه شده است.
//            int buildX = realRow;
//            int buildY = realCol;
//
//            addSystem("Attempting to build " + buildingTypeToPlace + " at (" + buildX + ", " + buildY + ")...");
//            Result result = mediator.buildBuilding(buildingTypeToPlace, resourceTypeToPlace, buildX, buildY);
//
//            if (result.isSuccess()) {
//                addSystem("SUCCESS: " + result.getMessage());
//                buildingTypeToPlace = null;
//                resourceTypeToPlace = null;
//                renderTilesAndUpdate();
//                clearSelection();
//            } else {
//                addError("ERROR: " + result.getMessage());
//            }
//            return;
//        }
//
//        // --- مدیریت جابه‌جایی ---
//        if (buildingToMove != null) {
//            // اصلاح: حذف +1
//            int newX = realRow;
//            int newY = realCol;
//
//            // استفاده از oldRow و oldCol که قبلاً با آفست ذخیره شده‌اند
//            Result result = mediator.moveBuilding(oldRow, oldCol, newX, newY);
//
//            if (result.isSuccess()) {
//                addSystem("SUCCESS: " + result.getMessage());
//                buildingToMove = null;
//                oldRow = -1; oldCol = -1;
//                renderTilesAndUpdate();
//                clearSelection();
//            } else {
//                addError("ERROR: " + result.getMessage());
//            }
//            return;
//        }
//
//        // --- انتخاب عادی ---
//        if (lastSelectedRow == r && lastSelectedCol == c) {
//            clearSelection();
//            return;
//        }
//
//        if (lastSelectedRow != -1 && lastSelectedCol != -1) {
//            ImageButton prevBtn = tileButtons[lastSelectedRow][lastSelectedCol];
//            if (prevBtn != null) prevBtn.getImage().setColor(Color.WHITE);
//            if (buildingInfoBubble != null) {
//                buildingInfoBubble.remove();
//                buildingInfoBubble = null;
//            }
//        }
//
//        ImageButton currentBtn = tileButtons[r][c];
//        if (currentBtn != null) currentBtn.getImage().setColor(0.7f, 0.7f, 0.7f, 1f);
//
//        // ذخیره مختصات گرید (برای گرافیک)
//        lastSelectedRow = r;
//        lastSelectedCol = c;
//
//        // نمایش مختصات واقعی به کاربر (نمایش +1 فقط برای زیبایی بصری کاربر است، نه منطق)
//        setStatus(String.format("Selected tile: [%d, %d]", realRow, realCol));
//
//        // دریافت ساختمان با مختصات واقعی
//        Building clickedBuilding = mediator.getBuildingAt(realRow, realCol);
//
//        if (clickedBuilding != null) {
//            if (currentBtn != null) {
//                Vector2 coords = currentBtn.localToStageCoordinates(new Vector2(0, 0));
//                showBuildingInfoBubble(clickedBuilding, coords.x, coords.y);
//            }
//        } else {
//            if (buildingInfoBubble != null) {
//                buildingInfoBubble.remove();
//                buildingInfoBubble = null;
//            }
//        }
//    }


//    private void toggleTileSelection(int r, int c) {
//        // ۱. محاسبه آفست
//        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
//        if (userInGame == null || userInGame.getKingdom() == null) return;
//
//        int mapRows = userInGame.getKingdom().getUserMap().getLength();
//        int mapCols = userInGame.getKingdom().getUserMap().getWidth();
//        int offsetRow = (mapRows - GRID_SIZE) / 2;
//        int offsetCol = (mapCols - GRID_SIZE) / 2;
//
//        int realRow = r + offsetRow;
//        int realCol = c + offsetCol;
//
//        // --- مدیریت ساخت‌وساز (بدون تغییر) ---
//        if (buildingTypeToPlace != null) {
//            int buildX = realRow;
//            int buildY = realCol;
//            addSystem("Attempting to build " + buildingTypeToPlace + " at (" + buildX + ", " + buildY + ")...");
//            Result result = mediator.buildBuilding(buildingTypeToPlace, resourceTypeToPlace, buildX, buildY);
//            if (result.isSuccess()) {
//                addSystem("SUCCESS: " + result.getMessage());
//                buildingTypeToPlace = null;
//                resourceTypeToPlace = null;
//                renderTilesAndUpdate();
//                clearSelection();
//            } else {
//                addError("ERROR: " + result.getMessage());
//            }
//            return;
//        }
//
//        // --- مدیریت جابه‌جایی (اینجا هنوز از مختصات قبلی استفاده می‌کنیم چون buildingToMove ست شده) ---
//        if (buildingToMove != null) {
//            int newX = realRow;
//            int newY = realCol;
//            Result result = mediator.moveBuilding(oldRow, oldCol, newX, newY);
//            if (result.isSuccess()) {
//                addSystem("SUCCESS: " + result.getMessage());
//                buildingToMove = null;
//                oldRow = -1; oldCol = -1;
//                renderTilesAndUpdate();
//                clearSelection();
//            } else {
//                addError("ERROR: " + result.getMessage());
//            }
//            return;
//        }
//
//        // --- انتخاب عادی ---
//        if (lastSelectedRow == r && lastSelectedCol == c) {
//            clearSelection();
//            return;
//        }
//
//        if (lastSelectedRow != -1 && lastSelectedCol != -1) {
//            ImageButton prevBtn = tileButtons[lastSelectedRow][lastSelectedCol];
//            if (prevBtn != null) prevBtn.getImage().setColor(Color.WHITE);
//            if (buildingInfoBubble != null) {
//                buildingInfoBubble.remove();
//                buildingInfoBubble = null;
//            }
//        }
//
///
//        ImageButton currentBtn = tileButtons[r][c];
////        if (currentBtn != null) currentBtn.getImage().setColor(0.7f, 0.7f, 0.7f, 1f);
//
//        ImageButton currentBtn = tileButtons[r][c];
//        if (currentBtn != null) {
//            // حذف انیمیشن‌های قبلی (برای اطمینان)
//            currentBtn.getImage().clearActions();
//
//            // تنظیم رنگ اولیه
//            currentBtn.getImage().setColor(Color.CYAN); // رنگ آبی فیروزه‌ای برای تمایز
//
//            // اضافه کردن انیمیشن "تپش" (Breathe Effect)
//            currentBtn.getImage().addAction(Actions.forever(
//                Actions.sequence(
//                    Actions.color(new Color(0.5f, 1f, 1f, 1f), 0.8f), // روشن شدن
//                    Actions.color(new Color(0.2f, 0.7f, 0.7f, 1f), 0.8f)  // تیره شدن
//                )
//            ));
//        }
//
//        lastSelectedRow = r;
//        lastSelectedCol = c;
//        setStatus(String.format("Selected tile: [%d, %d]", realRow, realCol));
//
//        // *** تغییر حیاتی: پیدا کردن و ذخیره ساختمان ***
//        // تلاش اول: (Row, Col)
//        Building clickedBuilding = mediator.getBuildingAt(realRow, realCol);
//        // تلاش دوم: اگر پیدا نشد، (Col, Row) را چک کن (برای اطمینان)
//        if (clickedBuilding == null) {
//            clickedBuilding = mediator.getBuildingAt(realCol, realRow);
//        }
//
//        // ذخیره در متغیر کلاس برای استفاده در دکمه‌ها
//        this.currentSelectedBuilding = clickedBuilding;
//
//        if (clickedBuilding != null) {
//            if (currentBtn != null) {
//                Vector2 coords = currentBtn.localToStageCoordinates(new Vector2(0, 0));
//                showBuildingInfoBubble(clickedBuilding, coords.x, coords.y);
//            }
//            // دیباگ برای اطمینان
//            System.out.println("Selected Building: " + clickedBuilding.getName() + " at " + realRow + "," + realCol);
//        } else {
//            if (buildingInfoBubble != null) {
//                buildingInfoBubble.remove();
//                buildingInfoBubble = null;
//            }
//        }
//    }

    private void toggleTileSelection(int r, int c) {
    // ۱. دسترسی به داده‌های نقشه
    GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
    if (userInGame == null || userInGame.getKingdom() == null) return;
    UserSubMap map = userInGame.getKingdom().getUserMap();

    int mapRows = map.getLength();
    int mapCols = map.getWidth();

    // محاسبه حاشیه (چون نقشه ۱۳تایی است و گرید ۱۱تایی، آفست می‌شود ۱)
    int offsetRow = (mapRows - GRID_SIZE) / 2;
    int offsetCol = (mapCols - GRID_SIZE) / 2;

    // ۲. *** اصلاح نهایی و قطعی محاسبات ***
    // r مقدار ۰ تا ۱۰ است. offset مقدار ۱ است.
    // localX باید ۱ تا ۱۱ باشد. پس جمع این دو کافیست.
    // (قبلاً startX را کم می‌کردیم که غلط بود و عدد منفی می‌داد)
    int localX = r + offsetRow;
    int localY = c + offsetCol;

    // --- بخش هایلایت گرافیکی ---
    if (lastSelectedRow != -1 && lastSelectedCol != -1) {
        ImageButton prevBtn = tileButtons[lastSelectedRow][lastSelectedCol];
        if (prevBtn != null) {
            prevBtn.getImage().clearActions();
            prevBtn.getImage().setColor(Color.WHITE);
        }
        if (buildingInfoBubble != null) {
            buildingInfoBubble.remove();
            buildingInfoBubble = null;
        }
    }

    ImageButton currentBtn = tileButtons[r][c];
    if (currentBtn != null) {
        currentBtn.getImage().clearActions();
        currentBtn.getImage().setColor(Color.CYAN);
        currentBtn.getImage().addAction(Actions.forever(
            Actions.sequence(
                Actions.color(new Color(0.5f, 1f, 1f, 1f), 0.8f),
                Actions.color(new Color(0.2f, 0.7f, 0.7f, 1f), 0.8f)
            )
        ));
    }

    lastSelectedRow = r;
    lastSelectedCol = c;

    // نمایش مختصات برای دیباگ (این همان چیزی است که به سرور می‌رود)
    setStatus(String.format("Selected Local Tile: [%d, %d]", localX, localY));

    // ۳. --- منطق عملیات ---

    // الف) ساخت‌وساز
    if (buildingTypeToPlace != null) {
        final int xToBuild = localX;
        final int yToBuild = localY;

        Dialog confirmBuild = new Dialog("Confirm Construction", skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    // ارسال مختصات صحیح (۱ تا ۱۱)
                    Result result = mediator.buildBuilding(buildingTypeToPlace, resourceTypeToPlace, xToBuild, yToBuild);

                    if (result.isSuccess()) {
                        addSystem("SUCCESS: " + result.getMessage());
                        game.getSoundManager().playSound("build_success", 1.0f);
                        buildingTypeToPlace = null;
                        resourceTypeToPlace = null;
                        renderTilesAndUpdate();
                        clearSelection();
                    } else {
                        addError(result.getMessage());
                        game.getSoundManager().playSound("error", 1.0f);
                    }
                }
            }
        };
        confirmBuild.text("Build " + buildingTypeToPlace + " here?");
        confirmBuild.button("Yes", true);
        confirmBuild.button("No", false);
        confirmBuild.show(stage);
        return;
    }

    // ب) جابه‌جایی (Move)
    if (buildingToMove != null) {
        int targetX = localX;
        int targetY = localY;

        // چک کردن کلیک تکراری (مقصد == مبدا)
        if (targetX == oldRow && targetY == oldCol) {
            addNotif("Move cancelled (Same location).");
            buildingToMove = null;
            renderTilesAndUpdate();
            return;
        }

        Result result = mediator.moveBuilding(oldRow, oldCol, targetX, targetY);

        if (result.isSuccess()) {
            addSystem("SUCCESS: " + result.getMessage());
            game.getSoundManager().playSound("build_success", 1.0f);
            if (buildingToMove != null) {
                // آپدیت کردن حافظه کلاینت
                Position newGlobalPos = map.localToGlobal(new Position(targetX, targetY));
                buildingToMove.setPosition(newGlobalPos.getX(), newGlobalPos.getY());
            }
        } else {
            addError("ERROR: " + result.getMessage());
            game.getSoundManager().playSound("error", 1.0f);
        }

        buildingToMove = null;
        oldRow = -1; oldCol = -1;
        this.currentSelectedBuilding = null;
        renderTilesAndUpdate();
        clearSelection();
        return;
    }

    // ج) انتخاب ساختمان (Select)
    // اینجا هم باید از localX و localY استفاده کنیم چون getBuildingAt در Mediator یا UserSubMap مختصات نسبی می‌خواهد
        Building clickedBuilding = mediator.getBuildingAt(localX, localY);

    // اگر با (x,y) پیدا نشد، (y,x) را چک کن (برای باگ‌های احتمالی جابجایی سطر/ستون)
        if (clickedBuilding == null) {
            clickedBuilding = mediator.getBuildingAt(localY, localX);
        }

        this.currentSelectedBuilding = clickedBuilding;

        if (clickedBuilding != null) {
           if (currentBtn != null) {
               Vector2 coords = currentBtn.localToStageCoordinates(new Vector2(0, 0));
               showBuildingInfoBubble(clickedBuilding, coords.x, coords.y);
           }
        }
    }

    public Point getSelectedTile() {
        if (lastSelectedRow == -1 || lastSelectedCol == -1) return null;

        // محاسبه مجدد آفست
        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
        if (userInGame == null) return null;

        int mapRows = userInGame.getKingdom().getUserMap().getLength();
        int mapCols = userInGame.getKingdom().getUserMap().getWidth();
        int offsetRow = (mapRows - GRID_SIZE) / 2;
        int offsetCol = (mapCols - GRID_SIZE) / 2;

        // برگرداندن مختصات واقعی (Real Map Coordinates)
        // اصلاح: حذف +1. چون آفست خودش عدد 1 را دارد (معمولا حاشیه نقشه 1 است)
        int realRow = lastSelectedRow + offsetRow;
        int realCol = lastSelectedCol + offsetCol;

        return new Point(realRow, realCol);
    }

    private void openNewBuildingPanel() {
        Table content = new Table(skin);
        final Table listTable = new Table(skin);

        populateBuildingList(listTable);

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        content.add(scrollPane).width(800).height(600);

        openWindow("Construct New Building", content);
    }

    private void openWindow(String title, Actor content) {
        if (activeDialog != null && activeDialog.getStage() != null) {
            activeDialog.remove();
        }

        Window window = new Window(title, skin);
        window.setModal(true);
        window.setMovable(true);
        window.padTop(40);
        window.add(content).pad(10).row();

        TextButton closeButton = new TextButton("X", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                window.remove();
                activeDialog = null;
            }
        });
        window.getTitleTable().add(closeButton).size(30, 30).padRight(5);

        window.pack();
        window.setPosition(
            (stage.getWidth() - window.getWidth()) / 2,
            (stage.getHeight() - window.getHeight()) / 2
        );
        stage.addActor(window);
        activeDialog = window;
    }
    private void openMessagesPanel() {
        final Dialog panel = new Dialog("Mailbox", skin, "default");
        panel.setModal(true);
        panel.setSize(Gdx.graphics.getWidth() * 0.9f, Gdx.graphics.getHeight() * 0.85f); // افزایش اندازه
        panel.pad(20);

        final TextButton announcementsTabBtn = new TextButton("Announcements", skin, "toggle");
        final TextButton inboxTabBtn = new TextButton("Inbox", skin, "toggle");
        final TextButton notificationsTabBtn = new TextButton("Notifications", skin, "toggle");
        final ButtonGroup<TextButton> tabGroup = new ButtonGroup<>(announcementsTabBtn, inboxTabBtn, notificationsTabBtn);
        tabGroup.setMaxCheckCount(1);
        tabGroup.setMinCheckCount(1);

        Table tabBar = new Table(skin);
        tabBar.add(announcementsTabBtn).pad(5).width(200);
        tabBar.add(inboxTabBtn).pad(5).width(200);
        tabBar.add(notificationsTabBtn).pad(5).width(200);

        final Table tabContentContainer = new Table(skin);
        tabContentContainer.top().left();

        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tabContentContainer.clear();
                if (announcementsTabBtn.isChecked()) {
                    buildAnnouncementsTab(tabContentContainer);
                } else if (inboxTabBtn.isChecked()) {
                    buildInboxTab(tabContentContainer);
                } else if (notificationsTabBtn.isChecked()) {
                    buildNotificationsTab(tabContentContainer);
                }
            }
        };
        announcementsTabBtn.addListener(tabListener);
        inboxTabBtn.addListener(tabListener);
        notificationsTabBtn.addListener(tabListener);

        TextButton composeButton = new TextButton("Compose New Message", skin);
        composeButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                openComposeMessagePanel();
            }
        });

        panel.getContentTable().add(tabBar).expandX().fillX().padBottom(10).row();
        panel.getContentTable().add(tabContentContainer).expand().fill().row();
        panel.getButtonTable().add(composeButton).left().padRight(20);
        panel.button("Close", true);

        inboxTabBtn.setChecked(true); // حالت پیش‌فرض
        buildInboxTab(tabContentContainer);

        panel.show(stage);
    }

    private void openMessageDetailDialog(GameMediator.NoticeInfo notice) {
        final Dialog detailPanel = new Dialog(notice.title, skin, "default");
        detailPanel.setModal(true);
        detailPanel.pad(20);

        Table headerTable = new Table(skin);
        headerTable.defaults().left().pad(2);
        headerTable.add(new Label("From:", skin)).padRight(10);
        headerTable.add(new Label(notice.senderName, skin)).row();
        headerTable.add(new Label("To:", skin)).padRight(10);
        headerTable.add(new Label(notice.receiver != null ? notice.receiver.getUsername() : "All Kingdoms", skin)).row();
        headerTable.add(new Label("Date:", skin)).padRight(10);
        headerTable.add(new Label(notice.timestamp, skin)).row();

        Label bodyLabel = new Label(notice.body, skin);
        bodyLabel.setWrap(true);
        bodyLabel.setAlignment(Align.topLeft);

        ScrollPane bodyScroll = new ScrollPane(bodyLabel, skin);
        bodyScroll.setFadeScrollBars(false);

        detailPanel.getContentTable().add(headerTable).expandX().fillX().padBottom(10).row();
        detailPanel.getContentTable().add(new Image(skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 1f)))).height(1).fillX().padBottom(10).row();
        detailPanel.getContentTable().add(bodyScroll).expand().fill().width(600).height(400);

        detailPanel.button("Close", true);
        detailPanel.show(stage);
    }
    /**
     * محتوای تب اطلاعیه‌های ادمین را می‌سازد.
     * این یک متد جدید است.
     * @param container کانتینری که محتوا در آن قرار می‌گیرد.
     */
    private void buildAnnouncementsTab(Table container) {
        final Table listTable = new Table(skin);
        listTable.top().left();

        java.util.List<GameMediator.NoticeInfo> announcements = new ArrayList<>();
        for (GameMediator.NoticeInfo notice : mediator.getNoticesForCurrentUser()) {
            if (notice.isSystemNotice && "ADMIN".equals(notice.senderName)) {
                announcements.add(notice);
            }
        }
        announcements.sort(Comparator.comparing(n -> n.timestamp));

        if (announcements.isEmpty()) {
            listTable.add(new Label("No announcements.", skin)).pad(10);
        } else {
            for (final GameMediator.NoticeInfo notice : announcements) {
                TextButton itemButton = new TextButton(notice.timestamp + ": " + notice.title, skin, "default");
                itemButton.getLabel().setAlignment(Align.left);
                itemButton.addListener(new ClickListener() {
                    public void clicked(InputEvent e, float x, float y) { openMessageDetailDialog(notice); }
                });
                listTable.add(itemButton).expandX().fillX().pad(4).row();
            }
        }
        container.add(new ScrollPane(listTable, skin)).expand().fill();
    }

    /**
     * محتوای تب صندوق ورودی (پیام‌های بازیکنان) را می‌سازد.
     * این یک متد جدید است.
     * @param container کانتینری که محتوا در آن قرار می‌گیرد.
     */
// In file: GamePlayScreen.java

    private void buildInboxTab(Table container) {
        final Table listTable = new Table(skin);
        listTable.top().left();

        java.util.List<GameMediator.NoticeInfo> inboxMessages = new ArrayList<>();
        for (final GameMediator.NoticeInfo notice : mediator.getNoticesForCurrentUser()) {
            if (!notice.isSystemNotice) inboxMessages.add(notice);
        }

        if (inboxMessages.isEmpty()) {
            listTable.add(new Label("Your inbox is empty.", skin)).pad(10);
        } else {
            for (final GameMediator.NoticeInfo notice : inboxMessages) {
                String sender = notice.senderName + (notice.receiver == null ? " (Broadcast)" : "");
                TextButton itemButton = new TextButton(sender + ": " + notice.title, skin, "default");
                itemButton.getLabel().setAlignment(Align.left);

                // --- START OF FIX ---
                // ابتدا چک می‌کنیم که استایل bold وجود دارد، سپس از آن استفاده می‌کنیم
                if (!notice.isRead) {
                    if (skin.has("default-bold", Label.LabelStyle.class)) {
                        itemButton.getLabel().setStyle(skin.get("default-bold", Label.LabelStyle.class));
                    }
                }
                // --- END OF FIX ---

                itemButton.addListener(new ClickListener() {
                    public void clicked(InputEvent e, float x, float y) {
                        if (!notice.isRead) {
                            mediator.markNoticeAsRead(notice.id);
                            itemButton.getLabel().setStyle(skin.get("default", Label.LabelStyle.class));
                        }
                        openMessageDetailDialog(notice);
                    }
                });
                listTable.add(itemButton).expandX().fillX().pad(4).row();
            }
        }
        container.add(new ScrollPane(listTable, skin)).expand().fill();
    }
    // In file: GamePlayScreen.java

// In file: GamePlayScreen.java
// In file: GamePlayScreen.java

    private void buildNotificationsTab(Table container) {
        final Table listTable = new Table(skin);
        listTable.top().left();

        java.util.List<GameMediator.NoticeInfo> notifications = new ArrayList<>();
        for (GameMediator.NoticeInfo notice : mediator.getNoticesForCurrentUser()) {
            if (notice.isSystemNotice && !"ADMIN".equals(notice.senderName)) {
                notifications.add(notice);
            }
        }

        if (notifications.isEmpty()) {
            listTable.add(new Label("No new system notifications.", skin)).pad(10);
        } else {
            for (final GameMediator.NoticeInfo notice : notifications) {
                TextButton itemButton = new TextButton(notice.title, skin, "default");
                itemButton.getLabel().setAlignment(Align.left);

                // --- START OF FIX ---
                // ابتدا چک می‌کنیم که استایل bold وجود دارد، سپس از آن استفاده می‌کنیم
                if (!notice.isRead) {
                    if (skin.has("default-bold", Label.LabelStyle.class)) {
                        itemButton.getLabel().setStyle(skin.get("default-bold", Label.LabelStyle.class));
                    }
                }
                // --- END OF FIX ---

                itemButton.addListener(new ClickListener() {
                    public void clicked(InputEvent e, float x, float y) {
                        if (!notice.isRead) {
                            mediator.markNoticeAsRead(notice.id);
                            itemButton.getLabel().setStyle(skin.get("default", Label.LabelStyle.class));
                        }
                        openMessageDetailDialog(notice);
                    }
                });
                listTable.add(itemButton).expandX().fillX().pad(4).row();
            }
        }
        container.add(new ScrollPane(listTable, skin)).expand().fill();
    }
    @Override
    public void onMessage(final String message) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                String cleanMessage = message;
                // حذف پیشوندهای اضافی برای نمایش بهتر در پاپ‌آپ
                if (message.contains("::")) {
                    cleanMessage = message.substring(message.indexOf("::") + 2).trim();
                }

                // ✅ این خط فراخوانی پاپ‌آپ است که فراموش شده بود
                showToastNotification(cleanMessage);

                // این بخش هم برای لاگ اصلی مثل قبل باقی می‌ماند
                if (message.toUpperCase().startsWith("ERROR:") || message.toUpperCase().startsWith("WARNING:")) {
                    addError(message);
                } else if (message.toUpperCase().startsWith("SUCCESS:") || message.toUpperCase().startsWith("ACTION:")) {
                    addSystem(message);
                } else {
                    addNotif(message);
                }
            }
        });
    }
    private void showToastNotification(String message) {
        // 1. ساخت ظاهر پنجره اعلان
        game.getSoundManager().playSound("notification", 0.4f); // ولوم ۴۰٪

        Table toastTable = new Table(skin);
        // *** تغییر ۱: تغییر رنگ پس‌زمینه به قرمز نیمه‌شفاف ***
        toastTable.setBackground(skin.newDrawable("white", new Color(0.8f, 0.1f, 0.1f, 0.9f)));
        toastTable.pad(25); // افزایش padding برای ظاهر بهتر

        Label messageLabel = new Label(message, skin);
        messageLabel.setWrap(true);
        messageLabel.setAlignment(Align.center); // متن در مرکز قرار می‌گیرد
        // *** تغییر ۲: افزایش عرض پنجره برای بزرگ‌تر شدن آن ***
        toastTable.add(messageLabel).width(500);
        toastTable.pack(); // محاسبه اندازه بهینه

// *** تغییر: تعیین موقعیت در بالا-مرکز صفحه ***
        float padding = 20f; // کمی فاصله از لبه بالای صفحه
        toastTable.setPosition(
            (stage.getWidth() - toastTable.getWidth()) / 2f, // همچنان در مرکز افقی
            stage.getHeight() - toastTable.getHeight() - padding // در بالای صفحه با کمی فاصله
        );

        // 3. تعریف انیمیشن (بدون تغییر)
        toastTable.getColor().a = 0f;
        toastTable.addAction(Actions.sequence(
            Actions.fadeIn(0.4f),
            Actions.delay(3.0f),
            Actions.fadeOut(0.4f),
            Actions.removeActor()
        ));

        // 4. اضافه کردن به صحنه (بدون تغییر)
        stage.addActor(toastTable);
    }
    private void displayNoticeContent(Table container, GameMediator.NoticeInfo notice) {
        container.clear();
        container.defaults().pad(5).left();

        Table headerTable = new Table(skin);
        headerTable.defaults().left();
        headerTable.add(new Label("From:", skin)).padRight(10);
        headerTable.add(new Label(notice.senderName, skin)).row();

        // نمایش گیرنده فقط اگر پیام شخصی باشد
        if (notice.receiver != null) {
            headerTable.add(new Label("To:", skin)).padRight(10);
            headerTable.add(new Label(notice.receiver.getUsername(), skin)).row();
        } else {
            headerTable.add(new Label("To:", skin)).padRight(10);
            headerTable.add(new Label("All Kingdoms", skin)).row();
        }

        headerTable.add(new Label("Title:", skin)).padRight(10);
        headerTable.add(new Label(notice.title, skin)).row();
        headerTable.add(new Label("Date:", skin)).padRight(10);
        headerTable.add(new Label(notice.timestamp, skin)).row();
        container.add(headerTable).expandX().fillX().padBottom(10).row();

        Image separatorLine = new Image(skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 1f)));
        container.add(separatorLine).expandX().fillX().height(1).padBottom(10).row();

        Label bodyLabel = new Label(notice.body, skin);
        bodyLabel.setWrap(true);
        bodyLabel.setAlignment(Align.topLeft);

        ScrollPane bodyScroll = new ScrollPane(bodyLabel, skin);
        bodyScroll.setFadeScrollBars(false);

        container.add(bodyScroll).expand().fill().row();
    }

    // ❌ متد openComposeMessagePanel قدیمی حذف شده و با کد جدید زیر جایگزین می‌شود. ❌

    /**
     * پنل ارسال پیام جدید با قابلیت انتخاب گیرنده (شخصی یا همگانی).
     * این متد جایگزین نسخه قدیمی شده است.
     */
    private void openComposeMessagePanel() {
        final Dialog panel = new Dialog("Compose Message", skin, "default");
        panel.setModal(true);
        panel.pad(20);
        panel.setSize(600, 500);

        Table content = new Table(skin);
        content.defaults().left().pad(5);

        content.add(new Label("To:", skin)).right();
        final SelectBox<GameMediator.MessageRecipientInfo> recipientSelectBox = new SelectBox<GameMediator.MessageRecipientInfo>(skin) {
            @Override
            protected String toString(GameMediator.MessageRecipientInfo item) {
                return item.displayText; // نمایش متن سفارشی در لیست
            }
        };

        MyLinkedList<GameMediator.MessageRecipientInfo> recipients = mediator.getAvailableRecipients();
        java.util.List<GameMediator.MessageRecipientInfo> recipientList = new java.util.ArrayList<>();

        // گزینه ارسال همگانی با ID خاص -1
        recipientList.add(new GameMediator.MessageRecipientInfo(-1, "Broadcast to All Kingdoms"));

        for (GameMediator.MessageRecipientInfo recipient : recipients) {
            recipientList.add(recipient);
        }

        com.badlogic.gdx.utils.Array<GameMediator.MessageRecipientInfo> gdxArray = new com.badlogic.gdx.utils.Array<>(recipientList.toArray(new GameMediator.MessageRecipientInfo[0]));
        recipientSelectBox.setItems(gdxArray);

        content.add(recipientSelectBox).expandX().fillX().row();

        content.add(new Label("Title:", skin)).right();
        final TextField titleField = new TextField("", skin);
        content.add(titleField).expandX().fillX().row();

        final TextArea bodyArea = new TextArea("", skin);
        content.add(new Label("Body:", skin)).right().top().padTop(8);
        content.add(bodyArea).colspan(1).expand().fill().padTop(10).row();

        TextButton sendButton = new TextButton("Send", skin);
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameMediator.MessageRecipientInfo selected = recipientSelectBox.getSelected();
                String title = titleField.getText();
                String body = bodyArea.getText();

                Result result;
                if (selected.userId == -1) { // اگر ارسال همگانی انتخاب شده باشد
                    result = mediator.sendBroadcastMessage(title, body);
                } else {
                    result = mediator.sendMessage(selected.userId, title, body);
                }

                if (result.isSuccess()) {
                    addSystem(result.getMessage());
                    panel.hide();
                } else {
                    addError(result.getMessage());
                }
            }
        });

        panel.getContentTable().add(content).expand().fill();
        panel.getButtonTable().add(sendButton);
        panel.button("Cancel", false);
        panel.show(stage);
    }
    private Actor createPlaceholder(String name , int level) {
        Image placeholder = new Image(GameFileUtility.getBuildingTexture(name,level-1));
        return placeholder;
    }

    private void openOverhaulPanel() {
        Point selectedCoords = getSelectedTile();
        if (selectedCoords == null) {
            addError("ERROR: Please select a building on the map to destroy first.");
            return;
        }
        final int col = selectedCoords.y;
        final int row = selectedCoords.x;

        Building targetBuilding = mediator.getBuildingAt(row, col);
        if (targetBuilding == null) {
            addNotif("INFO: The selected tile is empty. Nothing to destroy.");
            return;
        }
        Table root = new Table(skin); root.defaults().pad(8);

        TextButton upgradeBtn = new TextButton("Upgrade", skin);
        TextButton repairBtn = new TextButton("Repair", skin);

        upgradeBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { openUpgradePanel(); } });
        repairBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { openRepairPanel(); } });

        root.add(upgradeBtn).width(220).row();
        root.add(repairBtn).width(220).row();

        openWindow("Overhaul", root);
    }


//    private void openUpgradePanel() {
//        Point selectedCoords = getSelectedTile();
//        if (selectedCoords == null) {
//            addError("ERROR: Please select a building on the map to upgrade.");
//            return;
//        }
//
//        final int row = selectedCoords.x;
//        final int col = selectedCoords.y;
//
//        MyLinkedList<BuildingManager.UpgradeOption> allOptions = mediator.getUpgradeOptions();
//
//        // استفاده از متد هوشمند برای پیدا کردن گزینه ارتقا
//        BuildingManager.UpgradeOption upgradeData = findUpgradeOptionSafely(row, col, allOptions);
//
//        if (upgradeData == null) {
//            // اگر باز هم پیدا نشد، یعنی واقعاً قابل ارتقا نیست یا ساختمان خاصی (مثل دیوار) است
//            addNotif("INFO: The selected building cannot be upgraded or is fully upgraded.");
//            return;
//        }
//
//        Table content = new Table(skin);
//        content.defaults().pad(8);
//        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
//
//        String infoText = String.format(
//            "Building: %s\nLevel: %d -> %d\nUpgrade Time: %d turns\n\nCost to Upgrade:\n%s",
//            upgradeData.name, upgradeData.currentLevel, upgradeData.nextLevel, upgradeData.time, upgradeData.costString
//        );
//        Label infoLabel = new Label(infoText, skin);
//        infoLabel.setAlignment(Align.left);
//
//        TextButton confirmButton = new TextButton("Confirm Upgrade", skin);
//        confirmButton.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                // تلاش برای ارسال درخواست با مختصات اصلی
//                // اگر Mediator هوشمند باشد خودش هندل می‌کند، اما اینجا ما همان مختصاتی که کار کرد را می‌فرستیم
//                Result result = mediator.upgradeBuildingAt(row, col);
//
//                // اگر با (row, col) ناموفق بود، شانس دوم با (col, row) امتحان می‌کنیم
//                if (!result.isSuccess() && result.getMessage().contains("No building")) {
//                    result = mediator.upgradeBuildingAt(col, row);
//                }
//
//                if (result.isSuccess()) {
//                    addSystem("SUCCESS: " + result.getMessage());
//                    if (activeDialog != null) {
//                        activeDialog.remove();
//                        activeDialog = null;
//                    }
//                    renderTilesAndUpdate();
//                    clearSelection();
//                } else {
//                    addError(result.getMessage());
//                }
//            }
//        });
//
//        if (!upgradeData.isActionable) {
//            confirmButton.setDisabled(true);
//            confirmButton.getLabel().setColor(Color.GRAY);
//            addError(upgradeData.statusMessage);
//        }
//
//        String baseBuildingName = upgradeData.name;
//        if (baseBuildingName.contains("(")) {
//            baseBuildingName = baseBuildingName.substring(0, baseBuildingName.indexOf('(')).trim();
//        }
//
//        Image buildingImage = new Image(getCachedBuildingDrawable(baseBuildingName, upgradeData.nextLevel - 1));
//
//        Table textTable = new Table(skin);
//        textTable.add(infoLabel).left();
//        content.add(buildingImage).size(120).pad(10);
//        content.add(textTable).expand().fill().pad(5).left().row();
//        content.add(confirmButton).colspan(2).padTop(15).width(300).row();
//
//        openWindow("Upgrade Building", content);
//    }

//    private void openRepairPanel() {
//        Point selectedCoords = getSelectedTile();
//        if (selectedCoords == null) {
//            addError("ERROR: Please select a building on the map to repair.");
//            return;
//        }
//        final int col = selectedCoords.y;
//        final int row = selectedCoords.x;
//
//        MyLinkedList<BuildingManager.RepairOption> allOptions = mediator.getRepairOptions();
//
//        String targetCoords = String.format("(%d,%d)", row, col);
//
//        BuildingManager.RepairOption repairData = null;
//        for (BuildingManager.RepairOption option : allOptions) {
//            if (option.coords.equals(targetCoords)) {
//                repairData = option;
//                break;
//            }
//        }
//
//        if (repairData == null) {
//            addNotif("INFO: The selected building cannot be repaired.");
//            return;
//        }
//
//        Table content = new Table(skin);
//        content.defaults().pad(8);
//        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
//
//        String infoText = String.format(
//            "Building: %s \nLevel %d\nRepair Time: %d turns\n\nCost to Repair:\n%s",
//            repairData.name, repairData.level, repairData.time, repairData.costString
//        );
//        Label infoLabel = new Label(infoText, skin);
//        infoLabel.setAlignment(Align.left);
//
//        TextButton confirmButton = new TextButton("Confirm Repair", skin);
//        confirmButton.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                Result result = mediator.repairBuildingAt(row, col);
//                if (result.isSuccess()) {
//                    addSystem("SUCCESS: " + result.getMessage());
//                    if (activeDialog != null) {
//                        activeDialog.remove();
//                        activeDialog = null;
//                    }
//                    renderTilesAndUpdate();
//                    clearSelection();
//                } else {
//                    addError(result.getMessage());
//                    confirmButton.setDisabled(true);
//                    confirmButton.getLabel().setColor(Color.GRAY);
//                }
//            }
//        });
//
//        if (!repairData.isActionable) {
//            confirmButton.setDisabled(true);
//            confirmButton.getLabel().setColor(Color.GRAY);
//            addError(repairData.statusMessage);
//        }
//
//        String baseBuildingName = repairData.name;
//        if (baseBuildingName.contains("(")) {
//            baseBuildingName = baseBuildingName.substring(0, baseBuildingName.indexOf('(')).trim();
//        }
//        Image buildingImage = new Image(GameFileUtility.getBuildingTexture(baseBuildingName, repairData.level - 1));
//        buildingImage.setColor(0.5f, 0.5f, 0.5f, 0.8f);
//        Table textTable = new Table(skin);
//        textTable.add(infoLabel).left();
//        content.add(buildingImage).size(120).pad(10);
//        content.add(textTable).expand().fill().pad(5).left().row();
//        content.add(confirmButton).colspan(2).padTop(15).width(300).row();
//        openWindow("Repair Building", content);
//    }

    private void openUpgradePanel() {
        if (this.currentSelectedBuilding == null) {
            addError("ERROR: Please select a building on the map first.");
            return;
        }

        Building targetBuilding = this.currentSelectedBuilding;

        // دریافت مختصات دقیق از خود ساختمان برای پیدا کردن گزینه ارتقا
        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
        Position localPos = userInGame.getKingdom().getUserMap().globalToLocal(targetBuilding.getPosition());

        if (localPos == null) {
            addError("Error: Building position invalid.");
            return;
        }

        final int row = localPos.getX();
        final int col = localPos.getY();

        MyLinkedList<BuildingManager.UpgradeOption> allOptions = mediator.getUpgradeOptions();

        // بازسازی رشته مختصات دقیقاً مثل BuildingManager
        String targetCoords = "(" + row + "," + col + ")";

        BuildingManager.UpgradeOption upgradeData = null;
        for (BuildingManager.UpgradeOption option : allOptions) {
            if (option.coords.equals(targetCoords)) {
                upgradeData = option;
                break;
            }
        }

        if (upgradeData == null) {
            addNotif("INFO: This building cannot be upgraded (Max level or restricted).");
            return;
        }

        // ... (بقیه کدهای نمایش پنجره بدون تغییر) ...
        Table content = new Table(skin);
        content.defaults().pad(8);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));

        String infoText = String.format(
            "Building: %s\nLevel: %d -> %d\nUpgrade Time: %d turns\n\nCost to Upgrade:\n%s",
            upgradeData.name, upgradeData.currentLevel, upgradeData.nextLevel, upgradeData.time, upgradeData.costString
        );
        Label infoLabel = new Label(infoText, skin);
        infoLabel.setAlignment(Align.left);

        TextButton confirmButton = new TextButton("Confirm Upgrade", skin);
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // ارسال مختصات دقیق
                Result result = mediator.upgradeBuildingAt(row, col);

                if (result.isSuccess()) {
                    addSystem("SUCCESS: " + result.getMessage());
                    if (activeDialog != null) {
                        activeDialog.remove();
                        activeDialog = null;
                    }
                    renderTilesAndUpdate();
                    clearSelection();
                } else {
                    addError(result.getMessage());
                }
            }
        });

        // ... (ادامه کدها: افزودن دکمه و عکس به پنجره) ...
        if (!upgradeData.isActionable) {
            confirmButton.setDisabled(true);
            confirmButton.getLabel().setColor(Color.GRAY);
            addError(upgradeData.statusMessage);
        }
        String baseBuildingName = upgradeData.name;
        if (baseBuildingName.contains("(")) {
            baseBuildingName = baseBuildingName.substring(0, baseBuildingName.indexOf('(')).trim();
        }
        Image buildingImage = new Image(getCachedBuildingDrawable(baseBuildingName, upgradeData.nextLevel - 1));

        Table textTable = new Table(skin);
        textTable.add(infoLabel).left();
        content.add(buildingImage).size(120).pad(10);
        content.add(textTable).expand().fill().pad(5).left().row();
        content.add(confirmButton).colspan(2).padTop(15).width(300).row();

        openWindow("Upgrade Building", content);
    }

    private void openRepairPanel() {
        Point selectedCoords = getSelectedTile();
        if (selectedCoords == null) {
            addError("ERROR: Please select a building on the map to repair.");
            return;
        }
        final int row = selectedCoords.x;
        final int col = selectedCoords.y;

        MyLinkedList<BuildingManager.RepairOption> allOptions = mediator.getRepairOptions();

        // استفاده از متد هوشمند
        BuildingManager.RepairOption repairData = findRepairOptionSafely(row, col, allOptions);

        if (repairData == null) {
            addNotif("INFO: The selected building cannot be repaired or needs no repair.");
            return;
        }

        Table content = new Table(skin);
        content.defaults().pad(8);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));

        String infoText = String.format(
            "Building: %s \nLevel %d\nRepair Time: %d turns\n\nCost to Repair:\n%s",
            repairData.name, repairData.level, repairData.time, repairData.costString
        );
        Label infoLabel = new Label(infoText, skin);
        infoLabel.setAlignment(Align.left);

        TextButton confirmButton = new TextButton("Confirm Repair", skin);
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // استراتژی شانس دوم برای تعمیر هم اعمال می‌شود
                Result result = mediator.repairBuildingAt(row, col);
                if (!result.isSuccess() && result.getMessage().contains("No building")) {
                    result = mediator.repairBuildingAt(col, row);
                }

                if (result.isSuccess()) {
                    addSystem("SUCCESS: " + result.getMessage());
                    if (activeDialog != null) {
                        activeDialog.remove();
                        activeDialog = null;
                    }
                    renderTilesAndUpdate();
                    clearSelection();
                } else {
                    addError(result.getMessage());
                }
            }
        });

        if (!repairData.isActionable) {
            confirmButton.setDisabled(true);
            confirmButton.getLabel().setColor(Color.GRAY);
            // نمایش پیام خطا فقط اگر دیالوگ باز شد
            // addError(repairData.statusMessage);
        }

        String baseBuildingName = repairData.name;
        if (baseBuildingName.contains("(")) {
            baseBuildingName = baseBuildingName.substring(0, baseBuildingName.indexOf('(')).trim();
        }
        Image buildingImage = new Image(getCachedBuildingDrawable(baseBuildingName, repairData.level - 1));
        buildingImage.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        Table textTable = new Table(skin);
        textTable.add(infoLabel).left();
        content.add(buildingImage).size(120).pad(10);
        content.add(textTable).expand().fill().pad(5).left().row();
        content.add(confirmButton).colspan(2).padTop(15).width(300).row();
        openWindow("Repair Building", content);
    }

    private void openDestroyPanel() {
        Point selectedCoords = getSelectedTile();
        if (selectedCoords == null) {
            addError("ERROR: Please select a building on the map to destroy first.");
            return;
        }

        final int col = selectedCoords.y;
        final int row = selectedCoords.x;

        Building targetBuilding = mediator.getBuildingAt(row, col);
        if (targetBuilding == null) {
            addNotif("INFO: The selected tile is empty. Nothing to destroy.");
            return;
        }

        Table content = new Table(skin);
        content.pad(15);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));


        Table infoTable = new Table(skin);
        infoTable.add(createPlaceholder(targetBuilding.getName(), targetBuilding.getLevel())).size(120).pad(10);

        Table textInfo = new Table(skin);
        textInfo.add(new Label("Building: " + targetBuilding.getName(), skin)).left().row();
        textInfo.add(new Label("Level: " + targetBuilding.getLevel(), skin)).left().row();
        textInfo.add(new Label("Health: " + targetBuilding.getHealth(), skin)).left().row();
        infoTable.add(textInfo).left();


        TextButton destroyButton = new TextButton("Destroy Building", skin);

        content.add(infoTable).padBottom(20).row();
        content.add(destroyButton).width(300).height(50);

        destroyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialog confirmationDialog = new Dialog("Confirm Destruction", skin) {
                    @Override
                    protected void result(Object object) {
                        if (Boolean.TRUE.equals(object)) {
                            Result result = mediator.destroyBuildingAt(row, col);

                            if (result.isSuccess()) {
                                addSystem("SUCCESS: " + result.getMessage());
                                renderTilesAndUpdate(); // Redraw the map
                                clearSelection();

                                if (activeDialog != null) {
                                    activeDialog.remove();
                                    activeDialog = null;
                                }
                            } else {
                                addError("ERROR: " + result.getMessage());
                            }
                        }
                    }
                };
                Label confirmLabel = new Label(
                    "Are you sure you want to destroy " + targetBuilding.getName() + "?",
                    skin
                );
                confirmLabel.setColor(Color.BLACK);
                confirmationDialog.getContentTable().add(confirmLabel).pad(10).row();

                confirmationDialog.button("Confirm", true);
                confirmationDialog.button("Cancel", false);
                confirmationDialog.show(stage);

            }
        });

        openWindow("Destroy Building", content);
    }

//    private void openMovePanel() {
//        Point selectedCoords = getSelectedTile();
//        if (selectedCoords == null) {
//            addError("ERROR: Please select a building to move first.");
//            return;
//        }
//
//        final int col = selectedCoords.y;
//        final int row = selectedCoords.x;
//
//        Building targetBuilding = findBuildingSafely(row, col);
//        if (targetBuilding == null) {
//            addNotif("INFO: The selected tile is empty. Nothing to move.");
//            return;
//        }
//
//
//        Dialog confirmDialog = new Dialog("Move Building", skin) {
//            @Override
//            protected void result(Object object) {
//                if (Boolean.TRUE.equals(object)) {
//                    buildingToMove = targetBuilding;
//                    oldRow = row;
//                    oldCol = col;
//
//                    addSystem("ACTION: Select the new tile to move " + targetBuilding.getName() + ".");
//                    if (activeDialog != null) {
//                        activeDialog.remove();
//                        activeDialog = null;
//                    }
//
//                    renderTilesAndUpdate();
//                }
//            }
//        };
//
//        Label lbl = new Label("Do you want to move " + targetBuilding.getName() + "?", skin);
//        lbl.setColor(Color.BLACK);
//        confirmDialog.getContentTable().add(lbl).pad(10).row();
//        confirmDialog.button("Confirm", true);
//        confirmDialog.button("Cancel", false);
//        confirmDialog.show(stage);
//    }

//    private void openMovePanel() {
//        // استفاده مستقیم از ساختمان ذخیره شده
//        if (this.currentSelectedBuilding == null) {
//            addNotif("INFO: No building selected. Please click on a building first.");
//            return;
//        }
//
//        // استفاده از مختصات خود ساختمان برای اطمینان
//        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
//        UserSubMap map = userInGame.getKingdom().getUserMap();
//        Position localPos = map.globalToLocal(currentSelectedBuilding.getPosition());
//
//        if (localPos == null) {
//            addError("Error: Could not determine building position.");
//            return;
//        }
//
//        final int row = localPos.getX();
//        final int col = localPos.getY();
//        Building targetBuilding = this.currentSelectedBuilding;
//
//        Dialog confirmDialog = new Dialog("Move Building", skin) {
//            @Override
//            protected void result(Object object) {
//                if (Boolean.TRUE.equals(object)) {
//                    buildingToMove = targetBuilding;
//                    oldRow = row;
//                    oldCol = col;
//
//                    addSystem("ACTION: Select the new tile to move " + targetBuilding.getName() + ".");
//                    if (activeDialog != null) {
//                        activeDialog.remove();
//                        activeDialog = null;
//                    }
//                    renderTilesAndUpdate();
//                }
//            }
//        };
//
//        Label lbl = new Label("Do you want to move " + targetBuilding.getName() + "?", skin);
//        lbl.setColor(Color.BLACK);
//        confirmDialog.getContentTable().add(lbl).pad(10).row();
//        confirmDialog.button("Confirm", true);
//        confirmDialog.button("Cancel", false);
//        confirmDialog.show(stage);
//    }

    private void openMovePanel() {
        if (this.currentSelectedBuilding == null) {
            addNotif("INFO: No building selected. Please click on a building first.");
            return;
        }

        Building targetBuilding = this.currentSelectedBuilding;
        GameUser userInGame = mediator.getCurrentGame().getUser(currentUser.getId());
        UserSubMap map = userInGame.getKingdom().getUserMap();

        // دریافت مختصات دقیق از روی نقشه (نه فقط از خود ساختمان)
        // این کار تضمین می‌کند که اگر ساختمان جابجا شده باشد، مختصات جدید را بگیریم
        Position localPos = map.globalToLocal(targetBuilding.getPosition());

        if (localPos == null) {
            // اگر نال بود، یعنی مختصات ساختمان با نقشه سینک نیست.
            // تلاش برای پیدا کردن دستی ساختمان در نقشه (Fallback)
            // (این بخش اختیاری است اما برای رفع باگ عالی است)
            addError("Error: Building position desynchronized. Please re-select.");
            this.currentSelectedBuilding = null;
            clearSelection();
            return;
        }

        buildingToMove = targetBuilding;
        oldRow = localPos.getX();
        oldCol = localPos.getY();

        addSystem("ACTION: Select a NEW tile to move " + targetBuilding.getName() + ".");

        if (activeDialog != null) { activeDialog.remove(); activeDialog = null; }
        if (buildingInfoBubble != null) { buildingInfoBubble.remove(); buildingInfoBubble = null; }

        renderTilesAndUpdate();
    }
    private void openHistoryMainMenu() {
        Table content = new Table(skin);
        content.pad(20).defaults().width(300).height(50).space(15);

        // دکمه برای نمایش ماموریت‌های در حال انجام
        TextButton missionsBtn = new TextButton("وضعیت حملات (Ongoing Missions)", skin);
        missionsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // منوی فعلی به طور خودکار بسته و پنل ماموریت‌ها باز می‌شود
                openMissionsPanel();
            }
        });

        // دکمه برای نمایش تاریخچه نبردهای گذشته
        TextButton battleHistoryBtn = new TextButton("تاریخچه نبردها (Battle History)", skin);
        battleHistoryBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // منوی فعلی به طور خودکار بسته و پنل تاریخچه باز می‌شود
                openBattleHistoryPanel();
            }
        });

        content.add(missionsBtn).row();
        content.add(battleHistoryBtn).row();

        openWindow("History Menu", content);
    }
    private void openAttackPanel() {
        // ---- تعریف متغیرهای اصلی ----
        final AttackType[] selectedAttackType = { null };
        final GameUser[] selectedTarget = { null };
        final Map<SoldierType, Integer> selectedSoldieryCounts = new HashMap<>();
        final MyLinkedList<SoldierType> formationOrder = new MyLinkedList<>();

        // ---- ساخت پنجره اصلی ----
        final Window attackWindow = new Window("Launch Attack", skin);
        attackWindow.setModal(true);
        attackWindow.setMovable(true);
        attackWindow.padTop(40);

        // ---- ساخت کانتینرهای محتوا ----
        final Table wizardContent = new Table(skin); // محتوای اصلی
        final Table navigationBar = new Table(skin); // دکمه‌ها
        navigationBar.pad(8);

        attackWindow.add(wizardContent).expand().fill().pad(10).row();
        attackWindow.add(new Image(skin.newDrawable("white", new Color(1,1,1,0.08f)))).height(1).expandX().fillX().row();
        attackWindow.add(navigationBar).expandX().fill().pad(5);

        // ---- دکمه بستن ----
        final InputListener[] stageKeyListener = new InputListener[1]; // برای مدیریت Enter (قابل حذف/اضافه)
        TextButton closeButton = new TextButton("X", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (stageKeyListener[0] != null) stage.removeListener(stageKeyListener[0]);
                attackWindow.remove();
                activeDialog = null;
            }
        });
        attackWindow.getTitleTable().add(closeButton).size(30, 30).padRight(5);

        // ---- دکمه‌های ناوبری ----
        final TextButton backBtn = new TextButton("Back", skin);
        final TextButton nextBtn = new TextButton("Next", skin);
        final TextButton launchBtn = new TextButton("Launch Attack", skin);

        // ---- تابع کمکی برای تغییر اندازه و مرکزیت پنجره ----
        final Runnable resizeAndCenterWindow = () -> {
            attackWindow.pack();
            attackWindow.setPosition(
                (stage.getWidth() - attackWindow.getWidth()) / 2f,
                (stage.getHeight() - attackWindow.getHeight()) / 2f
            );
        };

        // ---- تعریف مراحل ----
        final Runnable[] steps = new Runnable[5]; // 0,1,2,3,4    (ما از 0,2,3,4 استفاده می‌کنیم تا کمترین تغییر را نسبت به ساختار قبلی داشته باشیم)

        // ===== مرحله 5: تایید نهایی (پس از چینش) =====
        steps[4] = () -> {
            wizardContent.clear();
            navigationBar.clear();

            wizardContent.add(new Label("Step 5: Confirm Attack", skin, "title")).left().padBottom(12).row();

            Table summary = new Table(skin);
            summary.left();
            summary.add(new Label("Target: " + (selectedTarget[0] != null ? selectedTarget[0].getUsername(): "N/A"), skin)).left().row();
            summary.add(new Label("Attack Type: " + (selectedAttackType[0] != null ? selectedAttackType[0].name() : "N/A"), skin)).left().row();
            summary.row();

            summary.add(new Label("Soldiers:", skin)).left().padTop(8).row();
            for (Map.Entry<SoldierType, Integer> e : selectedSoldieryCounts.entrySet()) {
                summary.add(new Label(e.getKey().name() + " x " + e.getValue(), skin)).left().row();
            }

            summary.add(new Label("Formation:", skin)).left().padTop(8).row();
            for (SoldierType s : formationOrder) {
                summary.add(new Label(s.name(), skin)).left().row();
            }

            wizardContent.add(summary).row();

            backBtn.clearListeners();
            backBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    steps[3].run(); // بازگشت به چینش
                }
            });

            launchBtn.setText("Launch Attack");
            launchBtn.clearListeners();
            launchBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Result result = mediator.launchAttack(selectedTarget[0], selectedAttackType[0], selectedSoldieryCounts, formationOrder);
                    if (result.isSuccess()) {
                        addSystem(result.getMessage());
                        if (stageKeyListener[0] != null) stage.removeListener(stageKeyListener[0]);
                        attackWindow.remove();
                        activeDialog = null;
                    } else {
                        addError(result.getMessage());
                    }
                }
            });

            navigationBar.add(backBtn).pad(10);
            navigationBar.add().expandX();
            navigationBar.add(launchBtn).pad(10);

            resizeAndCenterWindow.run();
        };

        // ===== مرحله 4: چینش (Drag & Drop) =====
        steps[3] = () -> {
            wizardContent.clear();
            navigationBar.clear();

            wizardContent.add(new Label("Step 4: Set Formation (Drag & Drop)", skin, "title")).left().padBottom(12).row();

            final Table formationTable = new Table(skin);
            formationTable.top().left();

            // مجموعه‌ای که ترتیب را نگه می‌دارد (اسم‌ها)
            final com.badlogic.gdx.utils.Array<String> items = new com.badlogic.gdx.utils.Array<>();
            for (SoldierType t : formationOrder) {
                items.add(t.name());
            }

            // تابعی برای بازسازی نمایش (در drop هم ازش استفاده می‌کنیم)
            final Runnable[] rebuildRef = new Runnable[1];
            rebuildRef[0] = () -> {
                formationTable.clear();
                // هر بار یک DragAndDrop جدید می‌سازیم تا منابع/مقصدهای قبلی تکرار نشوند
                final com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop dragAndDrop = new com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop();

                for (int i = 0; i < items.size; i++) {
                    final String name = items.get(i);

                    // ساخت یک کانتینر حاوی لیبل تا بتوانیم بک‌گراند و touchable قرار دهیم
                    final Label itemLabel = new Label(name, skin);
                    itemLabel.setAlignment(Align.center);
                    Container<Label> container = new Container<>(itemLabel);
                    container.setBackground(skin.newDrawable("white", Color.DARK_GRAY));
                    container.pad(8);
                    container.setTouchable(Touchable.enabled);
                    container.setName(name);

                    formationTable.add(container).growX().pad(4).row();

                    // Source (می‌توان کشید)
                    dragAndDrop.addSource(new com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source(container) {
                        @Override
                        public com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                            com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload payload = new com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload();
                            payload.setObject(name);

                            Label dragLabel = new Label(name, skin);
                            dragLabel.setAlignment(Align.center);
                            Container<Label> dragContainer = new Container<>(dragLabel);
                            dragContainer.setBackground(skin.newDrawable("white", Color.LIGHT_GRAY));
                            dragContainer.pad(8);
                            payload.setDragActor(dragContainer);

                            return payload;
                        }
                    });

                    // Target (می‌توان رها کرد)
                    dragAndDrop.addTarget(new com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target(container) {
                        @Override
                        public boolean drag(com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source source, com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload payload, float x, float y, int pointer) {
                            // همیشه اجازه می‌دهیم تا کاربر رها کند (می‌توان دیتا چک کرد)
                            return true;
                        }

                        @Override
                        public void drop(com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source source, com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload payload, float x, float y, int pointer) {
                            String dragged = (String) payload.getObject();
                            int fromIndex = items.indexOf(dragged, false);
                            int toIndex = items.indexOf(name, false);
                            if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                                items.removeIndex(fromIndex);
                                // اگر از قبل در لیست بالاتر بود، و می‌خواهیم قبل از عنصر مقصد درج کنیم،
                                // insert(toIndex, dragged) درست عمل می‌کند چون قبلاً حذف شده است.
                                items.insert(toIndex, dragged);
                                // بازسازی UI (rebuild دوباره Drag & Drop جدید می‌سازد)
                                rebuildRef[0].run();
                            }
                        }
                    });
                }
            };

            // اولین بار لیست رو بساز
            rebuildRef[0].run();

            ScrollPane scroll = new ScrollPane(formationTable, skin);
            scroll.setFadeScrollBars(false);
            wizardContent.add(scroll).width(680).height(320).row();

            // Back
            backBtn.clearListeners();
            backBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    steps[2].run();
                }
            });

            // Next -> برو به تایید (اول formationOrder را با items همگام می‌کنیم)
            TextButton formationNextBtn = new TextButton("Next: Confirm", skin);
            formationNextBtn.clearListeners();
            formationNextBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    goToConfirmStep(items, formationOrder);
                    steps[4].run();
                }
            });
            formationNextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    goToConfirmStep(items, formationOrder);
                    steps[4].run();
                }
            });

            // هندل Enter — قبل از افزودن لیسنر جدید، لیسنر قبلی رو حذف کن
            if (stageKeyListener[0] != null) stage.removeListener(stageKeyListener[0]);
            stageKeyListener[0] = new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER && attackWindow.isVisible()) {
                        goToConfirmStep(items, formationOrder);
                        steps[4].run();
                        return true;
                    }
                    return false;
                }
            };
            stage.addListener(stageKeyListener[0]);

            navigationBar.add(backBtn).pad(10);
            navigationBar.add().expandX();
            navigationBar.add(formationNextBtn).pad(10);

            resizeAndCenterWindow.run();
        };

        // ===== مرحله 3: انتخاب سربازان =====
        steps[2] = () -> {
            wizardContent.clear();
            navigationBar.clear();
            wizardContent.add(new Label("Step 3: Compose Your Army", skin, "title")).left().padBottom(12).row();

            final Map<SoldierType, TextField> qtyFields = new HashMap<>();
            Table soldiersTable = new Table(skin);

            final MyLinkedList<GameMediator.AttackReadySoldierInfo> available = mediator.getAvailableAttackSoldiers();
            for (GameMediator.AttackReadySoldierInfo s : available) {
                Table row = new Table(skin);
                row.pad(5);
                row.add(createPlaceholder(s.displayName, 1)).size(48).padRight(10);
                row.add(new Label(s.displayName + " (Av: " + s.availableCount + ")", skin)).left().expandX().fillX();

                TextField qty = new TextField("0", skin);
                qty.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
                qty.setAlignment(Align.center);
                qtyFields.put(s.type, qty);

                row.add(new Label("Qty:", skin)).padRight(4);
                row.add(qty).width(70);

                soldiersTable.add(row).expandX().fillX().row();
            }

            ScrollPane scrollPane = new ScrollPane(soldiersTable, skin);
            wizardContent.add(scrollPane).width(680).height(320).row();

            backBtn.clearListeners();
            backBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    steps[0].run();
                }
            });

            nextBtn.setText("Next: Set Formation");
            nextBtn.clearListeners();

            // Click مستقیم (برای تضمین واکنش)
            nextBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    goToFormationStep(qtyFields, available, selectedSoldieryCounts, formationOrder, steps[3]);
                }
            });
            // Change نیز اضافه می‌کنیم برای سازگاری
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    goToFormationStep(qtyFields, available, selectedSoldieryCounts, formationOrder, steps[3]);
                }
            });

            // هندل Enter — حذف لیسنر قبلی و افزودن لیسنر مرتبط با این مرحله
            if (stageKeyListener[0] != null) stage.removeListener(stageKeyListener[0]);
            stageKeyListener[0] = new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER && attackWindow.isVisible()) {
                        goToFormationStep(qtyFields, available, selectedSoldieryCounts, formationOrder, steps[3]);
                        return true;
                    }
                    return false;
                }
            };
            stage.addListener(stageKeyListener[0]);

            navigationBar.add(backBtn).pad(10);
            navigationBar.add().expandX();
            navigationBar.add(nextBtn).pad(10);

            resizeAndCenterWindow.run();
        };

        // ===== مرحله 1: انتخاب نوع حمله =====
        steps[0] = () -> {
            wizardContent.clear();
            navigationBar.clear();
            wizardContent.add(new Label("Step 1: Choose Attack Type", skin, "title")).left().padBottom(12).row();

            final java.util.function.Consumer<GameUser> onTargetSelectedCallback = (chosenTarget) -> {
                selectedTarget[0] = chosenTarget;
                if (activeDialog != null) activeDialog.setVisible(false);
                showPreAttackBriefingPanel(selectedTarget[0], () -> {
                    if (activeDialog != null) activeDialog.setVisible(true);
                    steps[2].run();
                });
            };

            TextButton groundBtn = new TextButton("Ground Attack", skin);
            groundBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent e, Actor a) {
                    if (mediator.checkGroundAttackFeasibility().isSuccess()) {
                        selectedAttackType[0] = AttackType.GROUND;
                        openInteractiveTargetMapPanel(AttackType.GROUND, onTargetSelectedCallback);
                    } else {
                        addError(mediator.checkGroundAttackFeasibility().getMessage());
                    }
                }
            });

            TextButton seaBtn = new TextButton("Sea Attack", skin);
            seaBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent e, Actor a) {
                    if (mediator.checkSeaAttackFeasibility().isSuccess()) {
                        selectedAttackType[0] = AttackType.SEA;
                        openInteractiveTargetMapPanel(AttackType.SEA, onTargetSelectedCallback);
                    } else {
                        addError(mediator.checkSeaAttackFeasibility().getMessage());
                    }
                }
            });

            Table buttonsTable = new Table();
            buttonsTable.add(groundBtn).size(260, 56).pad(8);
            buttonsTable.add(seaBtn).size(260, 56).pad(8);
            wizardContent.add(buttonsTable);

            resizeAndCenterWindow.run();
        };

        // ---- شروع و نمایش ----
        stage.addActor(attackWindow);
        activeDialog = attackWindow;
        steps[0].run();
    }

    // ===== متدهای کمکی =====
    private void goToFormationStep(Map<SoldierType, TextField> qtyFields,
                                   MyLinkedList<GameMediator.AttackReadySoldierInfo> available,
                                   Map<SoldierType, Integer> selectedSoldieryCounts,
                                   MyLinkedList<SoldierType> formationOrder,
                                   Runnable nextStep) {
        int total = 0;
        selectedSoldieryCounts.clear();

        for (GameMediator.AttackReadySoldierInfo s : available) {
            String txt = qtyFields.get(s.type).getText().trim();
            int val = 0;
            if (!txt.isEmpty()) {
                try {
                    val = Integer.parseInt(txt);
                } catch (NumberFormatException e) {
                    addError("Invalid number for " + s.displayName);
                    return;
                }
            }
            if (val > s.availableCount) {
                addError("Not enough " + s.displayName);
                return;
            }
            if (val > 0) {
                selectedSoldieryCounts.put(s.type, val);
                total += val;
            }
        }

        if (total <= 0) {
            addError("Select at least one soldier.");
            return;
        }

        formationOrder.clear();
        for (GameMediator.AttackReadySoldierInfo s : available) {
            if (selectedSoldieryCounts.containsKey(s.type)) {
                formationOrder.add(s.type);
            }
        }

        nextStep.run();
    }

    private void goToConfirmStep(com.badlogic.gdx.utils.Array<String> items, MyLinkedList<SoldierType> formationOrder) {
        formationOrder.clear();
        for (String s : items) {
            try {
                formationOrder.add(SoldierType.valueOf(s));
            } catch (IllegalArgumentException ex) {
                // اگر نامی نا‌معتبر بود نادیده گرفته می‌کنیم (نباید اتفاق بیفته)
            }
        }
    }



    private void openInteractiveTargetMapPanel(AttackType attackType, Consumer<GameUser> onTargetSelected) {
        MyLinkedList<GameMediator.AttackTargetInfo> validTargets = mediator.getPossibleAttackTargets(attackType);
        Set<Integer> validTargetIds = new HashSet<>();
        for (GameMediator.AttackTargetInfo info : validTargets) {
            validTargetIds.add(info.targetUser.getId());
        }

        final Dialog mapDialog = new Dialog("Select Your Target", skin, "default");
        mapDialog.setModal(true);
        mapDialog.pad(20);

        TextTooltip.TextTooltipStyle grayTooltipStyle = new TextTooltip.TextTooltipStyle(skin.get(TextTooltip.TextTooltipStyle.class));
        grayTooltipStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f));

        Stack mapStack = new Stack();
        mapStack.add(new Image(new Texture(Gdx.files.internal("images/map_visor.jpg"))));

        Table buttonLayer = new Table();
        buttonLayer.setFillParent(true);

        GameUser[] allUsers = currentGame.getUsers();
        if (allUsers != null) {
            for (GameUser user : allUsers) {
                if (user == null || user.getKingdom() == null || user.getId() == currentUser.getId()) continue;

                Kingdom kingdom = user.getKingdom();
                String kingdomDisplayName = kingdom.getType().name();
                TextButton kingdomButton = new TextButton(kingdomDisplayName, skin);
                kingdomButton.getLabel().setFontScale(0.8f);
                boolean isAttackable = validTargetIds.contains(user.getId());

                String username = user.getUsername();
                int castleLevel = kingdom.getCastle().getLevel();
                String status = isAttackable ? "Attackable" : "Not Attackable";
                String tooltipText = String.format(
                    "%s\nKingdom: %s\nCastle Lvl: %d\nStatus: %s",
                    username,
                    kingdomDisplayName,
                    castleLevel,
                    status
                );

                TextTooltip tooltip = new TextTooltip(tooltipText, TooltipManager.getInstance(), grayTooltipStyle);
                kingdomButton.addListener(tooltip);

                if (isAttackable) {
                    final GameUser finalTarget = user;
                    kingdomButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            mapDialog.hide();
                            onTargetSelected.accept(finalTarget);
                        }
                    });
                } else {
                    kingdomButton.setDisabled(true);
                    kingdomButton.getLabel().setColor(Color.GRAY);
                }

                positionKingdomButton(buttonLayer, kingdomButton, kingdom.getType());
            }
        }

        mapStack.add(buttonLayer);
        mapDialog.getContentTable().add(mapStack).size(600, 800);
        mapDialog.button("Cancel", false);
        mapDialog.show(stage);
    }


    private void positionKingdomButton(@NotNull Table container, Button button, KingdomType kingdomType) {
        container.addActor(button);
        switch (kingdomType) {
            case WINTERFELL:
                button.setPosition(270, 600);
                break;
            case IRON_ISLANDS:
                button.setPosition(35, 525);
                break;
            case EYRIE:
                button.setPosition(395, 405);
                break;
            case CASTERLY_ROCK:
                button.setPosition(105, 380);
                break;
            case DRAGONSTONE:
                button.setPosition(330, 270);
                break;
            case HIGHGARDEN:
                button.setPosition(105, 160);
                break;
            case STORMS_END:
                button.setPosition(310, 120);
                break;
        }
    }
    private Color getColorForKingdom(KingdomType kingdomType) {
        return switch (kingdomType) {
            case WINTERFELL -> Color.WHITE;
            case STORMS_END -> Color.YELLOW;
            case IRON_ISLANDS -> Color.DARK_GRAY;
            case DRAGONSTONE -> Color.RED;
            case HIGHGARDEN -> Color.GREEN;
            case EYRIE -> Color.CYAN;
            case CASTERLY_ROCK -> Color.GOLD;
            default -> Color.MAGENTA;
        };
    }


    private void showPreAttackBriefingPanel(final GameUser target, final Runnable onContinue) {
        final GameMediator.PreAttackBriefingInfo initialInfo = mediator.getBriefingInfo(target);
        if (initialInfo == null) {
            addError("Could not retrieve briefing info for the target.");
            if (activeDialog != null) activeDialog.setVisible(true);
            return;
        }

        final Dialog panel = new Dialog("Pre-Attack Briefing", skin, "default");
        panel.setModal(true);
        panel.pad(20);

        final Table content = new Table(skin);
        final Table infoTable = new Table(skin);
        final Table mapContainer = new Table(skin);
        final Label soldiersLabel = new Label("", skin);
        final Label resourcesLabel = new Label("", skin);
        final Label defenseHealthLabel = new Label("", skin);
        final TextButton useSpyButton = new TextButton("Use Spy", skin);

        infoTable.defaults().left().padBottom(10);
        infoTable.add(new Label("Target: " + target.getUsername(), skin, "title")).colspan(2).padBottom(15).row();
        infoTable.add(new Label("Est. Soldiers:", skin)).padRight(10); infoTable.add(soldiersLabel).row();
        infoTable.add(new Label("Est. Resources:", skin)).padRight(10); infoTable.add(resourcesLabel).row();
        infoTable.add(new Label("Defense Health:", skin)).padRight(10); infoTable.add(defenseHealthLabel).row();
        content.add(infoTable).expandY().top().padRight(20);
        content.add(mapContainer).size(300, 300);

        final Runnable updatePanelContent = () -> {
            GameMediator.PreAttackBriefingInfo currentInfo = mediator.getBriefingInfo(target);
            boolean showPreciseInfo = initialInfo.attackerHasSpy && !currentInfo.attackerHasSpy;

            mapContainer.clear();

            if (showPreciseInfo) {
                soldiersLabel.setText(String.valueOf(currentInfo.preciseTotalSoldiers));
                resourcesLabel.setText(String.format("Wood: %d, Food: %d", currentInfo.preciseWood, currentInfo.preciseFood));
                defenseHealthLabel.setText(String.valueOf(currentInfo.defenseHealth));
                useSpyButton.setDisabled(true);
                useSpyButton.setText("Spy Deployed");
                mapContainer.add(buildMapView(currentInfo.defenderMapLayout, false));
            } else {
                soldiersLabel.setText(currentInfo.vagueSoldierEstimate);
                resourcesLabel.setText(currentInfo.vagueResourceEstimate);
                defenseHealthLabel.setText("???");
                useSpyButton.setDisabled(!currentInfo.attackerHasSpy);
                Label mapPlaceholder = new Label("Map data is unavailable.\nUse a spy to reveal.", skin);
                mapPlaceholder.setAlignment(Align.center);
                mapContainer.add(mapPlaceholder).expand().fill();
            }
        };

        useSpyButton.setVisible(initialInfo.attackerHasSpy);
        useSpyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result result = mediator.useSpy();
                if (result.isSuccess()) {
                    addSystem(result.getMessage());
                    updatePanelContent.run();
                } else {
                    addError(result.getMessage());
                }
            }
        });

        TextButton continueButton = new TextButton("Continue to Attack", skin);
        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panel.hide();
                onContinue.run();
            }
        });

        panel.getContentTable().add(content).row();
        panel.getButtonTable().add(useSpyButton).pad(10);
        panel.getButtonTable().add(continueButton).pad(10);
        panel.button("Cancel", false);
        panel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!panel.isVisible()) { if (activeDialog != null) activeDialog.setVisible(true); }
            }
        });

        updatePanelContent.run();
        panel.show(stage);
    }


//    private Table buildMapView(Tile[][] mapGrid, boolean isBlurry) {
//        Table mapTable = new Table();
//        if (mapGrid == null) {
//            mapTable.add(new Label("Map Unavailable", skin));
//            return mapTable;
//        }
//
//        int mapRows = mapGrid.length;
//        int mapCols = mapGrid[0].length;
//        float tileSize = 300f / Math.max(mapRows, mapCols);
//
//        for (int r = 0; r < mapRows; r++) {
//            for (int c = 0; c < mapCols; c++) {
//                Tile tile = mapGrid[r][c];
//                Image tileImage;
//                if (tile != null && tile.getObjectOnTile() != null && tile.getObjectOnTile().getBuilding() != null) {
//                    Building building = tile.getObjectOnTile().getBuilding();
//                    tileImage = new Image(getBuildingTexture(building.getName(), building.getLevel() - 1));
//                } else {
//                    tileImage = new Image(tileTexture);
//                }
//
//                if (isBlurry) {
//                    tileImage.setColor(0.5f, 0.5f, 0.5f, 0.7f);
//                }
//
//                mapTable.add(tileImage).size(tileSize);
//            }
//            mapTable.row();
//        }
//        return mapTable;
//    }
private Table buildMapView(Tile[][] mapGrid, boolean isBlurry) {
    Table mapTable = new Table();
    if (mapGrid == null) {
        mapTable.add(new Label("Map Unavailable", skin));
        return mapTable;
    }

    int mapRows = mapGrid.length;
    int mapCols = mapGrid[0].length;
    float tileSize = 300f / Math.max(mapRows, mapCols);

    for (int r = 0; r < mapRows; r++) {
        for (int c = 0; c < mapCols; c++) {
            Tile tile = mapGrid[r][c];
            Image tileImage;

            if (tile != null && tile.getObjectOnTile() != null && tile.getObjectOnTile().getBuilding() != null) {
                Building building = tile.getObjectOnTile().getBuilding();
                // --- اصلاح شده: استفاده از متد امن ---
                tileImage = new Image(getCachedBuildingDrawable(building.getName(), building.getLevel() - 1));
            } else {
                // تایل خالی هم باید امن باشد (چون tileTexture را با safeLoad لود کردیم امن است)
                tileImage = new Image(tileTexture);
            }

            if (isBlurry) {
                tileImage.setColor(0.5f, 0.5f, 0.5f, 0.7f);
            }

            mapTable.add(tileImage).size(tileSize);
        }
        mapTable.row();
    }
    return mapTable;
}

    private void rebuildFormationTable(Table orderTable, MyLinkedList<SoldierType> formationOrder,
                                       Map<SoldierType, Integer> selectedSoldierCounts, Runnable refreshCallback) {
        orderTable.clear();
        for (int i = 0; i < formationOrder.getSize(); i++) {
            final int currentIndex = i;
            SoldierType t = formationOrder.get(currentIndex);
            String name = KingdomSoldierRegistry.resolveDisplayName(currentUser.getKingdom().getType(), t);
            int count = selectedSoldierCounts.getOrDefault(t, 0);

            Table row = new Table(skin);
            row.setBackground(skin.newDrawable("white", new Color(1,1,1,0.05f)));
            row.pad(8);

            row.add(new Label("#" + (i + 1), skin)).width(36).center();
            row.add(createPlaceholder(name, 1)).size(36).padRight(8);
            row.add(new Label(name + "  x" + count, skin)).left().expandX().fillX();

            TextButton up = new TextButton("↑", skin);
            if (i == 0) up.setDisabled(true);
            up.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    formationOrder.swap(currentIndex, currentIndex - 1);
                    refreshCallback.run();
                }
            });

            TextButton down = new TextButton("↓", skin);
            if (i >= formationOrder.getSize() - 1) down.setDisabled(true);
            down.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    formationOrder.swap(currentIndex, currentIndex + 1);
                    refreshCallback.run();
                }
            });

            row.add(up).size(40, 36).padRight(4);
            row.add(down).size(40, 36);
            orderTable.add(row).expandX().fillX().row();
        }
    }
    private void openTrainSoldiersPanel() {
        if (currentUser.getKingdom().getType() == KingdomType.DRAGONSTONE) {
            Table content = new Table(skin);
            content.pad(20);
            Label infoLabel = new Label("Dragonstone cannot train soldiers in Barracks.\n\nYou can hire elite mercenaries directly from the Bazar.", skin);
            infoLabel.setWrap(true);
            infoLabel.setAlignment(Align.center);

            TextButton bazarButton = new TextButton("Go to Bazar", skin);
            bazarButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (activeDialog != null) activeDialog.remove();
                    openBuyPanel();
                }
            });

            content.add(infoLabel).width(400).padBottom(20).row();
            content.add(bazarButton);
            openWindow("Mercenary Contracts", content);
            return;
        }

        Table content = new Table(skin);
        content.pad(15);
        Table listTable = new Table(skin);
        listTable.defaults().pad(10).left();
        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);

        MyLinkedList<GameMediator.TrainableUnitInfo> units = mediator.getTrainableUnits();
        for (GameMediator.TrainableUnitInfo unit : units) {
            Table itemCard = new Table(skin);
            itemCard.defaults().pad(7);
            itemCard.setBackground(skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 1f)));

            String displayName = unit.displayName;
            itemCard.add(createPlaceholder(String.valueOf(unit.type), 1)).size(80).padRight(10);

            Table textInfo = new Table();
            textInfo.add(new Label(displayName, skin)).left().row();
            String stats = String.format("HP: %d | AP: %d", unit.health, unit.attackPower);
            textInfo.add(new Label(stats, skin)).left().padTop(5).row();
            Label costLabel = new Label(unit.costString, skin);
            costLabel.setColor(Color.LIGHT_GRAY);
            costLabel.setFontScale(0.9f);
            textInfo.add(costLabel).left().padTop(5);
            itemCard.add(textInfo).expandX().left();

            Table actionTable = new Table();
            actionTable.add(new Label("Qty: ", skin)).right().padRight(5);
            final TextField qtyField = new TextField("1", skin);
            qtyField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
            actionTable.add(qtyField).width(80).padRight(10);

            TextButton requestBtn = new TextButton("Train", skin);
            requestBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    int qty = 0; try { qty = Integer.parseInt(qtyField.getText()); } catch (NumberFormatException ignored) {}
                    if (qty <= 0) {
                        addError("Please enter a valid quantity for " + displayName);
                        return;
                    }
                    java.util.Map<SoldierType, Integer> request = new java.util.HashMap<>();
                    request.put(unit.type, qty);

                    Result result = mediator.trainOrManageShip(request, false); // manageShipAction همیشه false است
                    if (result.isSuccess()) {
                        addSystem(result.getMessage());
                    } else {
                        addError(result.getMessage());
                    }
                }
            });
            actionTable.add(requestBtn).width(120);
            itemCard.add(actionTable).right();
            listTable.add(itemCard).expandX().fillX().row();
        }


        content.add(scrollPane).width(750).height(450).row();
        openWindow("Train Soldiers", content);
    }

    // In file: GamePlayScreen.java

    private void openMissionsPanel() {
        final Dialog panel = new Dialog("Ongoing Missions", skin, "default");
        panel.setModal(true);
        panel.pad(20);
        // ✅ افزایش اندازه پنل به ۷۰٪ عرض و ۶۰٪ ارتفاع صفحه
        panel.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.6f);

        Table mainContent = new Table();
        mainContent.top();

        MyLinkedList<GameMediator.MissionInfo> missions = mediator.getActiveMissionsInfo();

        if (missions.isEmpty()) {
            mainContent.add(new Label("You have no active missions.", skin)).expand().center();
        } else {
            for (GameMediator.MissionInfo info : missions) {
                mainContent.add(createMissionRow(info)).fillX().expandX().padBottom(15).row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(mainContent, skin);
        scrollPane.setFadeScrollBars(false);

        panel.getContentTable().add(scrollPane).expand().fill();

        TextButton refreshButton = new TextButton("Refresh", skin);
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                panel.hide();
                openMissionsPanel();
            }
        });

        panel.getButtonTable().add(refreshButton).width(120);
        panel.button("Close", true);
        panel.show(stage);
    }
//    private void openBattleHistoryPanel() {
//        MyLinkedList<GameMediator.BattleHistoryInfo> history = mediator.getBattleHistory();
//
//        final Dialog panel = new Dialog("Battle History", skin, "default");
//        panel.setModal(true);
//        panel.pad(20);
//        panel.setSize(Gdx.graphics.getWidth() * 0.8f, Gdx.graphics.getHeight() * 0.7f);
//
//        Table listTable = new Table(skin);
//        listTable.top();
//
//        if (history.isEmpty()) {
//            listTable.add(new Label("No battle records found.", skin)).expand().center();
//        } else {
//            Table header = new Table();
//            header.defaults().expandX().pad(5);
//            header.add(new Label("Type", skin));
//            header.add(new Label("Opponent", skin));
//            header.add(new Label("Result", skin));
//            header.add(new Label("Stars", skin));
//            header.add(new Label("Loot / Lost", skin));
//            header.add(new Label("Replay", skin));
//            listTable.add(header).fillX().padBottom(10).row();
//// کد اصلاح شده و صحیح
//            listTable.add(new Image(skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 1f)))).colspan(6).fillX().height(1).row();
//            for (final GameMediator.BattleHistoryInfo info : history) {
//                Label typeLabel = new Label(info.isAttack ? "ATTACK" : "DEFENSE", skin);
//                typeLabel.setColor(info.isAttack ? Color.GREEN : Color.FIREBRICK);
//                Label starsLabel = new Label(String.valueOf(info.displayStars) + " ★", skin);
//                starsLabel.setColor(Color.GOLD);
//
//                Label lootLabel = new Label(info.lootString, skin);
//                lootLabel.setFontScale(0.8f);
//
//                TextButton replayButton = new TextButton("Watch", skin);
//                replayButton.addListener(new ClickListener() {
//                    @Override
//                    public void clicked(InputEvent event, float x, float y) {
//                        openBattleReplayPanel(info);
//                    }
//                });
//
//                if (info.replayFilePath == null || info.replayFilePath.equals("N/A")) {
//                    replayButton.setDisabled(true);
//                }
//
//                listTable.add(typeLabel).pad(8).center();
//                listTable.add(new Label(info.opponentName, skin)).pad(8).center();
//                listTable.add(new Label(info.resultText, skin)).pad(8).center();
//                listTable.add(starsLabel).pad(8).center();
//                listTable.add(lootLabel).pad(8).center();
//                listTable.add(replayButton).pad(8).center();
//                listTable.row();
//            }
//        }
//
//        ScrollPane scrollPane = new ScrollPane(listTable, skin);
//        scrollPane.setFadeScrollBars(false);
//
//        panel.getContentTable().add(scrollPane).expand().fill();
//        panel.button("Close", true);
//        panel.show(stage);
//    }

private void openBattleHistoryPanel() {
    MyLinkedList<GameMediator.BattleHistoryInfo> history = mediator.getBattleHistory();

    final Dialog panel = new Dialog("Battle Log", skin, "default");
    panel.setModal(true);
    panel.pad(20);
    // تنظیم اندازه بزرگتر برای دید بهتر
    panel.setSize(Gdx.graphics.getWidth() * 0.85f, Gdx.graphics.getHeight() * 0.75f);

    Table listTable = new Table(skin);
    listTable.top();

    if (history.isEmpty()) {
        listTable.add(new Label("No battle records found.", skin)).expand().center();
    } else {
        // --- ساخت هدر جدول ---
        Table header = new Table(skin);
        header.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f)));
        header.defaults().expandX().center().pad(10);

        Label h1 = new Label("Type", skin); h1.setColor(Color.CYAN);
        Label h2 = new Label("Opponent", skin); h2.setColor(Color.CYAN);
        Label h3 = new Label("Result", skin); h3.setColor(Color.CYAN);
        Label h4 = new Label("Resources (Loot/Lost)", skin); h4.setColor(Color.CYAN);
        Label h5 = new Label("Action", skin); h5.setColor(Color.CYAN);

        header.add(h1).width(80);
        header.add(h2).width(150);
        header.add(h3).width(100);
        header.add(h4).width(300); // فضای زیاد برای منابع
        header.add(h5).width(100);

        listTable.add(header).growX().padBottom(5).row();

        // --- پر کردن ردیف‌ها ---
        for (final GameMediator.BattleHistoryInfo info : history) {
            Table row = new Table(skin);
            row.setBackground(skin.newDrawable("white", new Color(1, 1, 1, 0.05f))); // رنگ پس‌زمینه یک درمیان
            row.defaults().center().pad(5);

            // 1. نوع (حمله/دفاع)
            String typeStr = info.isAttack ? "ATTACK" : "DEFENSE";
            Label typeLabel = new Label(typeStr, skin);
            typeLabel.setColor(info.isAttack ? Color.ORANGE : new Color(0.4f, 0.8f, 1f, 1f));

            // 2. نام حریف
            Label nameLabel = new Label(info.opponentName, skin);

            // 3. نتیجه و ستاره
            Label resultLabel = new Label(info.resultText + " (" + info.displayStars + "★)", skin);
            if (info.resultText.toLowerCase().contains("vict") || info.resultText.toLowerCase().contains("defens")) {
                resultLabel.setColor(Color.GREEN);
            } else {
                resultLabel.setColor(Color.FIREBRICK);
            }

            // 4. منابع (استفاده از متد گرافیکی)
            Table lootTable = createLootDisplay(info.lootString);

            // 5. دکمه تماشا
            TextButton replayButton = new TextButton("Watch", skin);
            if (info.replayFilePath == null || info.replayFilePath.equals("N/A")) {
                replayButton.setDisabled(true);
                replayButton.setColor(Color.DARK_GRAY);
            } else {
                replayButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        openBattleReplayPanel(info);
                    }
                });
            }

            row.add(typeLabel).width(80);
            row.add(nameLabel).width(150);
            row.add(resultLabel).width(100);
            row.add(lootTable).width(300);
            row.add(replayButton).width(100);

            listTable.add(row).growX().padBottom(2).row();
        }
    }

    ScrollPane scrollPane = new ScrollPane(listTable, skin);
    scrollPane.setFadeScrollBars(false);

    panel.getContentTable().add(scrollPane).expand().fill();
    panel.button("Close", true);
    panel.show(stage);
}

    private Image shipActorOnMap;

    private void openShippingPanel() {
        final GameMediator.ShipInfo info = mediator.getShipInfo();
        if (info == null) return;

        if (!info.isIronIslands && !info.hasPortAccess) {
            addError("You need a Port to access shipping.");
            return;
        }

        Table content = new Table(skin);
        content.pad(20);
        content.add(new Image(shipTexture)).size(128).padBottom(20).row();
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));


        if (info.isIronIslands) {
            content.add(new Label("Your Warship Status", skin, "title")).padBottom(10).row();
            content.add(new Label("Level: " + info.level, skin)).left().row();
            content.add(new Label("Attack Power: " + info.attackPower, skin)).left().row();
            content.add(new Label("Capacity: " + info.capacity, skin)).left().row();
            content.add(new Label("Upgrade Cost: " + info.costString, skin)).padTop(10).left().row();
        } else {
            content.add(new Label("Fleet Management", skin, "title")).padBottom(10).row();
            if (info.hasRentedShip) {
                content.add(new Label("You have a ship rented for one attack.", skin)).center();
            } else {
                content.add(new Label("You do not have a permanent ship.", skin)).center().row();
                content.add(new Label("Rental Cost: " + info.costString, skin)).padTop(10).center().row();
            }
        }

        if (!"N/A".equals(info.actionText)) {
            TextButton actionButton = new TextButton(info.actionText, skin);
            actionButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result result = mediator.performShipAction();
                    if (result.isSuccess()) {
                        addSystem(result.getMessage());

                        updateShipOnStage();

                        if (activeDialog != null) activeDialog.remove();
                        openShippingPanel();
                    } else {
                        addError(result.getMessage());
                    }
                }
            });
            content.add(actionButton).width(250).padTop(20).row();
        }

        updateShipOnStage();

        openWindow("Shipping Management", content);
    }

    private void updateShipOnStage() {
        GameMediator.ShipInfo info = mediator.getShipInfo();
        if (info == null) return;

        if (info.hasRentedShip || info.isIronIslands) {
            if (shipActorOnMap == null) {
                shipActorOnMap = new Image(shipTexture);                shipActorOnMap.setSize(50, 50);
                shipActorOnMap.setPosition(1490, 585);
                stage.addActor(shipActorOnMap);
            }
        } else {
            if (shipActorOnMap != null) {
                shipActorOnMap.remove();
                shipActorOnMap = null;
            }
        }
    }

    private void openKingdomPanel() {
        Table t = new Table(skin);
        t.defaults().pad(8).width(250).fillX();

        Label title = new Label("Defense Management", skin, "title");
        TextButton upgradeDefBtn = new TextButton("Upgrade Defenses", skin);
        TextButton repairDefBtn = new TextButton("Repair Defenses", skin);
        TextButton deployArchersBtn = new TextButton("Deploy Archers to Wall", skin); // New Button

        upgradeDefBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (activeDialog != null) activeDialog.remove();
                openUpgradeDefensePanel();
            }
        });

        repairDefBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (activeDialog != null) activeDialog.remove();
                openRepairDefensePanel();
            }
        });

        deployArchersBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (activeDialog != null) activeDialog.remove();
                openDeployArchersPanel();
            }
        });

        t.add(title).colspan(2).padBottom(10).row();
        t.add(upgradeDefBtn).row();
        t.add(repairDefBtn).row();
        t.add(deployArchersBtn).padTop(10).row();

        openWindow("Kingdom", t);
    }

    private void openUpgradeDefensePanel() {
        GameMediator.DefenseUpgradeInfo info = mediator.getDefenseComplexUpgradeInfo();

        if (info == null) {
            addError("Could not retrieve upgrade information.");
            return;
        }

        if (!info.canUpgrade) {
            addNotif("INFO: " + info.statusMessage);
            return;
        }
        Table content = new Table(skin);
        content.pad(20);

        content.add(new Label("Upgrade Defense to Level " + info.nextLevel, skin, "title")).colspan(2).padBottom(15).row();

        content.add(new Label("Total Cost:", skin)).left().padBottom(10).colspan(2).row();
        content.add(new Label("Wood:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalWoodCost), skin)).left().row();
        content.add(new Label("Stone:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalStoneCost), skin)).left().row();
        content.add(new Label("Coin:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalCoinCost), skin)).left().row();

        content.add(new Label("Total Time:", skin)).left().padTop(15).padRight(10);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
        content.add(new Label(info.totalTime + " seconds", skin)).left().padTop(15).row();

        TextButton confirmButton = new TextButton("Confirm Upgrade", skin);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result result = mediator.executeDefenseComplexUpgrade();
                addNotif(result.getMessage());
                if (result.isSuccess() && activeDialog != null) {
                    activeDialog.remove();
                    activeDialog = null;
                }
            }
        });

        content.add(confirmButton).colspan(2).width(300).padTop(25);
        openWindow("Upgrade Defense Complex", content);
    }

    private void openRepairDefensePanel() {
        GameMediator.DefenseRepairInfo info = mediator.getDefenseComplexRepairInfo();

        if (info == null) {
            addError("Could not retrieve repair information.");
            return;
        }

        if (!info.needsRepair) {
            addNotif("INFO: Your defense complex does not need any repairs.");
            return;
        }

        Table content = new Table(skin);
        content.pad(20);

        content.add(new Label("Repair Defense Complex", skin, "title")).colspan(2).padBottom(15).row();

        content.add(new Label("Total Repair Cost:", skin)).left().padBottom(10).colspan(2).row();
        content.add(new Label("Wood:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalWoodCost), skin)).left().row();
        content.add(new Label("Stone:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalStoneCost), skin)).left().row();
        content.add(new Label("Coin:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.totalCoinCost), skin)).left().row();

        content.add(new Label("Total Time:", skin)).left().padTop(15).padRight(10);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
        content.add(new Label(info.totalTime + " seconds", skin)).left().padTop(15).row();

        TextButton confirmButton = new TextButton("Confirm Repair", skin);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result result = mediator.executeDefenseComplexRepair();
                addNotif(result.getMessage());
                if (result.isSuccess() && activeDialog != null) {
                    activeDialog.remove();
                    activeDialog = null;
                }
            }
        });

        content.add(confirmButton).colspan(2).width(300).padTop(25);
        openWindow("Repair Defense Complex", content);
    }

    private void openDeployArchersPanel() {
        final GameMediator.ArcherDeploymentInfo info = mediator.getArcherDeploymentInfo();

        if (info.availableArchers == 0 && info.archersOnWall == 0) {
            addNotif("INFO: You have no archers to manage.");
            return;
        }

        Table content = new Table(skin);
        content.pad(20);
        content.add(new Label("Deploy Archers to the Wall", skin, "title")).colspan(2).padBottom(15).row();

        content.add(new Label("Available Archers (in field):", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.availableArchers), skin)).left().row();
        content.add(new Label("Archers on Wall:", skin)).right().padRight(10);
        content.add(new Label(String.valueOf(info.archersOnWall), skin)).left().row();

        content.add(new Label("Quantity to Deploy:", skin)).right().padTop(20).padRight(10);
        final TextField quantityField = new TextField("0", skin);
        quantityField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        content.add(quantityField).width(100).padTop(20).left().row();

        TextButton confirmButton = new TextButton("Confirm Deployment", skin);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
        content.add(confirmButton).colspan(2).width(300).padTop(25);

        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityField.getText());
                } catch (NumberFormatException e) {
                    addError("ERROR: Please enter a valid number.");
                    return;
                }

                Result result = mediator.deployArchersToWall(quantity);

                if (result.isSuccess()) {
                    addSystem("SUCCESS: " + result.getMessage());
                    if (activeDialog != null) {
                        activeDialog.remove();
                        activeDialog = null;
                    }
                    openDeployArchersPanel();
                } else {
                    addError("ERROR: " + result.getMessage());
                }
            }
        });

        openWindow("Deploy Archers", content);
    }

    private void showWhiteWalkerIntelligencePanel() {
        GameMediator.WhiteWalkerInfo info = mediator.getWhiteWalkerInfo();

        Table content = new Table(skin);
        content.pad(25);
        content.defaults().left().pad(8);

        Label title = new Label("The White Walker Threat", skin, "title");
        title.setColor(Color.CYAN);
        content.add(title).colspan(2).center().padBottom(20).row();

        addInfoRow(content, "Status:", info.isFinalWaveTriggered ? "FINAL WAVE ACTIVE" : "Gathering Strength",
            info.isFinalWaveTriggered ? Color.CYAN : Color.LIGHT_GRAY);
        addInfoRow(content, "First Attack Wave After Day:", String.valueOf(info.startDay), Color.WHITE);
        addInfoRow(content, "Attack Interval (days):", String.valueOf(info.attackInterval), Color.WHITE);
        addInfoRow(content, "The Long Night (Final Wave):", "Day " + info.finalDay, Color.SKY);
        addInfoRow(content, "Current Strategy:", info.currentStrategy, Color.LIGHT_GRAY);

        Label subTitle = new Label("Recent Activity", skin, "title");
        subTitle.setColor(Color.ORANGE);
        content.add(subTitle).colspan(2).center().padTop(25).padBottom(10).row();

        Table logTable = new Table(skin);
        int wwMessages = 0;
        for (String msg : messageLog) {
            if (msg.startsWith("WHITEWALKER::")) {
                Label logLabel = new Label("• " + msg.substring(13), skin);
                logLabel.setWrap(true);
                logLabel.setColor(Color.LIGHT_GRAY);
                logTable.add(logLabel).width(450).left().pad(4).row();
                wwMessages++;
                if (wwMessages >= 5) break;
            }
        }
        if (wwMessages == 0) {
            logTable.add(new Label("The North is quiet... for now.", skin)).pad(5);
        }

        ScrollPane logPane = new ScrollPane(logTable, skin);
        logPane.setFadeScrollBars(false);
        content.add(logPane).colspan(2).height(160).fillX();

        openWindow("White Walker Intelligence", content);
    }

    private void addInfoRow(Table table, String label, String value, Color valueColor) {
        table.add(new Label(label, skin)).right().padRight(12);
        Label valueLabel = new Label(value, skin);
        valueLabel.setColor(valueColor);
        table.add(valueLabel).left().row();
    }

    private void showWallManagementPanel() {
        Table content = new Table(skin);
        content.pad(20);
        content.defaults().pad(8).left();

        Label title = new Label("The Northern Wall", skin, "title");
        title.setColor(Color.SKY);
        content.add(title).colspan(2).center().padBottom(20).row();

        GameMediator.WallInfo info = mediator.getWallInfo();
        content.add(new Label("Wall Strength:", skin)).right().padRight(12);
        Label strengthLabel = new Label(info.healthStatus, skin);
        strengthLabel.setColor(Color.LIGHT_GRAY);
        content.add(strengthLabel).left().row();

        content.add(new Label("Supplies:", skin)).right().padRight(12);
        content.add(new Label(info.healthStatus, skin)).left().row();
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));

        TextButton topContributorsButton = new TextButton("Top Contributors", skin);
        TextButton allContributionsButton = new TextButton("View All Contributions", skin);

        topContributorsButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { openContributionsDialog(true); }
        });
        allContributionsButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { openContributionsDialog(false); }
        });

        content.add(topContributorsButton).colspan(2).width(320).padTop(20).row();
        content.add(allContributionsButton).colspan(2).width(320).pad(10).row();

        openWindow("The Wall Management", content);
    }

    private void openWhiteWalkerMainMenuPanel() {
        Table content = new Table(skin);
        content.defaults().width(320).pad(12);

        Label title = new Label("The North & The Wall", skin, "title");
        title.setColor(Color.CYAN);
        content.add(title).padBottom(20).row();

        TextButton intelligenceBtn = new TextButton("Intelligence Report", skin);
        TextButton wallBtn = new TextButton("Wall Management", skin);
        TextButton helpBtn = new TextButton("Reinforce the Wall", skin);

        intelligenceBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (activeDialog != null) activeDialog.remove();
                showWhiteWalkerIntelligencePanel();
            }
        });
        wallBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (activeDialog != null) activeDialog.remove();
                showWallManagementPanel();
            }
        });
        helpBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (activeDialog != null) activeDialog.remove();
                openHelpPanel();
            }
        });

        content.add(intelligenceBtn).row();
        content.add(wallBtn).row();
        content.add(helpBtn).row();

        openWindow("The North & The Wall", content);
    }

    private void openContributionsDialog(boolean topOnly) {
        String title = topOnly ? "Top 5 Wall Contributors" : "All Wall Contributions";
        MyLinkedList<GameMediator.ContributionInfo> contributions = mediator.getWallContributions();

        java.util.ArrayList<GameMediator.ContributionInfo> sortedList = new java.util.ArrayList<>();
        for (GameMediator.ContributionInfo info : contributions) {
            sortedList.add(info);
        }
        sortedList.sort((c1, c2) -> Long.compare(c2.amount, c1.amount));

        Table listTable = new Table(skin);
        listTable.pad(10);
        listTable.defaults().pad(5);
        listTable.add(new Label("Rank", skin, "title")).padRight(15);
        listTable.add(new Label("Player", skin, "title")).expandX().left();
        listTable.add(new Label("Kingdom", skin, "title")).width(150).left();
        listTable.add(new Label("Total Amount", skin, "title")).width(150).right().row();

        if (sortedList.isEmpty()) {
            listTable.add(new Label("No contributions recorded yet.", skin)).colspan(4).center().pad(20);
        } else {
            int limit = topOnly ? 5 : sortedList.size();
            for (int i = 0; i < Math.min(limit, sortedList.size()); i++) {
                GameMediator.ContributionInfo info = sortedList.get(i);
                listTable.add(new Label("#" + (i + 1), skin));
                listTable.add(new Label(info.userName, skin)).left();
                listTable.add(new Label(info.kingdomName, skin)).left();
                listTable.add(new Label(String.valueOf(info.amount), skin)).right().row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        openCenteredWindowWithFixedHeight(title, scrollPane, 400);
    }

    private void openBuyPanel() {
        Table content = new Table(skin);
        content.pad(15);
        content.add(new Label("Bazar - Available Offers", skin, "title")).padBottom(10).row();

        Table listTable = new Table(skin);
        listTable.defaults().pad(8);
        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);

        MyLinkedList<Transaction> transactions = mediator.getMarketTransactions();

        if (transactions.isEmpty()) {
            listTable.add(new Label("The market is currently empty.", skin));
        } else {
            int transactionIndex = 0;
            for (Transaction transaction : transactions) {
                Table itemCard = new Table(skin);
                itemCard.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f)));
                itemCard.defaults().pad(5);

                String offerDetails = getTransactionGiveString(transaction);
                String priceDetails = getTransactionTakeString(transaction);

                String sellerName;
                GameUser seller = transaction.getExchanger();
                if (seller != null) {
                    sellerName = seller.getUsername();
                } else {
                    sellerName = "System Offer";
                }

                Image offerIcon = new Image(getIconForTransaction(transaction));
                Label detailsLabel = new Label("Offer: " + offerDetails + "\nPrice: " + priceDetails + "\nSeller: " + sellerName, skin);
                detailsLabel.setWrap(true);

                itemCard.add(offerIcon).size(64).pad(10);
                itemCard.add(detailsLabel).width(400).left();

                TextButton buyButton = new TextButton("Buy", skin);
                final int finalIndex = transactionIndex;
                buyButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Result result = mediator.buyMarketTransaction(finalIndex);
                        if (result.isSuccess()) {
                            addSystem(result.getMessage());
                            if (activeDialog != null) activeDialog.remove();
                            openBuyPanel();
                        } else {
                            addError(result.getMessage());
                        }
                    }
                });
                itemCard.add(buyButton).width(120).pad(10);

                listTable.add(itemCard).expandX().fillX().row();
                transactionIndex++;
            }
        }

        content.add(scrollPane).width(750).height(450);
        openWindow("Buy From Market", content);
    }

    private void openSellPanel() {
        final Object[] selectedItemToSell = {null};

        Table content = new Table(skin);
        content.pad(15);
        content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
        content.add(new Label("List an Item on the Market", skin, "title")).padBottom(10).row();

        Table tabs = new Table(skin);
        TextButton sellResourcesBtn = new TextButton("Sell Resources", skin, "toggle");
        TextButton sellSoldiersBtn = new TextButton("Sell Soldiers", skin, "toggle");
        new ButtonGroup<>(sellResourcesBtn, sellSoldiersBtn).setMaxCheckCount(1);
        tabs.add(sellResourcesBtn).pad(5);
        tabs.add(sellSoldiersBtn).pad(5);

        final Table selectionTable = new Table(skin);
        ScrollPane selectionScroll = new ScrollPane(selectionTable, skin);
        selectionScroll.setFadeScrollBars(false);

        final Table detailsTable = new Table(skin);
        detailsTable.setVisible(false);
        detailsTable.defaults().pad(5);

        final TextField quantityField = new TextField("1", skin);
        quantityField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        final java.util.Map<ResourceType, TextField> priceFields = new java.util.HashMap<>();
        priceFields.put(ResourceType.Wood, new TextField("0", skin));
        priceFields.put(ResourceType.Stone, new TextField("0", skin));
        priceFields.put(ResourceType.Food, new TextField("0", skin));
        priceFields.put(ResourceType.Coin, new TextField("0", skin));

        detailsTable.add(new Label("Quantity to Sell:", skin));
        detailsTable.add(quantityField).width(100).row();
        detailsTable.add(new Label("Asking Price:", skin)).colspan(4).padTop(10).row();
        detailsTable.add(new Label("W:", skin)); detailsTable.add(priceFields.get(ResourceType.Wood)).width(80);
        detailsTable.add(new Label("S:", skin)); detailsTable.add(priceFields.get(ResourceType.Stone)).width(80).row();
        detailsTable.add(new Label("F:", skin)); detailsTable.add(priceFields.get(ResourceType.Food)).width(80);
        detailsTable.add(new Label("C:", skin)); detailsTable.add(priceFields.get(ResourceType.Coin)).width(80);

        TextButton listOfferButton = new TextButton("List Offer on Market", skin);
        listOfferButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (selectedItemToSell[0] == null) {
                    addError("Please select an item to sell first.");
                    return;
                }

                try {
                    int quantity = Integer.parseInt(quantityField.getText());
                    if (quantity <= 0) {
                        addError("Quantity must be greater than zero.");
                        return;
                    }

                    MyLinkedList<ResourceItem> toTake = new MyLinkedList<>();
                    for(java.util.Map.Entry<ResourceType, TextField> entry : priceFields.entrySet()) {
                        int amount = Integer.parseInt(entry.getValue().getText());
                        if (amount > 0) toTake.add(new ResourceItem(entry.getKey(), amount));
                    }
                    if (toTake.isEmpty()) {
                        addError("You must set a price for your offer.");
                        return;
                    }

                    Result result;
                    if (selectedItemToSell[0] instanceof ResourceType) {
                        MyLinkedList<ResourceItem> toGive = new MyLinkedList<>();
                        toGive.add(new ResourceItem((ResourceType) selectedItemToSell[0], quantity));
                        result = mediator.sellResourcesOnMarket(toGive, toTake);
                    } else if (selectedItemToSell[0] instanceof SoldierType) {
                        MyLinkedList<ArmyManager.TroopRequest> toGive = new MyLinkedList<>();
                        toGive.add(new ArmyManager.TroopRequest((SoldierType) selectedItemToSell[0], quantity));
                        result = mediator.sellSoldiersOnMarket(toGive, toTake);
                    } else {
                        return;
                    }

                    if (result.isSuccess()) {
                        addSystem(result.getMessage());
                        if (activeDialog != null) activeDialog.remove();
                    } else {
                        addError(result.getMessage());
                    }

                } catch (NumberFormatException e) {
                    addError("Please enter valid numbers for quantity and price.");
                }
            }
        });

        sellResourcesBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (sellResourcesBtn.isChecked()) {
                    selectedItemToSell[0] = null;
                    detailsTable.setVisible(false);
                    selectionTable.clear();

                    String[] resourceTypes = {"Wood", "Stone", "Food", "Coin"};

                    for (String typeStr : resourceTypes) {
                        final ResourceType type = ResourceType.valueOf(typeStr);

                        String resourceInfo;
                        switch (typeStr) {
                            case "Wood":  resourceInfo = mediator.getResourceInfoWood();  break;
                            case "Stone": resourceInfo = mediator.getResourceInfoStone(); break;
                            case "Food":  resourceInfo = mediator.getResourceInfoFood();  break;
                            case "Coin":  resourceInfo = mediator.getResourceInfoCoin();  break;
                            default:      resourceInfo = "0 / 0";
                        }

                        String amount = "0";
                        if (resourceInfo.contains("/")) {
                            amount = resourceInfo.split("/")[0].trim();
                        }

                        TextButton itemBtn = new TextButton(typeStr + " (Av: " + amount + ")", skin);
                        itemBtn.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedItemToSell[0] = type;
                                detailsTable.setVisible(true);
                            }
                        });
                        selectionTable.add(itemBtn).width(300).pad(5).row();
                    }
                }
            }
        });

        sellSoldiersBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (sellSoldiersBtn.isChecked()) {
                    selectedItemToSell[0] = null;
                    detailsTable.setVisible(false);
                    selectionTable.clear();
                    MyLinkedList<ArmyManager.TroopRequest> sellableSoldiers = mediator.getSellableSoldiers();
                    for (ArmyManager.TroopRequest soldierInfo : sellableSoldiers) {
                        final SoldierType type = soldierInfo.type;
                        String name = KingdomSoldierRegistry.resolveDisplayName(currentUser.getKingdom().getType(), type);
                        TextButton itemBtn = new TextButton(name + " (Av: " + soldierInfo.count + ")", skin);
                        itemBtn.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedItemToSell[0] = type;
                                detailsTable.setVisible(true);
                            }
                        });
                        selectionTable.add(itemBtn).width(300).pad(5).row();
                    }
                }
            }
        });

        content.add(tabs).row();
        content.add(selectionScroll).width(750).height(150).padTop(10).row();
        content.add(detailsTable).padTop(15).row();
        content.add(listOfferButton).width(300).padTop(15).row();

        openWindow("Sell on Market", content);
        sellResourcesBtn.setChecked(true);
    }

    private String getTransactionTakeString(Transaction t) {
        StringBuilder sb = new StringBuilder();
        if (t.getSoldiersToTake() != null && t.getSoldiersToTake().getCount() > 0) {
            sb.append(t.getSoldiersToTake().toDisplayString(null)).append(", ");
        }
        if (t.getResourcesToTake() != null && !t.getResourcesToTake().isEmpty()) {
            for(ResourceItem item : t.getResourcesToTake()) {
                sb.append(item.amount).append(" ").append(item.type.name()).append(", ");
            }
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.length() > 0 ? sb.toString() : "Free";
    }

    private String getTransactionGiveString(Transaction t) {
        StringBuilder sb = new StringBuilder();

        if (t.getSoldiersToGive() != null && t.getSoldiersToGive().getCount() > 0) {
            SoldierUnit unit = t.getSoldiersToGive();
            String displayName;
            GameUser seller = t.getExchanger();

            // اگر فروشنده یک بازیکن واقعی است، از نام مخصوص پادشاهی او استفاده کن
            if (seller != null && seller.getKingdom() != null) {
                displayName = KingdomSoldierRegistry.resolveDisplayName(seller.getKingdom().getType(), unit.getType());
            } else {
                // اگر فروشنده سیستم است، از نام پایه سرباز استفاده کن
                displayName = unit.getType().name();
            }
            sb.append(unit.getCount()).append("x ").append(displayName).append(", ");
        }

        if (t.getResourcesToGive() != null && !t.getResourcesToGive().isEmpty()) {
            for(ResourceItem item : t.getResourcesToGive()) {
                sb.append(item.amount).append(" ").append(item.type.name()).append(", ");
            }
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.length() > 0 ? sb.toString() : "Nothing";
    }

    private Texture getIconForTransaction(Transaction t) {
        if (t.getSoldiersToGive() != null && t.getSoldiersToGive().getCount() > 0) {
            String soldierTypeName = t.getSoldiersToGive().getType().name();
            return GameFileUtility.getBuildingTexture(soldierTypeName, 0);
        }
        if (t.getResourcesToGive() != null && !t.getResourcesToGive().isEmpty()) {
            String resourceName = t.getResourcesToGive().getHead().data.type.name();
            return GameFileUtility.getBuildingTexture(resourceName, 0);
        }
        return GameFileUtility.getBuildingTexture("default", 0);
    }

// In file: GamePlayScreen.java
// In file: GamePlayScreen.java

    @Override
    public void disablePlayerControlsForObservation() {
        // 1. غیرفعال کردن منوی اصلی سمت راست
        if (this.rightPanelButtons != null) {
            this.rightPanelButtons.setTouchable(Touchable.disabled);
            for (Actor child : this.rightPanelButtons.getChildren()) {
                if (child instanceof Button) {
                    ((Button) child).setDisabled(true);
                }
            }
        }
        // 2. غیرفعال کردن دکمه‌های بالا-راست (منو، نقشه، پیام‌ها)
        if (this.topRightButtons != null) {
            this.topRightButtons.setTouchable(Touchable.disabled);
            for (Actor child : this.topRightButtons.getChildren()) {
                if (child instanceof Button) {
                    ((Button) child).setDisabled(true);
                }
            }
        }
        // 3. غیرفعال کردن کلیک روی نقشه بازی
        if (this.gridTable != null) {
            this.gridTable.setTouchable(Touchable.disabled);
        }

        addSystem("You are now in observation mode. All controls are disabled.");
    }
    public void showDefeatPanel() {
        game.getMusicManager().playMusic("music_defeat"); // <-- این خط را اضافه کنید

        // مرحله ۱: بلافاصله و قبل از هرچیز، بازیکن را به حالت تماشاچی ببر
        disablePlayerControlsForObservation();

        // مرحله ۲: یک پنل تصمیم‌گیری غیرقابل بستن نمایش بده
        final Dialog defeatDialog = new Dialog("Your Kingdom Has Fallen", skin, "default") {
            @Override
            public void cancel() {
                // با خالی گذاشتن این متد، از بسته شدن پنجره جلوگیری می‌کنیم
            }
        };

        defeatDialog.setModal(true);
        defeatDialog.setMovable(false);
// defeatDi
        Table content = new Table(skin);
        content.pad(25);

        Label titleLabel = new Label("YOU HAVE BEEN DEFEATED", skin, "title");
        titleLabel.setColor(Color.FIREBRICK);

        Label messageLabel = new Label(
            "Your castle was destroyed. You can continue to observe the game or return to the main menu.",
            skin
        );
        messageLabel.setWrap(true);
        messageLabel.setAlignment(Align.center);

        TextButton observeButton = new TextButton("Continue Observing", skin);
        observeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // فقط پنل را می‌بندیم. کنترل‌ها از قبل غیرفعال شده‌اند.
                defeatDialog.hide();
            }
        });

        TextButton mainMenuButton = new TextButton("Return to Main Menu", skin);
        mainMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                defeatDialog.hide();
                mediator.goToGameJoin(); // بازگشت به لابی بازی‌ها
            }
        });

        content.add(titleLabel).padBottom(30).row();
        content.add(messageLabel).width(500).padBottom(40).row();
        Table buttonTable = new Table();
        buttonTable.add(observeButton).width(220).height(50).padRight(10);
        buttonTable.add(mainMenuButton).width(220).height(50).padLeft(10);
        content.add(buttonTable).row();

        defeatDialog.getContentTable().add(content);
        defeatDialog.pack();
        defeatDialog.setPosition(
            (stage.getWidth() - defeatDialog.getWidth()) / 2f,
            (stage.getHeight() - defeatDialog.getHeight()) / 2f
        );
        stage.addActor(defeatDialog);
    }

    public void showGameResultsPanel(GameRecord record) {
        boolean isWinner = false;
        for (PlayerResult result : record.getPlayerResults()) {
            if (result.getUsername().equals(currentUser.getUsername())) {
                if (result.getStatus() == PlayerResult.PlayerStatus.WINNER || result.getStatus() == PlayerResult.PlayerStatus.SURVIVED) {
                    isWinner = true;
                }
                break;
            }
        }

        if (isWinner) {
            game.getMusicManager().playMusic("music_victory");
        } else {
            game.getMusicManager().playMusic("music_game_over");
        }
        // *** END OF MUSIC LOGIC ***

        Window resultsWindow = new Window("Game Over!", skin, "default");
        resultsWindow.setModal(true);
        resultsWindow.setMovable(false);
        resultsWindow.setSize(800, 600); // Set a fixed size

        Table rootContent = new Table();
        rootContent.pad(20);

        Label title = new Label("The Game Has Ended", skin, "title");
        title.setFontScale(1.8f);
        rootContent.add(title).colspan(3).center().padBottom(15).row();

        Label resultLabel = new Label("Final Outcome: " + record.getFinalGameResult(), skin);
        resultLabel.setColor(Color.GOLD);
        Label durationLabel = new Label("Duration: " + record.getGameDuration(), skin);
        rootContent.add(resultLabel).colspan(2).center().padBottom(20).row();
        rootContent.add(durationLabel).colspan(2).center().padBottom(20).row();

        Table scoreboard = new Table(skin);
        scoreboard.setBackground(skin.getDrawable("round-dark-gray"));
        scoreboard.add(new Label("Rank", skin)).pad(10).expandX();
        scoreboard.add(new Label("Player", skin)).pad(10).expandX();
        scoreboard.add(new Label("Score", skin)).pad(10).expandX();
        scoreboard.add(new Label("Status", skin)).pad(10).expandX().row();

        for (PlayerResult result : record.getPlayerResults()) {
            Label statusLabel = new Label(result.getStatus().toString(), skin);
            if (result.getStatus() == PlayerResult.PlayerStatus.WINNER) statusLabel.setColor(Color.GOLD);
            if (result.getStatus() == PlayerResult.PlayerStatus.SURVIVED) statusLabel.setColor(Color.GREEN);
            if (result.getStatus() == PlayerResult.PlayerStatus.DEFEATED) statusLabel.setColor(Color.FIREBRICK);

            scoreboard.add(new Label(String.valueOf(result.getFinalRank()), skin)).pad(8).center();
            scoreboard.add(new Label(result.getUsername(), skin)).pad(8).center();
            scoreboard.add(new Label(String.valueOf(result.getScore()), skin)).pad(8).center();
            scoreboard.add(statusLabel).pad(8).center().row();
        }

        ScrollPane scrollPane = new ScrollPane(scoreboard, skin);
        scrollPane.setFadeScrollBars(false);
        rootContent.add(scrollPane).colspan(3).expand().fill().padBottom(25).row();

        // دکمه خروج را اصلاح کنید:
        TextButton backButton = new TextButton("Return to Main Menu", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resultsWindow.remove();

                // *** تغییر مهم: فراخوانی متد پاک‌سازی کامل ***
                mediator.finishGameForUser();
            }
        });
        rootContent.add(backButton).colspan(3).center().width(300).height(50);

        resultsWindow.add(rootContent).expand().fill();
        resultsWindow.setPosition(
            (stage.getWidth() - resultsWindow.getWidth()) / 2f,
            (stage.getHeight() - resultsWindow.getHeight()) / 2f
        );
        stage.addActor(resultsWindow);
    }



    private void openHelpPanel() {
        Table content = new Table(skin);
        content.pad(15);
        content.defaults().pad(8);

        int dragonGlassCount = mediator.getCurrentDragonGlass();
        Label titleLabel = new Label("Your Dragon Glass: " + dragonGlassCount, skin, "title");
        titleLabel.setColor(Color.CYAN); // رنگ متن را برای زیبایی بیشتر تغییر می‌دهیم
        content.add(titleLabel).colspan(2).padBottom(20).row();content.add(new Label("Enter the amount of resources you want to contribute:", skin)).colspan(2).row();

        final java.util.Map<ResourceType, TextField> inputFields = new java.util.HashMap<>();
        String[] resourceTypes = {"Wood", "Stone", "Food", "Coin"};
        for (String typeStr : resourceTypes) {
            ResourceType type = ResourceType.valueOf(typeStr);
            TextField input = new TextField("0", skin);
            input.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
            inputFields.put(type, input);

            content.add(new Label(typeStr + ":", skin)).left();
            content.setBackground(skin.newDrawable("white", new Color(0.6f, 0.6f, 0.6f, 1f)));
            content.add(input).width(200).left().row();
        }

        TextButton confirmButton = new TextButton("Confirm Contribution", skin);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int wood = Integer.parseInt(inputFields.get(ResourceType.Wood).getText());
                    int stone = Integer.parseInt(inputFields.get(ResourceType.Stone).getText());
                    int food = Integer.parseInt(inputFields.get(ResourceType.Food).getText());
                    int coin = Integer.parseInt(inputFields.get(ResourceType.Coin).getText());

                    Result result = mediator.helpTheWall(wood, stone, coin, food);

                    if (result.isSuccess()) {
                        addSystem(result.getMessage());
                        if (activeDialog != null) {
                            activeDialog.remove();
                            activeDialog = null;
                        }
                    } else {
                        addError(result.getMessage());
                    }

                } catch (NumberFormatException e) {
                    addError("ERROR: Please enter valid numbers for all resources.");
                }
            }
        });

        content.add(confirmButton).colspan(2).width(300).padTop(20).row();
        openWindow("Help The Wall", content);
    }


    public void triggerResultsPanelDisplay() {
        if (currentUser != null && currentGame != null) {
            Result loadResult = UserSystem.loadGameRecord(currentUser, currentGame.getName());
            if (loadResult.isSuccess()) {
                GameRecord record = (GameRecord) loadResult.getGameInstance();
                showGameResultsPanel(record);
            } else {
                addError("Error: Could not load final game report.");
                mediator.goToGameJoin();
            }
        }
    }


    private GameUser getFreshCurrentUser() {
        if (game == null || mediator.getCurrentGame() == null || currentUser == null) {
            return this.currentUser; // Fallback to the initial user if something is wrong
        }
        // This is the CRITICAL line: Always get the user object from the mediator's game instance
        return mediator.getCurrentGame().getUser(this.currentUser.getId());
    }
    private void showMessageDetail(String m) {
        Dialog d = new Dialog("Message", skin);
        d.text(m);
        d.button("Close"); d.show(stage);
    }


//    private void addPortIconsToStage() {
//        if (currentGame == null || currentGame.getUsers() == null) {
//            return;
//        }
//
//        for (GameUser user : currentGame.getUsers()) {
//            if (user == null || user.getKingdom() == null) {
//                continue;
//            }
//
//            Kingdom kingdom = user.getKingdom();
//            Port port = kingdom.getPort();
//
//            if (port != null) {
//                Image portImage;
//                if (!port.isDestroyed()) {
//                    portImage = new Image(GameFileUtility.getBuildingTexture("Port", 1));
//                } else {
//                    portImage = new Image(GameFileUtility.getBuildingTexture("Port", 0));
//                }
//
//                float x = 0;
//                float y = 0;
//
//                switch (kingdom.getType()) {
//                    case EYRIE:
//                        x = 1450; y = 600;
//                        break;
//                    default :
//                        x = 1450; y = 600;
//                        break;
//                }
//
//                portImage.setPosition(x, y);
//                portImage.setSize(80, 80);
//                stage.addActor(portImage);
//            }
//        }
//    }
private void addPortIconsToStage() {
    if (currentGame == null || currentGame.getUsers() == null) {
        return;
    }

    for (GameUser user : currentGame.getUsers()) {
        if (user == null || user.getKingdom() == null) {
            continue;
        }

        Kingdom kingdom = user.getKingdom();
        Port port = kingdom.getPort();

        if (port != null) {
            Image portImage;

            // --- اصلاح شده: استفاده از متد امن کش ---
            // قبلاً: GameFileUtility.getBuildingTexture(...) که اگر فایل نبود کرش می‌کرد
            int level = port.isDestroyed() ? 0 : 1;
            portImage = new Image(getCachedBuildingDrawable("Port", level));
            // ----------------------------------------

            float x = 1450;
            float y = 600;

            // تنظیم مختصات بر اساس نوع پادشاهی (فعلاً دیفالت شما)
            switch (kingdom.getType()) {
                case EYRIE:
                    x = 1450; y = 600;
                    break;
                default :
                    x = 1450; y = 600;
                    break;
            }

            portImage.setPosition(x, y);
            portImage.setSize(80, 80);
            stage.addActor(portImage);
        }
    }
}
    // در فایل GamePlayScreen.java
    // در فایل GamePlayScreen.java

//    private void openBattleReplayPanel(final GameMediator.BattleHistoryInfo historyInfo) {
//        if (historyInfo == null || historyInfo.replayFilePath == null || historyInfo.replayFilePath.equals("N/A")) {
//            addError("Could not load battle replay file.");
//            return;
//        }
//
//        // ❌ قبلی (اشتباه)
//// final MyLinkedList<BattleEvent> loadedEvents = mediator.loadReplayLog(historyInfo.replayFilePath);
//// final java.util.List<BattleEvent> events = new ArrayList<>();
//// for (BattleEvent ev : loadedEvents) {
////     events.add(ev);
//// }
//
//// ✅ جدید (درست و هماهنگ با ReplayData)
//        ReplayData replayData = mediator.loadReplayLog(historyInfo.replayFilePath);
//
//        if (replayData == null || replayData.getEvents() == null || replayData.getEvents().isEmpty()) {
//            addError("Replay data is empty or corrupted. The battle may have occurred before the replay system was fixed.");
//            return;
//        }
//
//        final MyLinkedList<BattleEvent> events = replayData.getEvents();
//        System.out.println("UI DEBUG: events to play = " + events.size());
//
//        final String attackerName = (replayData.getAttackerName() != null ? replayData.getAttackerName() : "Attacker");
//        final String defenderName = (replayData.getDefenderName() != null ? replayData.getDefenderName() : "Defender");
//
//        if (events.isEmpty()) {
//            addError("Replay data is empty or corrupted. The battle may have occurred before the replay system was fixed.");
//            return;
//        }
//
//        System.out.println("UI DEBUG: events to play = " + events.size());
//
//        final Dialog panel = new Dialog("Battle Replay", skin, "default");
//        panel.setModal(true);
//        panel.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.6f);
//
//        Table content = new Table();
//        content.pad(20);
//
//
//        final Label attackerNameLabel = new Label(attackerName, skin);
//        final Label defenderNameLabel = new Label(defenderName, skin);
//
//        ProgressBar.ProgressBarStyle progressBarStyle = skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class);
//        final ProgressBar attackerHealthBar = new ProgressBar(0, 1000, 1, false, progressBarStyle);
//        attackerHealthBar.setValue(1000);
//        final ProgressBar defenderHealthBar = new ProgressBar(0, 1000, 1, false, progressBarStyle);
//        defenderHealthBar.setValue(1000);
//
//        final Table logTable = new Table();
//        logTable.top().left();
//        final ScrollPane logScroll = new ScrollPane(logTable, skin);
//
//        Table vsTable = new Table();
//        vsTable.add(attackerNameLabel);
//        vsTable.add(new Label(" VS ", skin, "title")).pad(0, 20, 0, 20);
//        vsTable.add(defenderNameLabel);
//        vsTable.row();
//        vsTable.add(attackerHealthBar).width(200);
//        vsTable.add();
//        vsTable.add(defenderHealthBar).width(200);
//
//        content.add(vsTable).padBottom(20).row();
//        content.add(logScroll).expand().fill().minHeight(200);
//        panel.getContentTable().add(content).expand().fill();
//
//        final boolean[] isPaused = {false};
//        final float[] playSpeed = {1.0f};
//        final float baseDelay = 0.5f;
//        final int[] currentIndex = {0};
//        final float[] timeToNextEvent = {baseDelay};
//
//        Action replayDriver = new Action() {
//            @Override
//            public boolean act(float delta) {
//                if (isPaused[0]) {
//                    return false;
//                }
//
//                timeToNextEvent[0] -= delta * playSpeed[0];
//
//                while (timeToNextEvent[0] <= 0f) {
//                    if (currentIndex[0] >= events.size()) {
//                        Label finishedLabel = new Label("--- REPLAY FINISHED ---", skin, "title");
//                        finishedLabel.setColor(Color.GOLD);
//                        logTable.add(finishedLabel).left().row();
//                        logScroll.layout();
//                        logScroll.setScrollPercentY(1.0f);
//                        System.out.println("UI DEBUG: Replay finished.");
//                        return true;
//                    }
//
//                    BattleEvent event = events.get(currentIndex[0]++);
//                    System.out.println("UI DEBUG: Showing event " + currentIndex[0] + "/" + events.size() + " → " + event.getClass().getSimpleName());
//
//                    if (event instanceof EventTextLog) {
//                        logTable.add(new Label(((EventTextLog) event).message, skin)).left().row();
//                    } else if (event instanceof EventRoundUpdate) {
//                        EventRoundUpdate round = (EventRoundUpdate) event;
//                        logTable.add(new Label(round.message, skin)).left().row();
//                        attackerHealthBar.setValue(round.attackerTotalHealth);
//                        defenderHealthBar.setValue(round.defenderTotalHealth);
//                    } else if (event instanceof EventHeroDuel) {
//                        EventHeroDuel duel = (EventHeroDuel) event;
//                        String winner = duel.attackerWon ? duel.attackerHero : duel.defenderHero;
//                        Label duelLabel = new Label("HERO DUEL! " + duel.attackerHero + " vs " + duel.defenderHero, skin);
//                        duelLabel.setColor(Color.CYAN);
//                        logTable.add(duelLabel).left().row();
//                        logTable.add(new Label("Winner: " + winner, skin, "title")).left().row();
//                    } else {
//                        logTable.add(new Label(event.toString(), skin)).left().row();
//                    }
//
//                    logScroll.layout();
//                    logScroll.setScrollPercentY(1.0f);
//                    timeToNextEvent[0] += baseDelay;
//                }
//
//                return false;
//            }
//        };
//
//        final TextButton pauseButton = new TextButton("Pause", skin);
//        pauseButton.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                isPaused[0] = !isPaused[0];
//                pauseButton.setText(isPaused[0] ? "Resume" : "Pause");
//            }
//        });
//
//        final TextButton speed1xButton = new TextButton("1x Speed", skin);
//        speed1xButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { playSpeed[0] = 1.0f; } });
//
//        final TextButton speed2xButton = new TextButton("2x Speed", skin);
//        speed2xButton.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { playSpeed[0] = 2.0f; } });
//
//        // ❌ قبلاً اینجا بود: panel.addAction(replayDriver);
//        // ✅ حالا میاریمش روی stage
//        stage.addAction(replayDriver);
//
//        panel.getButtonTable().add(pauseButton).pad(10);
//        panel.getButtonTable().add(speed1xButton).pad(10);
//        panel.getButtonTable().add(speed2xButton).pad(10);
//        panel.button("Close", true);
//
//        panel.show(stage);
//    }

//private void openBattleReplayPanel(final GameMediator.BattleHistoryInfo historyInfo) {
//    if (historyInfo == null || historyInfo.replayFilePath == null || historyInfo.replayFilePath.equals("N/A")) {
//        addError("Replay file not found.");
//        return;
//    }
//
//    ReplayData replayData = mediator.loadReplayLog(historyInfo.replayFilePath);
//    if (replayData == null || replayData.getEvents() == null || replayData.getEvents().isEmpty()) {
//        addError("Replay data is corrupted or empty.");
//        return;
//    }
//
//    final MyLinkedList<BattleEvent> events = replayData.getEvents();
//    final String attackerName = (replayData.getAttackerName() != null ? replayData.getAttackerName() : "Attacker");
//    final String defenderName = (replayData.getDefenderName() != null ? replayData.getDefenderName() : "Defender");
//
//    final Dialog panel = new Dialog("Battle Replay", skin, "default");
//    panel.setModal(true);
//    panel.setSize(Gdx.graphics.getWidth() * 0.75f, Gdx.graphics.getHeight() * 0.75f);
//
//    // --- بخش بالای صفحه ---
//    Table topTable = new Table();
//    topTable.pad(10);
//
//    Label lblAtk = new Label("[ATK] " + attackerName, skin); lblAtk.setColor(Color.ORANGE);
//    Label lblDef = new Label("[DEF] " + defenderName, skin); lblDef.setColor(Color.CYAN);
//
//    ProgressBar.ProgressBarStyle barStyle = skin.get("default-horizontal", ProgressBar.ProgressBarStyle.class);
//    barStyle.background.setMinHeight(15);
//    barStyle.knobBefore.setMinHeight(15);
//
//    final ProgressBar attackerHealthBar = new ProgressBar(0, 1000, 1, false, barStyle);
//    attackerHealthBar.setValue(1000);
//    final ProgressBar defenderHealthBar = new ProgressBar(0, 1000, 1, false, barStyle);
//    defenderHealthBar.setValue(1000);
//
//    topTable.add(lblAtk).left();
//    topTable.add().expandX();
//    topTable.add(lblDef).right().row();
//
//    topTable.add(attackerHealthBar).width(280).left();
//    topTable.add(new Label(" VS ", skin)).pad(10);
//    topTable.add(defenderHealthBar).width(280).right();
//
//    // --- بخش لاگ‌ها ---
//    final Table logTable = new Table();
//    logTable.top().left();
//    final ScrollPane logScroll = new ScrollPane(logTable, skin);
//    logScroll.setFadeScrollBars(false);
//
//    panel.getContentTable().add(topTable).growX().padBottom(10).row();
//    panel.getContentTable().add(logScroll).expand().fill().row();
//
//    // --- کنترلر پخش ---
//    final boolean[] isPaused = {false};
//    final float[] playSpeed = {1.0f};
//    final float baseDelay = 0.8f;
//    final int[] currentIndex = {0};
//    final float[] timeToNextEvent = {0.5f};
//
//    Action replayDriver = new Action() {
//        @Override
//        public boolean act(float delta) {
//            if (isPaused[0]) return false;
//
//            timeToNextEvent[0] -= delta * playSpeed[0];
//            if (timeToNextEvent[0] <= 0f) {
//                if (currentIndex[0] >= events.size()) {
//                    // --- اصلاح شده: رفع ارور color() ---
//                    Table endRow = new Table(skin);
//                    endRow.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
//
//                    Label endLabel = new Label("--- END OF BATTLE ---", skin);
//                    endLabel.setColor(Color.GOLD);
//
//                    endRow.add(endLabel).pad(10);
//                    // -----------------------------------
//
//                    logTable.add(endRow).growX().padTop(20).row();
//                    logScroll.layout();
//                    logScroll.setScrollPercentY(1.0f);
//                    return true;
//                }
//
//                BattleEvent event = events.get(currentIndex[0]++);
//                addEventToLogTable(logTable, event, skin, attackerHealthBar, defenderHealthBar);
//
//                logScroll.layout();
//                logScroll.setScrollPercentY(1.0f);
//                timeToNextEvent[0] = baseDelay;
//            }
//            return false;
//        }
//    };
//    stage.addAction(replayDriver);
//
//    // دکمه‌های کنترل
//    Table controls = new Table();
//    TextButton btnPause = new TextButton("Pause", skin);
//    btnPause.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { isPaused[0] = !isPaused[0]; btnPause.setText(isPaused[0] ? "Resume" : "Pause"); } });
//
//    TextButton btnSpeed = new TextButton("Speed: 1x", skin);
//    btnSpeed.addListener(new ChangeListener() {
//        @Override public void changed(ChangeEvent e, Actor a) {
//            if(playSpeed[0] == 1f) { playSpeed[0]=2f; btnSpeed.setText("Speed: 2x"); }
//            else if(playSpeed[0] == 2f) { playSpeed[0]=4f; btnSpeed.setText("Speed: 4x"); }
//            else { playSpeed[0]=1f; btnSpeed.setText("Speed: 1x"); }
//        }
//    });
//
//    controls.add(btnPause).width(100).pad(5);
//    controls.add(btnSpeed).width(100).pad(5);
//    panel.getButtonTable().add(controls);
//    panel.button("Close", true);
//    panel.show(stage);
//}

private void openBattleReplayPanel(final GameMediator.BattleHistoryInfo historyInfo) {
    if (historyInfo == null || historyInfo.replayFilePath == null || historyInfo.replayFilePath.equals("N/A")) {
        addError("Replay file not found.");
        return;
    }

    ReplayData replayData = mediator.loadReplayLog(historyInfo.replayFilePath);
    if (replayData == null || replayData.getEvents() == null || replayData.getEvents().isEmpty()) {
        addError("Replay data is corrupted or empty.");
        return;
    }

    final MyLinkedList<BattleEvent> events = replayData.getEvents();
    final String attackerName = (replayData.getAttackerName() != null ? replayData.getAttackerName() : "Attacker");
    final String defenderName = (replayData.getDefenderName() != null ? replayData.getDefenderName() : "Defender");

    final Dialog panel = new Dialog("Battle Replay", skin, "default");
    panel.setModal(true);
    // --- تغییر ۱: افزایش سایز کلی پنل ---
    panel.setSize(950, 750);
    panel.setPosition((stage.getWidth() - 950) / 2f, (stage.getHeight() - 750) / 2f);

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

    panel.getContentTable().add(logContainer).expand().fill().row();

    // --- بخش ۳: کنترلر پخش ---
    final boolean[] isPaused = {false};
    final float[] playSpeed = {1.0f};
    final float baseDelay = 0.8f;
    final int[] currentIndex = {0};
    final float[] timeToNextEvent = {0.5f};

    Action replayDriver = new Action() {
        @Override
        public boolean act(float delta) {
            if (isPaused[0]) return false;

            timeToNextEvent[0] -= delta * playSpeed[0];
            if (timeToNextEvent[0] <= 0f) {
                if (currentIndex[0] >= events.size()) {
                    // پایان
                    Table endRow = new Table(skin);
                    endRow.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f)));
                    Label endLabel = new Label("--- END OF BATTLE ---", skin);
                    endLabel.setColor(Color.GOLD);
                    endLabel.setFontScale(1.2f);
                    endRow.add(endLabel).pad(15);

                    logTable.add(endRow).growX().padTop(20).row();
                    logScroll.layout();
                    logScroll.setScrollPercentY(1.0f);
                    return true;
                }

                BattleEvent event = events.get(currentIndex[0]++);
                addEventToLogTable(logTable, event, skin, attackerHealthBar, defenderHealthBar);

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
            else { playSpeed[0]=1f; btnSpeed.setText("Speed: 1x"); }
        }
    });

    controls.add(btnPause).width(150).height(50).pad(10);
    controls.add(btnSpeed).width(150).height(50).pad(10);

    panel.getButtonTable().add(controls).row();
    TextButton closeBtn = new TextButton("Close", skin);
    closeBtn.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
            panel.remove();
        }
    });
    panel.getButtonTable().add(closeBtn).width(200).height(50).padTop(10);

    panel.show(stage);
}

    // این متد جدید را به GamePlayScreen.java اضافه کنید
    private void openWorldMapPanel() {
        final Dialog mapDialog = new Dialog("World Map", skin, "default");
        mapDialog.setModal(true);
        mapDialog.pad(20);

        // استایل برای نمایش اطلاعات وقتی موس روی دکمه‌ها می‌رود (Tooltip)
        TextTooltip.TextTooltipStyle tooltipStyle = new TextTooltip.TextTooltipStyle(skin.get(TextTooltip.TextTooltipStyle.class));
        tooltipStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f));

        Stack mapStack = new Stack();
        mapStack.add(new Image(mapVisorTexture));
        Table buttonLayer = new Table();
        buttonLayer.setFillParent(true);

        GameUser[] allUsers = currentGame.getUsers();
        if (allUsers != null) {
            for (GameUser user : allUsers) {
                // فقط کاربرانی را نمایش بده که پادشاهی فعال دارند
                if (user == null || user.getKingdom() == null) continue;

                Kingdom kingdom = user.getKingdom();
                TextButton kingdomButton = new TextButton(kingdom.getType().name(), skin);
                kingdomButton.getLabel().setFontScale(0.8f);

                // ساخت متن راهنما با اطلاعات عمومی پادشاهی
                String tooltipText = String.format(
                    "Ruler: %s\nKingdom: %s\nCastle Lvl: %d",
                    user.getUsername(),
                    kingdom.getType().name(),
                    kingdom.getCastle().getLevel()
                );
                TextTooltip tooltip = new TextTooltip(tooltipText, TooltipManager.getInstance(), tooltipStyle);
                kingdomButton.addListener(tooltip);

                // دکمه‌ها غیرفعال هستند چون فقط برای نمایش اطلاعات استفاده می‌شوند
                kingdomButton.setDisabled(true);
                kingdomButton.getLabel().setColor(Color.WHITE); // رنگ متن را سفید نگه می‌داریم تا خوانا باشد

                // قرار دادن دکمه در جای صحیح روی نقشه
                positionKingdomButton(buttonLayer, kingdomButton, kingdom.getType());
            }
        }

        mapStack.add(buttonLayer);
        mapDialog.getContentTable().add(mapStack).size(600, 800);
        mapDialog.button("Close", true);
        mapDialog.show(stage);
    }    private Texture createPlaceholderTexture(int w, int h) {
        Pixmap p = new Pixmap(w,h, Pixmap.Format.RGBA8888);
        p.setColor(0.85f,0.85f,0.85f,1f);
        p.fill();
        p.setColor(0.6f,0.6f,0.6f,1f);
        p.drawRectangle(1,1,w-2,h-2);
        Texture t = new Texture(p); p.dispose(); return t;
    }

    private void openExtraPanel(){ openCenteredWindow("Extra", new Label("Not implemented", skin)); }

    private void openCenteredWindowWithFixedHeight(String title, Actor content, int height) {
        // reuse openCenteredWindow behavior but set fixed height
        if (activeDialog != null && activeDialog.getStage() != null) { activeDialog.remove(); activeDialog=null; }
        Window win = new Window(title, skin);
        win.setModal(true); win.setMovable(false);
        win.padTop(40);
        win.getTitleTable().defaults().height(30).width(30);
        win.add(content).width(900).height(height).row();
        TextButton close = new TextButton("X", skin);
        close.addListener(new ChangeListener(){ @Override public void changed(ChangeEvent event, Actor actor){ win.remove(); activeDialog=null; }});
        win.getTitleTable().add(close).padRight(5);
        win.pack();
        win.setPosition((Gdx.graphics.getWidth()-win.getWidth())/2f,(Gdx.graphics.getHeight()-win.getHeight())/2f);
        stage.addActor(win); activeDialog = win;
    }

// In file: GamePlayScreen.java

//    private void renderTilesAndUpdate() {
//        gridTable.clear();
//        gridTable.defaults().pad(2);
//        int tilePx = Math.min(53, Math.round((Gdx.graphics.getWidth() - 600f) / GRID_SIZE));
//
//        // *** START OF FIX ***
//        // به جای استفاده از آبجکت قدیمی 'this.currentGame'،
//        // همیشه آخرین نسخه از آبجکت بازی را مستقیماً از مدیاTور دریافت می‌کنیم.
//        GameOfThrones freshGameData = mediator.getCurrentGame();
//        if (freshGameData == null) {
//            buildEmptyGrid(tilePx, null, null);
//            return;
//        }
//        GameUser userInGame = freshGameData.getUser(currentUser.getId());
//        // *** پایان بخش کلیدی ***
//        // *** END OF FIX ***
//
//        if (userInGame == null || userInGame.getKingdom() == null || userInGame.getKingdom().getUserMap() == null) {
//            buildEmptyGrid(tilePx, null, null);
//            return;
//        }
//        int mapRows = userInGame.getKingdom().getUserMap().getLength();
//        int mapCols = userInGame.getKingdom().getUserMap().getWidth();
//
//        int offsetRow = (mapRows - GRID_SIZE) / 2;
//        int offsetCol = (mapCols - GRID_SIZE) / 2;
//
//        for (int r = 0; r < GRID_SIZE; r++) {
//            for (int c = 0; c < GRID_SIZE; c++) {
//                final int mapR = r + offsetRow;
//                final int mapC = c + offsetCol;
//                final int rx = r, cx = c;
//                // ... داخل حلقه‌های for ...
//                Building building = null;
//                try {
//                    building = userInGame.getKingdom().getUserMap().getBuildingAt(mapR, mapC);
//                } catch (Exception ignored) {}
//
//                TextureRegionDrawable baseDrawable;
//
//                // --- تغییر اصلی اینجاست: استفاده از کش ---
//                if (building != null) {
//                    // به جای ساختن new Texture، از کش می‌خوانیم
//                    baseDrawable = getCachedBuildingDrawable(building.getName(), Math.max(0, building.getLevel() - 1));
//                } else {
//                    baseDrawable = new TextureRegionDrawable(new TextureRegion(tileTexture));
//                }
//                // ----------------------------------------
//
//                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
//                style.imageUp = baseDrawable;
//                // برای جلوگیری از ساخت رنگ جدید در هر فریم، از رنگ‌های ثابت استفاده کنید یا همین‌طور بگذارید (چون Color سبک است)
//                style.imageOver = baseDrawable.tint(new Color(0.9f, 0.9f, 0.9f, 1f));
//                style.imageDown = baseDrawable.tint(new Color(0.6f, 0.6f, 0.6f, 1f));
//
////                final ImageButton btn = new ImageButton(style);
////                btn.setSize(tilePx, tilePx);
////                btn.addListener(new ChangeListener() {
////                    @Override
////                    public void changed(ChangeEvent event, Actor actor) {
////                        toggleTileSelection(rx, cx);
////                    }
////                });
//
//                final ImageButton btn = new ImageButton(style);
//                btn.setSize(tilePx, tilePx);
//
//                // تنظیم نقطه ثقل دکمه به وسط (برای اینکه از مرکز بزرگ بشه نه از گوشه)
//                btn.setTransform(true);
//                btn.setOrigin(Align.center);
//
//                // --- تغییر ۱: افزایش سایز پیش‌فرض ---
//                // این عدد تعیین می‌کند ساختمان‌ها در حالت عادی چقدر بزرگ باشند
//                // 1.15f یعنی ۱۵ درصد بزرگتر از کادر خودشان (کمی بیرون‌زدگی جذاب)
//                final float baseScale = 1.15f;
//                btn.setScale(baseScale);
//
//                // --- تغییر ۲: پر کردن فضای خالی ---
//                // این دستورات باعث می‌شود عکس دقیقا تا لبه‌های دکمه کشیده شود
//                btn.getImageCell().expand().fill();
//                btn.getImage().setScaling(Scaling.fit);
//
//                // اضافه کردن انیمیشن هاور (Hover)
//                btn.addListener(new ClickListener() {
//                    @Override
//                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                        if (pointer == -1) { // فقط حرکت موس
//                            btn.addAction(Actions.scaleTo(1.3f, 1.3f, 0.1f)); // بزرگ شدن در 0.1 ثانیه
//                        }
//                    }
//
//                    @Override
//                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
//                        if (pointer == -1) {
//                            // --- تغییر ۳: برگشت به سایز بیس (نه 1.0) ---
//                            // وقتی موس رفت کنار، برگرده به همون 1.15
//                            btn.addAction(Actions.scaleTo(baseScale, baseScale, 0.1f));
//                        }
//                    }
//                });
//
//                btn.addListener(new ChangeListener() {
//                    @Override
//                    public void changed(ChangeEvent event, Actor actor) {
//                        toggleTileSelection(rx, cx);
//                    }
//                });
//
//// --- شروع کد نوار سلامتی جدید ---
//                Stack cellStack = new Stack();
//                cellStack.add(btn); // لایه اول: دکمه ساختمان
//
//
//                // ... (کد ساخت دکمه و Stack مثل قبل) ...
//
//                // شرط نمایش نوار سلامتی
//                if (building != null && !building.isDestroyed()) {
//
//                    // --- اصلاح فرمول محاسبه جان ---
//                    // قبلا 500f بود که اشتباه بود. الان روی 100f تنظیم شد تا با جان واقعی یکی شود.
//                    // اگر فرمول دقیق‌تری در GameBalance دارید (مثلاً برای دیوارها)، بهتر است از آنجا بخوانید.
//                    float baseHp = 100f;
//                    float maxHp = baseHp * building.getLevel(); // فرض: هر لول ۱۰۰ تا جان اضافه می‌کند
//
//                    if (maxHp <= 0) maxHp = 100;
//
//                    float currentHp = building.getHealth();
//                    float hpPercent = Math.min(1.0f, Math.max(0.0f, currentHp / maxHp));
//                    // -----------------------------
//
//                    ProgressBar.ProgressBarStyle progressStyle = new ProgressBar.ProgressBarStyle();
//                    progressStyle.background = barBackground;
//
//                    // --- تغییر ۱: کاهش ضخامت حاشیه مشکی ---
//                    // قبلاً ۱۲ بود، الان ۸ می‌گذاریم تا باریک شود
//                    progressStyle.background.setMinHeight(6);
//
//                    // انتخاب رنگ
//                    if (hpPercent > 0.6f) {
//                        progressStyle.knobBefore = barGreen;
//                    } else if (hpPercent > 0.3f) {
//                        progressStyle.knobBefore = barOrange;
//                    } else {
//                        progressStyle.knobBefore = barRed;
//                    }
//
//                    // --- تغییر ۲: کاهش ضخامت نوار رنگی ---
//                    // قبلاً ۸ بود، الان ۶ می‌گذاریم (باید کمی کمتر از عدد بالایی باشد تا حاشیه دیده شود)
//                    progressStyle.knobBefore.setMinHeight(4);
//
//                    // ۲. ساخت نوار
//                    ProgressBar hpBar = new ProgressBar(0, 1, 0.01f, false, progressStyle);
//                    hpBar.setValue(hpPercent);
//
//                    // ۳. کانتینر
//                    Table barTable = new Table();
//                    barTable.top();
//                    // ارتفاع سلول جدول را هم تنظیم می‌کنیم تا جای زیادی نگیرد
//                    barTable.add(hpBar).width(tilePx * 0.9f).padTop(2f);
//
//                    cellStack.add(barTable);
//                }
//                // ... (ادامه کدها: اضافه کردن به gridTable) ...
//// --- پایان کد نوار سلامتی ---
//
//                tileButtons[r][c] = btn;
//                tileSelected[r][c] = false;
//
//// اضافه کردن Stack به جای دکمه خالی
//                gridTable.add(cellStack).size(tilePx, tilePx);
//            }
//            gridTable.row();
//        }
//        gridTable.setPosition(955, Gdx.graphics.getHeight() - 550);
//        if (gridTable.getStage() == null) stage.addActor(gridTable);
//    }

private void renderTilesAndUpdate() {
    gridTable.clear();
    gridTable.defaults().pad(2);

    int tilePx = Math.min(53, Math.round((Gdx.graphics.getWidth() - 600f) / GRID_SIZE));

    GameOfThrones freshGameData = mediator.getCurrentGame();
    if (freshGameData == null) {
        buildEmptyGrid(tilePx, null, null);
        return;
    }
    GameUser userInGame = freshGameData.getUser(currentUser.getId());

    if (userInGame == null || userInGame.getKingdom() == null || userInGame.getKingdom().getUserMap() == null) {
        buildEmptyGrid(tilePx, null, null);
        return;
    }

    int mapRows = userInGame.getKingdom().getUserMap().getLength();
    int mapCols = userInGame.getKingdom().getUserMap().getWidth();

    int offsetRow = (mapRows - GRID_SIZE) / 2;
    int offsetCol = (mapCols - GRID_SIZE) / 2;

    for (int r = 0; r < GRID_SIZE; r++) {
        for (int c = 0; c < GRID_SIZE; c++) {
            final int mapR = r + offsetRow;
            final int mapC = c + offsetCol;
            final int rx = r, cx = c;

            Building building = null;
            try {
                building = userInGame.getKingdom().getUserMap().getBuildingAt(mapR, mapC);
            } catch (Exception ignored) {}

            // دریافت عکس (امن و کش شده)
            TextureRegionDrawable baseDrawable;
            if (building != null) {
                baseDrawable = getCachedBuildingDrawable(building.getName(), Math.max(0, building.getLevel() - 1));
            } else {
                baseDrawable = new TextureRegionDrawable(new TextureRegion(tileTexture));
            }

            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            style.imageUp = baseDrawable;
            style.imageOver = baseDrawable.tint(new Color(0.9f, 0.9f, 0.9f, 1f));
            style.imageDown = baseDrawable.tint(new Color(0.6f, 0.6f, 0.6f, 1f));

            final ImageButton btn = new ImageButton(style);
            btn.setSize(tilePx, tilePx);
            btn.setTransform(true);
            btn.setOrigin(Align.center);
            final float baseScale = 1.15f;
            btn.setScale(baseScale);
            btn.getImageCell().expand().fill();
            btn.getImage().setScaling(Scaling.fit);

            // --- تعریف جدول نوار سلامتی (قبل از لیسنر تا داخل لیسنر دسترسی داشته باشیم) ---
            final Table barTable = new Table();
            barTable.top();
            barTable.setVisible(false); // <--- پیش‌فرض مخفی باشد

            // انیمیشن هاور (موس) + نمایش نوار سلامتی
            btn.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer == -1) {
                        btn.addAction(Actions.scaleTo(1.3f, 1.3f, 0.1f));
                        // وقتی موس آمد، نوار را نشان بده
                        barTable.setVisible(true);
                    }
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer == -1) {
                        btn.addAction(Actions.scaleTo(baseScale, baseScale, 0.1f));
                        // وقتی موس رفت، نوار را مخفی کن
                        barTable.setVisible(false);
                    }
                }
            });

            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    toggleTileSelection(rx, cx);
                }
            });

            tileButtons[r][c] = btn;
            tileSelected[r][c] = false;

            // --- ساختار Stack ---
            Stack cellStack = new Stack();
            cellStack.add(btn); // لایه اول: دکمه

            if (building != null && !building.isDestroyed()) {
                // محاسبه درصد جان
                float baseHp = 100f;
                float maxHp = baseHp * building.getLevel();

                // --- اصلاح لاجیک محاسبه جان (برای پر نشان دادن بار) ---
                // اگر جان فعلی کمتر از مکس است اما ساختمان سالم است (stable)، یعنی تازه ارتقا یافته
                // و ما برای زیبایی گرافیکی آن را پر نشان می‌دهیم (یا باید در دیتابیس جان را پر کنید)
                float currentHp = building.getHealth();

                // اگر ساختمان سالم است و در حال تعمیر نیست، درصد را بر اساس لول فعلی درست کن
                if (building.getStatus() == Status.stable && currentHp < maxHp && currentHp > 0) {
                    // نکته: این فقط نمایش است، عدد واقعی در دیتابیس تغییر نمی‌کند
                    // اگر می‌خواهید دقیق باشد، باید currentHp واقعی را نشان دهید که احتمالا پر نشده است
                }

                float hpPercent = Math.min(1.0f, Math.max(0.0f, currentHp / maxHp));

                // ساخت نوار
                ProgressBar.ProgressBarStyle progressStyle = new ProgressBar.ProgressBarStyle();
                progressStyle.background = barBackground;
                progressStyle.background.setMinHeight(8);

                if (hpPercent > 0.6f) {
                    progressStyle.knobBefore = barGreen;
                } else if (hpPercent > 0.3f) {
                    progressStyle.knobBefore = barOrange;
                } else {
                    progressStyle.knobBefore = barRed;
                }
                progressStyle.knobBefore.setMinHeight(6);

                ProgressBar hpBar = new ProgressBar(0, 1, 0.01f, false, progressStyle);
                hpBar.setValue(hpPercent);

                // اضافه کردن نوار به جدولی که مخفی کرده بودیم
                barTable.add(hpBar).width(tilePx * 0.9f).padTop(2f);

                // اضافه کردن جدول نوار به استک (روی دکمه)
                cellStack.add(barTable);
            }

            gridTable.add(cellStack).size(tilePx, tilePx);
        }
        gridTable.row();
    }

    gridTable.setPosition(955, Gdx.graphics.getHeight() - 550);
    if (gridTable.getStage() == null) stage.addActor(gridTable);
}


    private void buildEmptyGrid(int tilePx, Drawable tileDrawableOver, Drawable tileDrawableDown) {
        gridTable.clear(); gridTable.defaults().pad(2); gridTable.setPosition(955, Gdx.graphics.getHeight() - 550);
        for (int r=0;r<GRID_SIZE;r++){
            for (int c=0;c<GRID_SIZE;c++){
                final int rx=r,cx=c;
                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
                style.imageUp = new Image(tileTexture).getDrawable(); style.imageOver = tileDrawableOver; style.imageDown = tileDrawableDown;
                ImageButton btn = new ImageButton(style); btn.setSize(tilePx,tilePx);
                btn.addListener(new ChangeListener(){ @Override public void changed(ChangeEvent event, Actor actor){ toggleTileSelection(rx,cx); } });
                tileButtons[r][c]=btn; tileSelected[r][c]=false; gridTable.add(btn).size(tilePx,tilePx);
            }
            gridTable.row();
        }
        if (gridTable.getStage()==null) stage.addActor(gridTable);
    }

// این متد را به طور کامل در GamePlayScreen.java جایگزین کنید

    private Table createMissionRow(GameMediator.MissionInfo info) {
        Table row = new Table();

        Stack progressBarStack = new Stack();
        Table barTable = new Table();

        // --- بخش اصلاح شده ---
        // به جای قلعه‌های ثابت، از عکس مخصوص هر پادشاهی استفاده می‌کنیم
        Actor startKingdomIcon = createPlaceholder(info.attackerKingdomName, 1);
        Actor endKingdomIcon = createPlaceholder(info.defenderKingdomName, 1);
        // --- پایان بخش اصلاح شده ---

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.TAN);
        pixmap.fill();
        Image pathImage = new Image(new Texture(pixmap));
        pixmap.dispose();

        float castleSize = 30f;
        float barWidth = 300f;
        barTable.add(startKingdomIcon).size(castleSize); // استفاده از آیکون جدید
        barTable.add(pathImage).height(5).width(barWidth);
        barTable.add(endKingdomIcon).size(castleSize);   // استفاده از آیکون جدید

        progressBarStack.add(barTable);

        Image armyIcon = new Image(GameFileUtility.getBuildingTexture("Archer",1));
        Container<Image> armyIconContainer = new Container<>(armyIcon);
        armyIconContainer.setActor(armyIcon);
        armyIconContainer.size(32, 32);

        float iconXPosition = castleSize + (barWidth * info.progressPercent) - (armyIconContainer.getWidth() / 2);
        armyIconContainer.padLeft(iconXPosition);
        armyIconContainer.left().bottom();

        progressBarStack.add(armyIconContainer);

        Table infoTable = new Table();
        infoTable.add(new Label("Target: " + info.defenderKingdomName, skin)).left().row();
        infoTable.add(new Label("ETA: " + info.remainingTimeFormatted, skin)).left().row();

        row.add(progressBarStack).width(barWidth + castleSize * 2).padRight(20);
        row.add(infoTable).left();

        return row;
    }
    public void addMessage(String msg) {
        messageLog.addFirst("MESSAGE::" + msg);
        refreshMessages();
    }
    public void addNotif(String msg) {
        messageLog.addFirst("NOTIF::" + msg);
        refreshMessages();
    }

    public void addSystem(String msg) {
        messageLog.addFirst("SYSTEM::" + msg);
        refreshMessages();
    }

    public void addError(String msg) {
        messageLog.addFirst("ERROR::" + msg);
        refreshMessages();
    }


    private void refreshMessages() {
        messagesTable.clear();

        for (String prefixedMsg : messageLog) {
            boolean shouldDisplay = false;
            String messageType = "NOTIF";
            String displayText = prefixedMsg;

            if (prefixedMsg.contains("::")) {
                String[] parts = prefixedMsg.split("::", 2);
                messageType = parts[0];
                displayText = parts.length > 1 ? parts[1] : "";
            }

            if (currentMessageFilter == null || messageType.equals(currentMessageFilter)) {
                shouldDisplay = true;
            }

            if (shouldDisplay) {
                Label l = new Label(displayText, skin);
                l.setWrap(true);

                switch (messageType) {
                    case "ERROR": l.setColor(0.8f,0,0,1); break;
                    case "SYSTEM": l.setColor(0,0.8f,0,1); break;
                    case "NOTIF":
                        l.setColor(Color.YELLOW);
                        break;
                    default: l.setColor(Color.WHITE); break;
                }
                messagesTable.add(l).left().expandX().fillX().padBottom(2).row();
            }
        }

        if (messagesScroll != null) {
            messagesScroll.layout();
            messagesScroll.setScrollPercentY(0f);
        }
    }


    private void refreshDynamicInfo() {
        lblUsername.setText("< " + safeUsername() + " >");
        lblKingdom.setText("Lord of " + safeKingdomName());

        String dayStr = mediator.getCurrentDay();
        String seasonStr = mediator.getCurrentSeason();
        if (lblDaySeason != null) {
            lblDaySeason.setText("Day: " + dayStr + "          Season: " + seasonStr);
        }

        if (lblInfantry != null) {
            lblInfantry.setText("Infantry: " + mediator.getSoldierCountByType(SoldierType.INFANTRY));
            lblArchers.setText("Archers: " + mediator.getSoldierCountByType(SoldierType.ARCHER));
            lblCavalry.setText("Cavalry: " + mediator.getSoldierCountByType(SoldierType.CAVALRY));
            lblSpies.setText("Spies: " + mediator.getSoldierCountByType(SoldierType.SPY));
            lblHero.setText("Hero: " + mediator.getCurrentUser().getKingdom().getHero().isAvailable());
            lblTotalSoldiers.setText(mediator.getTotalSoldierInfo());
        }

        if (lblWallStatus != null) {
            lblWallStatus.setText(mediator.getWallStatus());
        }

        lblWood.setText(mediator.getResourceInfoWood());
        lblStone.setText(mediator.getResourceInfoStone());
        lblFood.setText(mediator.getResourceInfoFood());
        lblCoin.setText(mediator.getResourceInfoCoin());
    }
    private void openCenteredWindow(String title, Actor content) {
        if (activeDialog != null && activeDialog.getStage() != null) { activeDialog.remove(); activeDialog = null; }
        Window win = new Window(title, skin);
        win.setModal(true); win.setMovable(false); win.padTop(40);
        win.getTitleTable().defaults().height(30).width(30);
        win.add(content).width(900).row();
        TextButton close = new TextButton("X", skin);
        close.addListener(new ChangeListener(){ @Override public void changed(ChangeEvent event, Actor actor){ win.remove(); activeDialog=null; }});
        win.getTitleTable().add(close).padRight(5);
        win.pack(); win.setPosition((Gdx.graphics.getWidth()-win.getWidth())/2f,(Gdx.graphics.getHeight()-win.getHeight())/2f);
        stage.addActor(win); activeDialog = win;
    }


    private void populateBuildingList(Table listTable) {
        listTable.clear();
        MyLinkedList<BuildingManager.ConstructionOption> options = mediator.getConstructionOptions();

        if (options.isEmpty()) {
            listTable.add(new Label("No buildings available for construction.", skin));
            return;
        }

        for (BuildingManager.ConstructionOption option : options) {
            Table itemTable = new Table();
            itemTable.add(createPlaceholder(option.displayName, 1)).width(80).height(80).pad(5);

            Table infoTable = new Table();
            infoTable.add(new Label(option.displayName, skin)).left().row();
            Label costLabel = new Label(option.costString, skin);
            costLabel.setFontScale(0.8f);
            infoTable.add(costLabel).left().row();

            Label statusLabel = new Label(option.statusMessage, skin);
            statusLabel.setFontScale(0.8f);
            if (!option.isActionable) {
                statusLabel.setColor(Color.ORANGE);
            }
            infoTable.add(statusLabel).left();

            itemTable.add(infoTable).left().expandX().fillX();
            TextButton chooseButton = new TextButton("Select", skin);

            if (!option.isActionable) {
                chooseButton.setDisabled(true);
                chooseButton.getLabel().setColor(0.6f, 0.6f, 0.6f, 1);
            }

            chooseButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    buildingTypeToPlace = option.buildingType;
                    resourceTypeToPlace = option.resourceType;
                    addNotif("ACTION: Place " + option.displayName + " on an empty tile.");
                    if (activeDialog != null) {
                        activeDialog.remove();
                        activeDialog = null;
                    }
                }
            });

            listTable.add(itemTable).left();
            listTable.add(chooseButton).padLeft(20).row();
        }
    }
    private Object tryCall(Object target, String methodName, Object... args) {
        if (target == null) return null;
        try {
            Method[] methods = target.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.length) return m.invoke(target, args);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String safeGetNumberFromObject(Object target, String methodName, String fallback) {
        try { Object res = tryCall(target, methodName); return res != null ? res.toString() : fallback; } catch (Exception e) { return fallback; }
    }

    private Object safeGetKingdom() {
        try { return tryCall(currentUser, "getKingdom"); } catch (Exception e) { return null; }
    }

    private String safeUsername() {
        try { Method m = currentUser.getClass().getMethod("getUsername"); return (String) m.invoke(currentUser); } catch (Exception e) { return "Unknown"; }
    }

    private String safeKingdomName() {
        Object k = safeGetKingdom(); if (k == null) return "—"; try { Object t = tryCall(k, "getType"); return t != null ? t.toString() : "—"; } catch (Exception e) { return "—"; }
    }

    private GameUser[] safeGetUsers(GameUser current) {
        if (current == null) return null; try { Object game = current.getGame(); if (game != null) return ((GameOfThrones) game).getUsers(); } catch (Exception ignored) {} return null;
    }


    private void setStatus(String msg) { statusLabel.setText(msg); }


    private Table buildStaticMapContent() {
        Table content = new Table(skin);
        content.pad(10);

        Table mapGrid = new Table();

        if (currentGame == null || currentGame.getMap() == null) {
            content.add(new Label("Map data is not available.", skin));
            return content;
        }

        Tile[][] tiles = currentGame.getMap().getMap();
        if (tiles == null) {
            content.add(new Label("Map tiles could not be loaded.", skin));
            return content;
        }

        Map<Integer, Color> playerColors = new HashMap<>();
        Color[] kingdomColors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.GRAY, Color.ORANGE, Color.CYAN};
        int colorIndex = 0;

        Map<Point, Integer> playerLocations = new HashMap<>();
        for (GameUser user : currentGame.getUsers()) {
            if (user != null && user.getKingdom() != null) {
                Point entryPoint = new Point(user.getKingdom().getEntryPoint().getX(), user.getKingdom().getEntryPoint().getY());
                playerLocations.put(entryPoint, user.getId());
                if (!playerColors.containsKey(user.getId())) {
                    playerColors.put(user.getId(), kingdomColors[colorIndex % kingdomColors.length]);
                    colorIndex++;
                }
            }
        }

        // Generate the map visuals tile by tile
        int tileSize = 12; // The size of each square on the static map
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                Tile tile = tiles[i][j];
                Point currentPoint = new Point(i, j);
                Color tileColor;

                if (playerLocations.containsKey(currentPoint)) {
                    tileColor = playerColors.get(playerLocations.get(currentPoint));
                } else if (tile != null && tile.getTerrainType() != null) {
                    tileColor = getColorForTerrain(tile.getTerrainType());
                } else {
                    tileColor = Color.BLACK;
                }

                Pixmap pixmap = new Pixmap(tileSize, tileSize, Pixmap.Format.RGBA8888);
                pixmap.fill();
                Image tileImage = new Image(new Texture(pixmap));
                pixmap.dispose();

                mapGrid.add(tileImage).size(tileSize);
            }
            mapGrid.row();
        }

        content.add(new Label("World Map", skin, "title")).padBottom(10).row();
        content.add(mapGrid).row();

        Table legend = new Table(skin);
        legend.padTop(20);
        legend.add(new Label("Legend:", skin)).left().colspan(4).padBottom(5).row();

        legend.add(createLegendItem(getColorForTerrain(TerrainType.LAND), "Land"));
        legend.add(createLegendItem(getColorForTerrain(TerrainType.SEA), "Sea"));
        legend.add(createLegendItem(getColorForTerrain(TerrainType.MOUNTAIN), "Mountain"));
        legend.add(createLegendItem(getColorForTerrain(TerrainType.SNOW), "Snow"));

        int playerLegendCount = 0;
        for (GameUser user : currentGame.getUsers()) {
            if (user != null && user.getKingdom() != null) {
                if (playerLegendCount % 4 == 0) {
                    legend.row(); // New row for players
                }
                legend.add(createLegendItem(playerColors.get(user.getId()), user.getUsername()));
                playerLegendCount++;
            }
        }

        content.add(legend).left();
        return content;
    }


    private Color getColorForTerrain(com.yourname.got.logic.enums.TerrainType terrainType) {
        return switch (terrainType) {
            case LAND -> new Color(0.1f, 0.5f, 0.2f, 1f); // Dark Green
            case SEA -> new Color(0.2f, 0.4f, 0.8f, 1f); // Blue
            case MOUNTAIN -> new Color(0.5f, 0.5f, 0.5f, 1f); // Gray
            case SNOW -> new Color(0.9f, 0.9f, 1f, 1f); // White
            default -> Color.BLACK;
        };
    }


    private Actor createLegendItem(Color color, String text) {
        Table item = new Table();
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        ///pixmap.setColor(color);
        pixmap.fill();
        item.add(new Image(new Texture(pixmap))).size(16).padRight(5);
        pixmap.dispose();
        item.add(new Label(text, skin));
        return item;
    }

    @Override public void show() {       Gdx.input.setInputProcessor(stage);


        mediator.setMessageListener(this);
        mediator.setPanelPresenter(this); // <<-- NEW: Register this screen as the panel presenter
        mediator.startLoopManager();
     /*   String musicToPlay = "default_gameplay"; // آهنگ پیش‌فرض اگر هیچکدام مطابقت نداشت

        if (currentUser != null && currentUser.getKingdom() != null) {
            KingdomType type = currentUser.getKingdom().getType();

            switch (type) {
                case WINTERFELL:
                    musicToPlay = "gameplay_snow";
                    break;
                case CASTERLY_ROCK:
                    musicToPlay = "gameplay_rock";
                    break;
                case IRON_ISLANDS:
                    musicToPlay = "gameplay_sea";
                    break;
                default:
                    musicToPlay = "default_gameplay";
                    break;
            }
        }
        this.game.getMusicManager().playMusic(musicToPlay);
*/
        updateGameMusic(); // <-- این خط را جایگزین کدهای بالا کنید

    }
// In file: GamePlayScreen.java

    /**
     * موسیقی بازی را بر اساس فصل و پادشاهی به‌روزرسانی می‌کند.
     * برای جلوگیری از پخش مکرر، تم فعلی را بررسی می‌کند.
     */
    private void updateGameMusic() {
        String season = mediator.getCurrentSeason();

        // اولویت با فصل زمستان است
        if ("WINTER".equalsIgnoreCase(season)) {
            if (!"winter".equals(currentMusicTheme)) {
                game.getMusicManager().playMusic("gameplay_winter");
                currentMusicTheme = "winter";
            }
            return; // اگر زمستان بود، دیگر نیازی به بررسی پادشاهی نیست
        }

        // اگر فصل زمستان نبود، بر اساس پادشاهی موسیقی پخش کن
        String kingdomTheme = "kingdom";
        if (!kingdomTheme.equals(currentMusicTheme)) {
            String musicToPlay = "default_gameplay";
            if (currentUser != null && currentUser.getKingdom() != null) {
                switch (currentUser.getKingdom().getType()) {
                    case WINTERFELL: musicToPlay = "gameplay_snow"; break;
                    case CASTERLY_ROCK: musicToPlay = "gameplay_rock"; break;
                    case IRON_ISLANDS: musicToPlay = "gameplay_sea"; break;
                    case EYRIE: musicToPlay = "gameplay_eyrie"; break;
                    case DRAGONSTONE: musicToPlay = "gameplay_dragon"; break;
                    case STORMS_END: musicToPlay = "gameplay_storm"; break;
                    case HIGHGARDEN: musicToPlay = "gameplay_highgarden"; break;

                    default: musicToPlay = "default_gameplay"; break;
                }
            }
            game.getMusicManager().playMusic(musicToPlay);
            currentMusicTheme = kingdomTheme;
        }
    }
//    public void render(float delta) {
//        timeSinceLastUpdate += delta;
//        if (timeSinceLastUpdate >= REFRESH_INTERVAL) {
//            refreshDynamicInfo();
//            updateGameMusic(); // <-- این خط را اینجا اضافه کنید
//            updateAtmosphere(); // <<--- این خط رو اضافه کن
//            timeSinceLastUpdate = 0f;
//        }
//
//        Gdx.gl.glClearColor(0.05f, 0.05f, 0.06f, 1f);
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        stage.getBatch().begin();
//        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        stage.getBatch().end();
//        stage.act(delta);
//        stage.draw();
//        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
//            mediator.saveGame();
//            Gdx.app.exit();
//        }
//    }
@Override
public void render(float delta) {
    // 1. آپدیت زمان و لاجیک
    timeSinceLastUpdate += delta;
    if (timeSinceLastUpdate >= REFRESH_INTERVAL) {
        refreshDynamicInfo();
        updateGameMusic();
        updateAtmosphere(); // آپدیت رنگ محیط
        timeSinceLastUpdate = 0f;
    }

    // 2. پاک کردن صفحه
    Gdx.gl.glClearColor(0.05f, 0.05f, 0.06f, 1f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    // 3. رسم لایه‌های گرافیکی
    stage.getBatch().begin();

    // الف) رسم تصویر پس‌زمینه
    stage.getBatch().setColor(Color.WHITE);
    if (background != null) {
        stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // ب) رسم لایه اتمسفر (رنگ فصل) - با شرط ایمنی
    // فقط اگر تکسچر وجود داشت و شفافیت بیشتر از 0 بود رسم کن
    if (whiteTexture != null && currentAtmosphereColor.a > 0.01f) {
        stage.getBatch().setColor(currentAtmosphereColor);
        stage.getBatch().draw(whiteTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // برگرداندن رنگ به سفید برای جلوگیری از رنگی شدن دکمه‌ها
    stage.getBatch().setColor(Color.WHITE);

    stage.getBatch().end();

    // 4. رسم دکمه‌ها و ساختمان‌ها
    stage.act(delta);
    stage.draw();

    // 5. خروج با Escape
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
        mediator.saveGame();
        Gdx.app.exit();
    }
}

    @Override public void resize(int width, int height) { stage.getViewport().update(width,height,true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        mediator.setMessageListener(null);
        mediator.setPanelPresenter(null);
    }
//    @Override public void dispose() { stage.dispose(); skin.dispose(); background.dispose(); tileTexture.dispose(); }
    // File: GamePlayScreen.java

//@Override
//public void dispose() {
//    stage.dispose();
//    skin.dispose();
//    background.dispose();
//    tileTexture.dispose();
//
//    // dispose کردن تکسچرهای دستی
//    if (whiteTexture != null) whiteTexture.dispose();
//    if (mapVisorTexture != null) mapVisorTexture.dispose();
//    if (shipTexture != null) shipTexture.dispose();
//
//    // dispose کردن تکسچرهای نوار سلامتی
//    if (tBlack != null) tBlack.dispose();
//    if (tGreen != null) tGreen.dispose();
//    if (tOrange != null) tOrange.dispose();
//    if (tRed != null) tRed.dispose();
//
//    // dispose کردن تمام عکس‌های ساختمان‌ها در کش
//    for (TextureRegionDrawable drawable : buildingDrawableCache.values()) {
//        if (drawable.getRegion() != null && drawable.getRegion().getTexture() != null) {
//            drawable.getRegion().getTexture().dispose();
//        }
//    }
//    buildingDrawableCache.clear();
//}    // >> ADD THIS NEW METHOD ANYWHERE INSIDE THE GamePlayScreen CLASS <<

    @Override
    public void dispose() {
        // پاکسازی استیج و اسکین
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (background != null) background.dispose();

        // پاکسازی تکسچرهای دستی
        if (whiteTexture != null) whiteTexture.dispose();
        if (mapVisorTexture != null) mapVisorTexture.dispose();
        if (shipTexture != null) shipTexture.dispose();

        // پاکسازی تکسچرهای نوار سلامتی
        if (tBlack != null) tBlack.dispose();
        if (tGreen != null) tGreen.dispose();
        if (tOrange != null) tOrange.dispose();
        if (tRed != null) tRed.dispose();

        // --- اصلاح حیاتی برای جلوگیری از کرش ---
        // اول کش را پاک می‌کنیم، اما مواظب هستیم tileTexture را پاک نکنیم
        for (TextureRegionDrawable drawable : buildingDrawableCache.values()) {
            if (drawable.getRegion() != null && drawable.getRegion().getTexture() != null) {
                Texture t = drawable.getRegion().getTexture();
                // فقط اگر این تکسچر، tileTexture اصلی "نیست"، آن را پاک کن
                if (t != tileTexture) {
                    t.dispose();
                }
            }
        }
        buildingDrawableCache.clear();

        // در آخر، خود tileTexture را با خیال راحت پاک می‌کنیم
        if (tileTexture != null) tileTexture.dispose();
    }

    @Override
    public void refreshMap() {
        // This log helps confirm the entire chain is working
        System.out.println("UI REFRESH TRIGGERED: A building's visual state has changed.");

        // This is the method you wanted to call!
        renderTilesAndUpdate();
    }

    private void showBuildingInfoBubble(Building building, float tileX, float tileY) {
        // اگر پنجره قبلی باز است، آن را حذف کن
        if (buildingInfoBubble != null) {
            buildingInfoBubble.remove();
        }

        // ساخت پنجره با استایل skin
        buildingInfoBubble = new Window("", skin);
        buildingInfoBubble.setMovable(false);
        // تنظیم پس‌زمینه تیره و شفاف برای زیبایی
        buildingInfoBubble.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.85f)));
        buildingInfoBubble.pad(10);

        // آماده‌سازی اطلاعات
        String nameText = building.getName();
        String levelText = "Level " + building.getLevel();
        String hpText = "HP: " + building.getHealth();
        String statusText = building.getStatus() == Status.stable ? "Active" : building.getStatus().toString();

        // تنظیم رنگ وضعیت
        Color statusColor = Color.GREEN;
        if (building.getStatus() == Status.underConstruction) statusColor = Color.ORANGE;
        else if (building.getStatus() == Status.upgrading) statusColor = Color.CYAN;
        else if (building.getStatus() == Status.underRepair) statusColor = Color.YELLOW;
        else if (building.isDestroyed()) statusColor = Color.RED;

        // چیدمان محتوا
        Label nameLabel = new Label(nameText, skin);
        nameLabel.setColor(Color.GOLD);
        nameLabel.setFontScale(1.1f);

        Label lvlLabel = new Label(levelText, skin);
        Label hpLabel = new Label(hpText, skin);
        Label stLabel = new Label(statusText, skin);
        stLabel.setColor(statusColor);

        buildingInfoBubble.add(nameLabel).colspan(2).center().padBottom(5).row();
        buildingInfoBubble.add(new Image(skin.newDrawable("white", Color.GRAY))).height(1).colspan(2).fillX().padBottom(5).row();
        buildingInfoBubble.add(lvlLabel).left().padRight(10);
        buildingInfoBubble.add(hpLabel).right().row();
        buildingInfoBubble.add(stLabel).colspan(2).center().padTop(5);

        buildingInfoBubble.pack(); // محاسبه سایز

        // محاسبه موقعیت (کمی بالاتر از کاشی کلیک شده)
        // tileX و tileY مختصات دکمه در صفحه هستند
        // ما پنجره را کمی بالاتر و وسط دکمه قرار می‌دهیم
        float bubbleX = tileX + (50 - buildingInfoBubble.getWidth()) / 2; // فرض بر اینکه سایز کاشی حدود 50 است
        float bubbleY = tileY + 55;

        // جلوگیری از خروج پنجره از کادر صفحه
        if (bubbleX < 0) bubbleX = 10;
        if (bubbleY + buildingInfoBubble.getHeight() > stage.getHeight()) bubbleY = tileY - buildingInfoBubble.getHeight() - 10;

        buildingInfoBubble.setPosition(bubbleX, bubbleY);

        // انیمیشن باز شدن (Fade In)
        buildingInfoBubble.getColor().a = 0f;
        buildingInfoBubble.addAction(Actions.fadeIn(0.2f));

        stage.addActor(buildingInfoBubble);
    }

    // متد نمایش متن شناور (مثل +100 Gold یا -50 HP)
    @Override
    public void showFloatingText(int row, int col, String text, String colorName) {
        // ۱. پیدا کردن کاشی مورد نظر در گرید
        // توجه: در متد renderTilesAndUpdate ما دکمه‌ها را در tileButtons[r][c] ذخیره کردیم
        // باید مطمئن شویم که row و col در محدوده هستند
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) return;

        ImageButton targetBtn = tileButtons[row][col];
        if (targetBtn == null) return;

        // ۲. تبدیل مختصات دکمه به مختصات صفحه (Stage)
        Vector2 coords = targetBtn.localToStageCoordinates(new Vector2(0, 0));
        float x = coords.x + targetBtn.getWidth() / 2; // وسط دکمه
        float y = coords.y + targetBtn.getHeight() / 2;

        // ۳. انتخاب رنگ
        Color c = Color.WHITE;
        if (colorName.equalsIgnoreCase("GREEN")) c = Color.GREEN;
        else if (colorName.equalsIgnoreCase("RED")) c = Color.RED;
        else if (colorName.equalsIgnoreCase("YELLOW")) c = Color.YELLOW;
        else if (colorName.equalsIgnoreCase("GOLD")) c = Color.GOLD;

        // ۴. ساخت و نمایش متن
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), c);
        // کپی کردن استایل برای تغییر اندازه فونت بدون خراب کردن بقیه لیبل‌ها
        Label.LabelStyle bigStyle = new Label.LabelStyle(style);
        // اگر فونت بزرگتر دارید اینجا ست کنید، وگرنه همین سایز می‌ماند

        final Label label = new Label(text, bigStyle);
        label.setPosition(x, y);
        label.setAlignment(Align.center);
        label.setFontScale(1.3f); // کمی بزرگتر کردن متن

        // ۵. انیمیشن حرکت به بالا و محو شدن
        label.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0, 80, 2.0f), // حرکت ۸۰ پیکسل به بالا در ۲ ثانیه
                Actions.fadeOut(2.0f)        // همزمان محو شدن
            ),
            Actions.removeActor() // حذف از حافظه
        ));

        stage.addActor(label);
    }

    private void updateAtmosphere() {
        String season = mediator.getCurrentSeason();
        if (season == null) season = "UNKNOWN"; // جلوگیری از خطای NullPointer

        Color targetColor = new Color(0, 0, 0, 0f); // حالت عادی (شفاف)

        if ("WINTER".equalsIgnoreCase(season)) {
            // زمستان: آبی سرد
            targetColor = new Color(0.2f, 0.4f, 0.8f, 0.45f);
        } else if ("SUMMER".equalsIgnoreCase(season)) {
            // تابستان: زرد گرم
            targetColor = new Color(1f, 0.9f, 0.4f, 0.15f);
        } else if ("AUTUMN".equalsIgnoreCase(season)) {
            targetColor = new Color(0.8f, 0.5f, 0.2f, 0.25f);
        }

        // تغییر نرم رنگ
        currentAtmosphereColor.lerp(targetColor, 0.1f);
    }

    private Drawable getColoredDrawable(Color color, int w, int h) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
        return drawable;
    }

    private Texture createSinglePixelTexture(Color color) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(color);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private TextureRegionDrawable getCachedBuildingDrawable(String name, int level) {
        String key = name + "_" + level; // کلید منحصر به فرد: مثلا Mine_1

        if (!buildingDrawableCache.containsKey(key)) {
            // اگر در کش نیست، بساز و اضافه کن
            try {
                Texture t = GameFileUtility.getBuildingTexture(name, level);
                TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(t));
                buildingDrawableCache.put(key, drawable);
            } catch (Exception e) {
                // اگر عکس نبود، دیفالت را کش کن
                TextureRegionDrawable defaultDrawable = new TextureRegionDrawable(new TextureRegion(tileTexture));
                buildingDrawableCache.put(key, defaultDrawable);
            }
        }
        return buildingDrawableCache.get(key);
    }

    // --- متدهای کمکی جدید برای حل مشکل مختصات ---

    /**
     * این متد تلاش می‌کند ساختمان را پیدا کند.
     * ابتدا (row, col) را چک می‌کند، اگر نبود (col, row) را چک می‌کند.
     */
    private Building findBuildingSafely(int row, int col) {
        // تلاش اول: ترتیب استاندارد
        Building b = mediator.getBuildingAt(row, col);
        if (b != null) return b;

        // تلاش دوم: جابجایی سطر و ستون (برای اطمینان)
        b = mediator.getBuildingAt(col, row);
        return b;
    }

    /**
     * این متد گزینه ارتقا را در لیست پیدا می‌کند.
     * هر دو حالت مختصات را در رشته‌ها جستجو می‌کند.
     */
    private BuildingManager.UpgradeOption findUpgradeOptionSafely(int row, int col, MyLinkedList<BuildingManager.UpgradeOption> options) {
        String target1 = String.format("(%d,%d)", row, col);
        String target2 = String.format("(%d,%d)", col, row);

        for (BuildingManager.UpgradeOption option : options) {
            if (option.coords.equals(target1) || option.coords.equals(target2)) {
                return option;
            }
        }
        return null;
    }

    /**
     * این متد گزینه تعمیر را در لیست پیدا می‌کند.
     */
    private BuildingManager.RepairOption findRepairOptionSafely(int row, int col, MyLinkedList<BuildingManager.RepairOption> options) {
        String target1 = String.format("(%d,%d)", row, col);
        String target2 = String.format("(%d,%d)", col, row);

        for (BuildingManager.RepairOption option : options) {
            if (option.coords.equals(target1) || option.coords.equals(target2)) {
                return option;
            }
        }
        return null;
    }

    // متد اصلاح شده برای نمایش لاگ‌ها به صورت انگلیسی و بدون ایموجی (برای جلوگیری از ارور فونت)
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

    // متد برای انگلیسی کردن و تمیز کردن متن‌ها
    private String formatBattleMessage(String msg) {
        // اگر پیام فارسی در دیتابیس ذخیره شده باشد، سعی می‌کنیم ترجمه کنیم
        // (این بخش برای سازگاری با لاگ‌های قدیمی است)
        if (msg.contains("ضربه زدند")) return msg.replace("ضربه زدند", "dealt damage");
        if (msg.contains("قدرت")) return msg.replace("قدرت", "Power");
        if (msg.contains("مهاجم")) return msg.replace("مهاجم", "Attacker");
        if (msg.contains("مدافع")) return msg.replace("مدافع", "Defender");

        // تمیزکاری متن‌های انگلیسی
        if (msg.contains("totalDamage")) return msg.replace("totalDamage", "Damage");

        return msg;
    }

    // متد کمکی برای تبدیل متن غنایم به آیکون‌های گرافیکی
    private Table createLootDisplay(String lootString) {
        Table table = new Table();
        if (lootString == null || lootString.equals("N/A")) return table;

        // تمیز کردن متن برای استخراج اعداد
        // فرض بر این است که فرمت رشته: "Loot: [W: 100] [S: 200] ..." است
        String cleanStr = lootString.replaceAll("[^0-9\\- ]", "").trim();
        String[] parts = cleanStr.split("\\s+");

        if (parts.length >= 4) {
            try {
                int w = Integer.parseInt(parts[0]);
                int s = Integer.parseInt(parts[1]);
                int f = Integer.parseInt(parts[2]);
                int c = Integer.parseInt(parts[3]);

                addResourceIcon(table, "WOOD", w);
                addResourceIcon(table, "STONE", s);
                addResourceIcon(table, "FOOD", f);
                addResourceIcon(table, "COIN", c);

            } catch (Exception e) {
                // اگر فرمت خوانده نشد، متن ساده نشان بده
                Label lbl = new Label(lootString, skin);
                lbl.setFontScale(0.7f);
                lbl.setColor(Color.LIGHT_GRAY);
                table.add(lbl);
            }
        } else {
            Label lbl = new Label(lootString, skin);
            lbl.setFontScale(0.7f);
            table.add(lbl);
        }
        return table;
    }    // این متد جدید را به کلاس اضافه کنید تا ارور برطرف شود
//    private void addResourceIcon(Table table, String type, int amount) {
//        if (amount == 0) return; // اگر مقدار صفر است، چیزی نشان نده
//
//        // استفاده از عکس Storage به عنوان آیکون منابع (چون این عکس را حتماً دارید و کش شده است)
//        Image icon = new Image(getCachedBuildingDrawable("Storage", 0));
//
//        // تعیین رنگ (سبز برای مثبت، قرمز برای منفی)
//        Color color = (amount > 0) ? Color.GREEN : Color.FIREBRICK;
//        String text = (amount > 0 ? "+" : "") + amount;
//
//        Label label = new Label(text, skin);
//        label.setColor(color);
//        label.setFontScale(0.8f);
//
//        // اضافه کردن به جدول
//        table.add(icon).size(20).padRight(2);
//        table.add(label).padRight(10);
//    }

    // متد کمکی برای ساخت آیکون رنگی منابع (بدون نیاز به فایل عکس)
    private void addResourceIcon(Table table, String type, int amount) {
        if (amount == 0) return;

        // ساخت آیکون رنگی با استفاده از whiteTexture
        Image icon;
        if (whiteTexture != null) {
            icon = new Image(whiteTexture);
        } else {
            // حالت اضطراری اگر whiteTexture لود نشده باشد
            icon = new Image(skin.newDrawable("white", Color.WHITE));
        }

        // رنگ‌بندی استاندارد منابع
        switch (type.toUpperCase()) {
            case "WOOD": icon.setColor(new Color(0.6f, 0.4f, 0.2f, 1f)); break; // قهوه‌ای
            case "STONE": icon.setColor(Color.LIGHT_GRAY); break; // طوسی
            case "FOOD": icon.setColor(new Color(0.8f, 0.3f, 0.3f, 1f)); break; // قرمز (سیب/گوشت)
            case "COIN": icon.setColor(Color.GOLD); break; // طلایی
            default: icon.setColor(Color.WHITE);
        }

        // رنگ متن: سبز برای غنیمت، قرمز برای خسارت
        Color textColor = (amount > 0) ? Color.GREEN : Color.FIREBRICK;
        String text = (amount > 0 ? "+" : "") + amount;

        Label label = new Label(text, skin);
        label.setColor(textColor);
        label.setFontScale(0.8f);

        table.add(icon).size(14, 14).padRight(4);
        table.add(label).padRight(10);
    }


}
