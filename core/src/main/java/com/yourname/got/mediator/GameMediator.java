package com.yourname.got.mediator;


import com.yourname.got.logic.util.GameLog;
import com.badlogic.gdx.Gdx;
import com.yourname.got.GoTGame;
import com.yourname.got.logic.Balance.BuildingManager;
import com.yourname.got.logic.Balance.GameBalance;
import com.yourname.got.logic.Config.GameConfig;
import com.yourname.got.logic.enums.KingdomType;

import java.io.*;

import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.system.GameLoopManager;
import com.yourname.got.logic.system.GameRulesManager;
import com.yourname.got.logic.system.GameUserList;
import com.yourname.got.logic.util.*;
import com.yourname.got.screens.*;
import com.yourname.got.logic.WhiteWalker.*;
import com.yourname.got.logic.model.Building.Building;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class GameMediator {

    private final GoTGame game;
    private GameMessageListener messageListener;
    private final UserSystem userSystem;
    private GameUser currentUser;
    private GameOfThrones currentGame;
    private transient GameLoopManager gameLoopManager;
    private static GameMediator instance;
    private GamePanelPresenter panelPresenter;


    public GameMediator(GoTGame game) {
        this.game = game;
        this.messageListener = game;
        this.userSystem = new UserSystem();
        GameMediator.instance = this;

    }


    public interface GamePanelPresenter {
        void showDefeatPanel();
        void showGameResultsPanel(GameRecord record);
        void disablePlayerControlsForObservation();
        void refreshMap(); // <<-- ADD THIS LINE
        void showFloatingText(int row, int col, String text, String colorName);
        /** The dead are marching on the player (default: nothing). */
        default void showLongNightWarning(String enemies, int seconds, boolean finalWave) {}
        /** A White Walker battle against the player has ended (default: nothing). */
        default void showAssaultReport(com.yourname.got.logic.WhiteWalker.AssaultReport report) {}
    }

    public void setPanelPresenter(GamePanelPresenter presenter) {
        this.panelPresenter = presenter;
    }
    public static GameMediator getInstance() {
        return instance;
    }

    public static class TrainableUnitInfo {
        public final SoldierType type;
        public final String displayName;
        public final String costString;
        public final int health; // << فیلد جدید
        public final int attackPower; // << فیلد جدید
        public final boolean isAvailable;

        public TrainableUnitInfo(SoldierType type, String displayName, String costString, int health, int attackPower, boolean isAvailable) {
            this.type = type;
            this.displayName = displayName;
            this.costString = costString;
            this.health = health;
            this.attackPower = attackPower;
            this.isAvailable = isAvailable;
        }
    }

    public static class ShipInfo {
        public final boolean hasPortAccess;
        public final boolean isIronIslands;
        public final boolean hasRentedShip;
        public final int level;
        public final int attackPower;
        public final int capacity;
        public final String actionText; // e.g., "Upgrade", "Rent Ship"
        public final String costString;

        public ShipInfo(boolean hasPortAccess, boolean isIronIslands, boolean hasRentedShip, int level, int attackPower, int capacity, String actionText, String costString) {
            this.hasPortAccess = hasPortAccess;
            this.isIronIslands = isIronIslands;
            this.hasRentedShip = hasRentedShip;
            this.level = level;
            this.attackPower = attackPower;
            this.capacity = capacity;
            this.actionText = actionText;
            this.costString = costString;
        }
    }

    public static class DefenseUpgradeInfo {
        public final boolean canUpgrade;
        public final int nextLevel;
        public final int totalWoodCost;
        public final int totalStoneCost;
        public final int totalCoinCost;
        public final int totalTime;
        public final String statusMessage;

        public DefenseUpgradeInfo(boolean canUpgrade, int nextLevel, int wood, int stone, int coin, int time, String statusMessage) {
            this.canUpgrade = canUpgrade;
            this.nextLevel = nextLevel;
            this.totalWoodCost = wood;
            this.totalStoneCost = stone;
            this.totalCoinCost = coin;
            this.totalTime = time;
            this.statusMessage = statusMessage;
        }
    }

    public static class DefenseRepairInfo {
        public final boolean needsRepair;
        public final int totalWoodCost;
        public final int totalStoneCost;
        public final int totalCoinCost;
        public final int totalTime;

        public DefenseRepairInfo(boolean needsRepair, int wood, int stone, int coin, int time) {
            this.needsRepair = needsRepair;
            this.totalWoodCost = wood;
            this.totalStoneCost = stone;
            this.totalCoinCost = coin;
            this.totalTime = time;
        }
    }

    public static class ArcherDeploymentInfo {
        public final int availableArchers;
        public final int archersOnWall;

        public ArcherDeploymentInfo(int availableArchers, int archersOnWall) {
            this.availableArchers = availableArchers;
            this.archersOnWall = archersOnWall;
        }
    }

    /** What the attacker should know before pressing "Launch Attack" (shown in the confirmation step). */
    public static class AttackPreview {
        public final int totalSoldiers;
        public final int totalAttackPower;
        public final int totalHealth;
        public final int etaSeconds;
        public final boolean heroLeads;
        public final String targetNote;

        public AttackPreview(int totalSoldiers, int totalAttackPower, int totalHealth, int etaSeconds, boolean heroLeads, String targetNote) {
            this.totalSoldiers = totalSoldiers;
            this.totalAttackPower = totalAttackPower;
            this.totalHealth = totalHealth;
            this.etaSeconds = etaSeconds;
            this.heroLeads = heroLeads;
            this.targetNote = targetNote;
        }
    }

    public static class AttackTargetInfo {
        public final GameUser targetUser;
        public final String displayText;

        public AttackTargetInfo(GameUser targetUser, String displayText) {
            this.targetUser = targetUser;
            this.displayText = displayText;
        }
    }

    public static class PreAttackBriefingInfo {
        public final String vagueSoldierEstimate;
        public final String vagueResourceEstimate;
        public final boolean attackerHasSpy;

        public final int preciseTotalSoldiers;
        public final int preciseWood;
        public final int preciseFood;
        public final int defenseHealth;
        public final Tile[][] defenderMapLayout;

        public PreAttackBriefingInfo(String vSoldiers, String vRes, boolean hasSpy, int pSoldiers, int pWood, int pFood, int defHealth, Tile[][] map) {
            this.vagueSoldierEstimate = vSoldiers;
            this.vagueResourceEstimate = vRes;
            this.attackerHasSpy = hasSpy;
            this.preciseTotalSoldiers = pSoldiers;
            this.preciseWood = pWood;
            this.preciseFood = pFood;
            this.defenseHealth = defHealth;
            this.defenderMapLayout = map;
        }
    }

    public static class WallInfo {
        public final String healthStatus;
        public final int lastAttackDay;
        public final int totalHelpCount;
        public final boolean isDestroyed;

        public WallInfo(String healthStatus, int lastAttackDay, int totalHelpCount, boolean isDestroyed) {
            this.healthStatus = healthStatus;
            this.lastAttackDay = lastAttackDay;
            this.totalHelpCount = totalHelpCount;
            this.isDestroyed = isDestroyed;
        }
    }

    public static class ContributionInfo {
        public final String kingdomName;
        public final String userName;
        public final long amount;

        public ContributionInfo(String kingdomName, String userName, long amount) {
            this.kingdomName = kingdomName;
            this.userName = userName;
            this.amount = amount;
        }
    }

    public static class WhiteWalkerInfo {
        public final int startDay;
        public final int finalDay;
        public final int attackInterval;
        public final String currentStrategy;
        public final boolean isFinalWaveTriggered;

        public WhiteWalkerInfo(int startDay, int finalDay, int attackInterval, String currentStrategy, boolean isFinalWaveTriggered) {
            this.startDay = startDay;
            this.finalDay = finalDay;
            this.attackInterval = attackInterval;
            this.currentStrategy = currentStrategy;
            this.isFinalWaveTriggered = isFinalWaveTriggered;
        }
    }

    public static class BattleHistoryInfo {
        public final boolean isAttack;
        public final String opponentName;
        public final int displayStars;
        public final String resultText;
        public final String lootString;       // <<-- NEW/MODIFIED
        public final String troopComposition;
        public final String timestamp;
        public final String replayFilePath;// <<-- NEW
        public String battleType; // فیلد جدید: Raid یا Conquest

        public BattleHistoryInfo(boolean isAttack, String opponent, int stars, String result,
                                 String loot, String troops, String time, String replayPath , String battleType) {
            this.isAttack = isAttack;
            this.opponentName = opponent;
            this.displayStars = stars;
            this.resultText = result;
            this.lootString = loot;
            this.troopComposition = troops;
            this.timestamp = time;
            this.replayFilePath = replayPath;
            this.battleType = battleType;
        }
    }

    // در فایل GameMediator.java
    public static class MissionInfo {
        public final String attackerKingdomName; // <<-- فیلد جدید
        public final String defenderKingdomName;
        public final String remainingTimeFormatted;
        public final float progressPercent;

        public MissionInfo(String attacker, String defender, String remainingTime, float progress) {
            this.attackerKingdomName = attacker; // <<-- مقداردهی فیلد جدید
            this.defenderKingdomName = defender;
            this.remainingTimeFormatted = remainingTime;
            this.progressPercent = progress;
        }
    }

    public static class NoticeInfo {
        public final int id;
        public final String title;
        public final String senderName;
        public final String body;
        public final String timestamp;
        public final boolean isRead;
        public final boolean isSystemNotice;
        public final GameUser receiver; // <--- این خط را اضافه کنید


        public NoticeInfo(int id, String title, String senderName, GameUser receiver, String body, String timestamp, boolean isRead, boolean isSystemNotice) { // <--- پارامتر GameUser receiver را به کانستراکتور اضافه کنید
            this.id = id;
            this.title = title;
            this.senderName = senderName;
            this.receiver = receiver; // <--- این خط را اضافه کنید
            this.body = body;
            this.timestamp = timestamp;
            this.isRead = isRead;
            this.isSystemNotice = isSystemNotice;
        }
    }
    // ✅ متد جدید برای ارسال پیام همگانی ✅
    public Result sendBroadcastMessage(String title, String body) {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");
        if (title.trim().isEmpty() || body.trim().isEmpty()) {
            return new Result(false, "Title and body cannot be empty.");
        }

        GameUser sender = currentGame.getUser(currentUser.getId());
        Kingdom senderKingdom = sender.getKingdom();
        CrowTower crowTower = senderKingdom.getCrowTower();

        if (crowTower == null) {
            return new Result(false, "You must build a Crow Tower to send broadcast messages.");
        }

        // پیام‌های همگانی برای سادگی آنی هستند.
        int durationSeconds = 0;

        // گیرنده null به معنای همگانی بودن است.
        Notice newNotice = new Notice(durationSeconds, sender, null, title, body);
        currentGame.getPostOffice().add(newNotice);

        GameFileUtility.saveFullGameState(currentGame);

        return new Result(true, "Broadcast message sent successfully!");
    }

    public static class MessageRecipientInfo {
        public final int userId;
        public final String displayText;

        public MessageRecipientInfo(int userId, String displayText) {
            this.userId = userId;
            this.displayText = displayText;
        }
    }

    /** Called by the White Walker manager when an attack on {@code userId} has been created. */
    public void triggerLongNightWarning(int userId, String enemies, int seconds, boolean finalWave) {
        if (panelPresenter != null && currentUser != null && currentUser.getId() == userId) {
            Gdx.app.postRunnable(() -> panelPresenter.showLongNightWarning(enemies, seconds, finalWave));
        }
    }

    /** Called by the White Walker mission when the battle against one kingdom is over. */
    public void triggerAssaultReport(com.yourname.got.logic.WhiteWalker.AssaultReport report) {
        if (report != null && panelPresenter != null && currentUser != null && currentUser.getId() == report.userId) {
            Gdx.app.postRunnable(() -> panelPresenter.showAssaultReport(report));
        }
    }

    public void triggerDefeatPanelDisplay() {
        if (panelPresenter != null) {
            Gdx.app.postRunnable(() -> panelPresenter.showDefeatPanel());
        }
    }

    public void triggerResultsPanelDisplay() {
        if (panelPresenter == null || currentUser == null || currentGame == null) {
            GameLog.error("WARN: Cannot trigger results panel - presenter/user/game missing.");
            return;
        }

        Result loadRes = UserSystem.loadGameRecord(currentUser, currentGame.getName());
        if (!loadRes.isSuccess() || loadRes.getGameInstance() == null) {
            sendMessage("Error: Could not load final game report.");
            goToGameJoin();
            return;
        }

        Object gi = loadRes.getGameInstance();
        if (!(gi instanceof GameRecord)) {
            sendMessage("Error: Game record is corrupted or invalid.");
            goToGameJoin();
            return;
        }

        final GameRecord record = (GameRecord) gi;

        Gdx.app.postRunnable(() -> {
            try {
                panelPresenter.showGameResultsPanel(record);
            } catch (Throwable t) {
                GameLog.error("ERROR showing results panel: " + t.getMessage());
                sendMessage("Error: Could not show results panel.");
                goToGameJoin();
            }
        });
    }

    public void triggerDisablePlayerControls() {
        if (panelPresenter != null) {
            Gdx.app.postRunnable(() -> panelPresenter.disablePlayerControlsForObservation());
        }
    }

    public boolean register(String username, String password, String confirmPassword) {
        if (username.isEmpty() || password.isEmpty()) {
            sendMessage("Username and password cannot be empty");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            sendMessage("The password and its confirmation are not the same");
            return false;
        }

        Result result = userSystem.register(username, password);

        sendMessage(result.getMessage());

        if (result.isSuccess()) {

            this.currentUser = (GameUser) result.getGameInstance();

            return true;
        } else {

            return false;
        }
    }
    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            sendMessage("Username and password cannot be empty");
            return;
        }

        Result result = userSystem.login(username, password);
        sendMessage(result.getMessage());

        if (result.isSuccess()) {
            this.currentUser = (GameUser) result.getGameInstance();

            // بررسی اینکه آیا کاربر در یک بازی بوده است
            if (currentUser.getInGame() && currentUser.getGame() != null) {
                GameOfThrones activeGame = GameFileUtility.loadGame(
                    currentUser.getGame().getName(),
                    currentUser.getGame().getId()
                );

                if (activeGame != null) {
                    // *** اصلاح حیاتی: مدیریت بازی تمام شده ***
                    if (activeGame.isFinished()) {
                        GameLog.info("LOG: User logged into a finished game. Clearing state and showing history.");

                        // چون بازی تمام شده، نیازی نیست دوباره وارد بازی شویم
                        this.currentGame = null;
                        currentUser.setInGame(false);
                        currentUser.setGame(null);
                        UserSystem.updateUser(currentUser);

                        // به جای رفتن به گیم‌پلی، او را به لابی یا تاریخچه می‌بریم
                        goToGameJoin();
                        return; // خروج امن از متد
                    }

                    // اگر بازی هنوز در جریان است
                    if (activeGame.isGameReady()) {
                        this.currentGame = activeGame;
                        sendMessage("Resuming your active game...");
                        initiateGameSession(activeGame);
                        return;
                    }
                } else {
                    GameLog.info("WARN: Game file not found. Resetting user state.");
                    currentUser.setInGame(false);
                    currentUser.setGame(null);
                    UserSystem.updateUser(currentUser);
                }
            }
            goToGameLoading();
        }
    }
    // متد جدید برای خروج نهایی کاربر بعد از دیدن نتایج
    public void finishGameForUser() {
        if (currentUser == null) return;

        GameLog.info("Cleaning up user " + currentUser.getUsername() + " after game end.");

        // پاک‌سازی وضعیت کاربر
        currentUser.setInGame(false);
        currentUser.setGame(null);
        currentUser.setInFight(false);

        // ذخیره تغییرات
        UserSystem.updateUser(currentUser);

        // نال کردن متغیرهای مدیاتور
        this.currentGame = null;

        // بازگشت به لابی
        goToGameJoin();
    }

    public Result changeUserPassword(String currentPassword, String newPassword, String confirmNewPassword) {
        if (currentUser == null) {
            return new Result(false, "No active user. Please log in.");
        }

        if (!com.yourname.got.logic.login.PasswordUtil.matches(currentPassword, currentUser.getPassword())) {
            return new Result(false, "Current password is incorrect.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return new Result(false, "New password cannot be empty.");
        }
        if (!newPassword.matches("\\d{4}")) {
            return new Result(false, "New password must be exactly 4 digits.");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            return new Result(false, "New password and confirmation do not match.");
        }

        currentUser.setPassword(com.yourname.got.logic.login.PasswordUtil.hash(newPassword));

        UserSystem.updateUser(currentUser);

        return new Result(true, "Password changed successfully!");
    }

    public List<String> getGameList() {
        return GameBuilder.getGameList();
    }

    public Result createNewGame(String gameName, String password) {
        if (currentUser == null) {
            return new Result(false,"First you need to log in to your account");
        }
        if (currentUser.isInGame) {
            return new Result(false,"You are already a member in a game");
        }

        GameBuilder.CreateGameResult result = GameBuilder.createNewGame(gameName, password, currentUser);
        sendMessage(result.getMessage());

        if (result.isSuccess()) {
            goToGameLoading();
        }
        return new Result(true,result.getMessage());
    }

    public void joinGame(int selectedIndex, List<String> games, String password) {
        if (currentUser == null) {
            sendMessage("You must be logged in to join a game.");
            return;
        }
        if (currentUser.getInGame()) {
            sendMessage("You are already in a game. Please leave your current game first.");
            return;
        }

        GameBuilder.JoinGameResult result = GameBuilder.joinGame(selectedIndex, games, password, currentUser);

        sendMessage(result.getMessage());

        if (result.isSuccess()) {
            this.currentGame = result.getGame();
            goToLoadingScreen();
        }
    }

    public void leaveCurrentGame() {
        if (isGameStateInvalid()) {
            return;
        }
        Result result = currentGame.removePlayer(currentUser);

        sendMessage(result.getMessage());

        if (result.isSuccess()) {
            this.currentGame = null;
        }
    }


    /** A game-wide event (the Wall, market shipments...). Shown only on the in-game screen. */
    public static void sendGlobalMessage(String text) {
        if (instance == null) {
            GameLog.info("GLOBAL MESSAGE (Mediator not ready): " + text);
            return;
        }
        GameMessageListener listener = instance.messageListener;
        if (listener != null && listener.acceptsGameEvents()) {
            listener.onMessage(text);
        }
    }

    /**
     * An event that concerns ONE player (his storage is full, his deal expired...).
     * It is shown only if that player is the one currently playing, and only on the in-game screen:
     * before, such messages popped up on other kingdoms' screens and even on the login screen.
     */
    public static void sendMessageToUser(GameUser user, String text) {
        if (user == null) return;
        if (instance == null) {
            GameLog.info("MESSAGE for " + user.getUsername() + " (Mediator not ready): " + text);
            return;
        }
        GameUser current = instance.currentUser;
        if (current != null && current.getId() == user.getId()) {
            sendGlobalMessage(text);
        }
    }


    public String getCurrentDay() {
        if (currentGame == null || currentGame.getGameTime() == null) return "—";
        return String.valueOf(currentGame.getGameTime().getDay());
    }

    public String getCurrentSeason() {
        if (currentGame == null || currentGame.getGameTime() == null) return "—";
        return currentGame.getGameTime().getSeason().toString();
    }

    public String getWallStatus() {
        if (currentUser == null || currentGame == null) return "Wall: Unknown";

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "Wall: Unknown";

        DefenceManager defenceManager = userInGame.getKingdom().getArmyManager().getDefenceManager();
        if (defenceManager != null && defenceManager.getDefenseComplex() != null && defenceManager.getDefenseComplex().getWall() != null) {
            Wall wall = currentGame.getWall();
            return String.format("%d / %d", wall.getHealth(), wall.getInitialHealth());
        }

        return "Wall: N/A";
    }


    public void goToGameBuilder() {
        if (currentUser != null) {
            game.setScreen(new GameBuilderScreen(game, this, currentUser));
        } else {
            sendMessage("ابتدا باید وارد شوید.");
        }
    }

    public void goToGameJoin() {
        if (currentUser != null) {
            game.setScreen(new GameJoinScreen(game, this, currentUser));
        } else {
            sendMessage("ابتدا باید وارد شوید.");
        }
    }

    public void goToLoadingScreen() {
        if (currentUser != null) {
            game.setScreen(new GameLoadingScreen(game, this, currentUser));
        } else {
            sendMessage("You must be in a game to go to this page");
        }
    }

    public void goToGamePlayScreen() {
        if (currentUser != null && currentGame != null) {
            game.setScreen(new GamePlayScreen(game, this, currentUser, currentGame));
        } else {
            sendGlobalMessage("برای شروع بازی باید وارد یک بازی آماده شوید.");
        }
    }

    public void goToGameLoading() {
        if (currentUser != null) {
            game.setScreen(new GameLoadingScreen(game, this, currentUser));
        } else {
            sendMessage("You must be logged in to proceed.");
        }
    }

    public Result startGameFromUI(GameUser user, int gameId) {
        if (user == null) {
            return new Result(false, "Authentication error. User is not valid.");
        }

        try {
            if (user.getGame() == null) {
                return new Result(false, "Error: User is not associated with a game.");
            }
            GameOfThrones gameInstance = GameFileUtility.loadGame(user.getGame().getName(), gameId);

            if (gameInstance == null) {
                return new Result(false, "Error: Game could not be found or loaded.");
            }
            if (!gameInstance.getOwner().getUsername().equals(user.getUsername())) {
                return new Result(false, "Only the game owner can start the game.");
            }
            if (gameInstance.isGameReady()) {
                return new Result(false, "This game has already started.");
            }
            // Bots count as opponents: a lone player can start when bots are configured.
            if (gameInstance.getNumberOfPlayers() + GameOfThrones.configuredBots() < 2) { // Minimum 2 players
                return new Result(false, "At least 2 players are required to start.");
            }
// در فایل GameMediator.java و متد startGameFromUI

            gameInstance.startGame(); // در این خط پادشاهی‌ها اختصاص داده می‌شوند

// **** این خط جدید و حیاتی را اضافه کنید ****
// آبجکت currentUser را با نسخه جدید از داخل بازی رفرش می‌کنیم
            this.currentUser = gameInstance.getUser(user.getId());

            this.currentGame = gameInstance;
            initiateGameSession(gameInstance);
// ...
            GameLog.info("thereds start in start method");
            return new Result(true, "Game started successfully!", gameInstance);


        } catch (Exception e) {
            GameLog.error("FATAL: An unexpected error occurred while starting game ID " + gameId);
            e.printStackTrace();
            return new Result(false, "An unexpected server error occurred.");
        }
    }

    private static boolean isTimeCheating(GameOfThrones game) {
        if (game == null || game.getGameTime() == null || game.getGameTime().getStartTime() == null) {
            return false;
        }

        long gameStartTime = game.getGameTime().getStartTime();
        long currentUserTime = System.currentTimeMillis();

        return currentUserTime < gameStartTime;
    }

/**
     * The player as it lives INSIDE the running game (the object the game loop updates).
     * After "login -> resume game" the mediator used to keep a separate, stale copy of the user, so labels read
     * from it (Hero: Locked...) never changed.
     */
    public GameUser getCurrentGameUser() {
        if (currentGame != null && currentUser != null) {
            GameUser inGame = currentGame.getUser(currentUser.getId());
            if (inGame != null) return inGame;
        }
        return currentUser;
    }

/** Called by the lobby of a player who was waiting when the owner started the game. */
    public void enterStartedGame(GameOfThrones game) {
        if (game == null || currentUser == null) return;
        GameUser inGame = game.getUser(currentUser.getId());
        if (inGame != null) this.currentUser = inGame;
        this.currentGame = game;
        initiateGameSession(game);
    }

public void initiateGameSession(GameOfThrones game) {
    GameLog.info("LOG: Initiating Game Session..."); // لاگ دیباگ

    if (game == null) {
        sendMessage("Error: Cannot start session for a null game.");
        return;
    }

    defeatPanelShownFor.clear();
    // Work with the user that lives inside this game from now on.
    if (currentUser != null) {
        GameUser inGameUser = game.getUser(currentUser.getId());
        if (inGameUser != null) this.currentUser = inGameUser;
    }

    if (isTimeCheating(game)) {
        sendMessage(" [ANTI-CHEAT] Your system time is invalid.");
        goToGameJoin();
        return;
    }

    // استفاده از try-catch برای جلوگیری از فریز شدن در صورت بروز خطا
    try {
        if (game.getRulesManager() == null) {
            game.setRulesManager(new GameRulesManager(game));
        }

        this.currentGame = game;
        sendMessage("Welcome to the game!");

        // بررسی وضعیت شکست
        GameOfThrones.PlayerState playerState = currentGame.getPlayerState(currentUser.getId());

        goToGamePlayScreen(); // انتقال به صفحه بازی

        if (playerState != null && playerState.status == GameOfThrones.PlayerStatus.DEFEATED) {
            Gdx.app.postRunnable(() -> {
                if (panelPresenter != null) {
                    panelPresenter.showDefeatPanel();
                }
            });
        }

        GameLog.info("LOG: Game Session Started Successfully.");

    } catch (Exception e) {
        // چاپ کامل خطا برای فهمیدن علت گیر کردن
        GameLog.error("CRITICAL ERROR in initiateGameSession:");
        e.printStackTrace();
        sendMessage("Error starting game: " + e.getMessage());
    }
}
    public void startLoopManager() {
        if (currentGame == null || currentUser == null) return;
        GameUser me = currentGame.getUser(currentUser.getId());
        if (me == null || currentGame.isFinished()) return;

        if (gameLoopManager != null && gameLoopManager.isAlive()) {
            if (gameLoopManager.isRunningFor(currentGame, me.getId())) {
                GameLog.info("INFO: Game loop is already running.");
                return;
            }
            // A loop of another game / another user is still running: stop it first,
            // otherwise two loops would update the game twice per tick.
            gameLoopManager.requestStop();
        }

        GameLog.info("INFO: Starting GameLoopManager thread...");
        GameUserList list = new GameUserList();
        list.add(me);
        GameLoopManager loop = new GameLoopManager(list, this);
        loop.setDaemon(true);
        gameLoopManager = loop;
        loop.start();
    }

    /** Stops the running game loop (call when the player leaves the game or logs out). */
    public void stopLoopManager() {
        if (gameLoopManager != null) {
            gameLoopManager.requestStop();
            gameLoopManager = null;
        }
    }


    public GameUser getCurrentUser() {
        return currentUser;
    }

    public GameOfThrones getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(GameOfThrones game) {
        this.currentGame = game;
    }

    public void setMessageListener(GameMessageListener listener) {
        this.messageListener = listener;
    }

    public GameMessageListener getMessageListener(){return messageListener;}

    public void sendMessage(String message) {
        if (messageListener != null) {
            messageListener.onMessage(message);
        }
    }

    private boolean isGameStateInvalid() {
        if (currentUser == null || currentGame == null) {
            sendMessage("Error: You are not in an active game");
            return true;
        }
        return false;
    }

    public void deleteCurrentGame() {
        if (isGameStateInvalid()) { // Checks for null currentUser or currentGame
            return; // Message is sent inside the helper method
        }

        if (!currentGame.getOwner().getUsername().equals(currentUser.getUsername())) {
            sendMessage("Only the game owner can delete the game.");
            return;
        }
        GameFileUtility.deleteGame(currentGame);
        sendMessage("Game has been successfully deleted.");

        // Navigate the user to a safe screen after deletion
        goToGameJoin(); // Or any other appropriate screen
    }

    public int getBuildingCount(Building building, String resourceType) {
        if (building == null) return 0;

        Kingdom k = building.getKingdom();
        if (k == null) return 0;

        if (building instanceof Mine) {
            return k.countMinesOfType(resourceType.toUpperCase());
        } else if (building instanceof Storage) {
            return k.countStoragesOfType(resourceType.toUpperCase());
        } else {
            int count = 0;
            for (Building b : k.getBuildings()) {
                if (b == null) continue;
                if (b.getClass().equals(building.getClass())) {
                    count++;
                }
            }
            return count;
        }
    }

    public Result upgradeBuildingAt(int x1Based, int y1Based) {
        if (isGameStateInvalid()) {
            return new Result(false, "Error: You are not in an active game.");
        }

        try {
            GameUser userInGame = currentGame.getUser(currentUser.getId());
            if (userInGame == null) {
                return new Result(false, "Internal error: Current user not found in game.");
            }
            Kingdom kingdom = userInGame.getKingdom();
            if (kingdom == null) return new Result(false, "Error: Kingdom not found.");

            GameRulesManager rules = currentGame.getRulesManager();
            if (!rules.canPerformNonWarAction(userInGame)) {
                return new Result(false, "You cannot upgrade buildings while in combat.");
            }

            Building building = kingdom.getUserMap().getBuildingAt(x1Based, y1Based);
            if (building == null) {
                return new Result(false, "No building found at the selected coordinates.");
            }

            if (building instanceof WallsOfGate || building instanceof Gate) {
                return new Result(false, "Walls and Gates must be upgraded via military management.");
            }

            String resourceType = null;
            int existingCount = 0;

            if (building instanceof Mine mine) {
                resourceType = mine.getResourceType();
                existingCount = kingdom.countMinesOfType(resourceType);
            } else if (building instanceof Storage storage) {
                resourceType = storage.getResourceType();
                existingCount = kingdom.countStoragesOfType(resourceType);
            } else {
                existingCount = kingdom.countBuildingsOfType(building.getClass().getSimpleName());
            }

            Result canUpgrade = rules.canUpgradeBuilding(userInGame, building);
            if (!canUpgrade.isSuccess()) {
                return canUpgrade;
            }

            Result upgradeResult = BuildingManager.upgrade(kingdom, building, resourceType, existingCount, currentGame.getSpeed());

            if (upgradeResult.isSuccess()) {
                GameFileUtility.saveFullGameState(currentGame);
            }

            return upgradeResult;

        } catch (Exception ex) {
            ex.printStackTrace();
            return new Result(false, "An internal error occurred while upgrading the building.");
        }
    }

    public BuildingManager.UpgradeOption getUpgradeOptionForBuilding(int x1Based, int y1Based) {
        if (isGameStateInvalid()) {
            sendMessage("Error: Invalid game state.");
            return null;
        }

        MyLinkedList<BuildingManager.UpgradeOption> allOptions = BuildingManager.getUpgradeOptions(currentUser);

        String targetCoords = String.format("(%d,%d)", x1Based, y1Based);

        for (BuildingManager.UpgradeOption option : allOptions) {
            if (option.coords.equals(targetCoords)) {
                return option;
            }
        }
        return null;
    }

    public BuildingManager.RepairOption getRepairOptionForBuilding(int x1Based, int y1Based) {
        if (isGameStateInvalid()) {
            return null;
        }
        MyLinkedList<BuildingManager.RepairOption> allOptions = BuildingManager.getRepairOptions(currentUser);
        String targetCoords = String.format("(%d,%d)", x1Based, y1Based);

        for (BuildingManager.RepairOption option : allOptions) {
            if (option.coords.equals(targetCoords)) {
                return option;
            }
        }
        return null;
    }

    public Result repairBuildingAt(int x1Based, int y1Based) {
        if (isGameStateInvalid()) {
            return new Result(false, "خطا: وضعیت بازی نامعتبر است.");
        }
        try {
            GameUser userInGame = currentGame.getUser(currentUser.getId());
            Kingdom kingdom = userInGame.getKingdom();
            // توجه: LibGDX معمولاً (0,0) را گوشه بالا-چپ در نظر می‌گیرد، اما منطق شما ممکن است متفاوت باشد.
            // اینجا فرض می‌کنیم y نماینده ردیف و x ستون است.
            // Same convention as upgradeBuildingAt/destroyBuildingAt: local (x, y) of the selected tile. The old
            // "(y - 1, x - 1)" pointed at a DIFFERENT tile, so a repair could hit the wrong building.
            Building buildingToRepair = kingdom.getUserMap().getBuildingAt(x1Based, y1Based);

            if (buildingToRepair == null) {
                return new Result(false, "هیچ ساختمانی در این مختصات یافت نشد.");
            }

            // تعیین نوع منبع برای محاسبه هزینه
            String resourceType = (buildingToRepair instanceof Mine m) ? m.getResourceType()
                : (buildingToRepair instanceof Storage s) ? s.getResourceType() : null;

            int count = (buildingToRepair instanceof Mine m) ? kingdom.countMinesOfType(m.getResourceType())
                : (buildingToRepair instanceof Storage s) ? kingdom.countStoragesOfType(s.getResourceType())
                : kingdom.countStoragesOfType(buildingToRepair.getClass().getTypeName());

            // واگذاری منطق اصلی تعمیر به BuildingManager
            Result repairResult = BuildingManager.repair(kingdom, buildingToRepair, resourceType, count, currentGame.getSpeed());

            if (repairResult.isSuccess()) {
                GameFileUtility.saveFullGameState(currentGame);
            }
            return repairResult;

        } catch (Exception ex) {
            ex.printStackTrace();
            return new Result(false, "یک خطای داخلی هنگام تعمیر ساختمان رخ داد.");
        }
    }

    public Result moveBuilding(int srcX1, int srcY1, int destX1, int destY1) {
        // 1. اعتبارسنجی کلی
        if (currentUser == null || currentGame == null) {
            return new Result(false, "Invalid game state.");
        }

        // 2. پیدا کردن user واقعی داخل currentGame (اجتناب از کار روی کپی)
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) {
            return new Result(false, "Internal error: current user not found in game instance.");
        }

        Kingdom kingdom = userInGame.getKingdom();
        if (kingdom == null || kingdom.getUserMap() == null) {
            return new Result(false, "No kingdom or map found for the user.");
        }

        // 3. گرفتن تایل منبع به روش امن (getTile) — NOT selectTile
        // getTile باید 1-based بگیرد (مطابق پیاده‌سازی شما)
        Tile srcTile = kingdom.getUserMap().getTile(srcX1, srcY1);
        GameLog.info("moveBuilding: srcTile=" + srcTile + " building=" +
            (srcTile != null && srcTile.getObjectOnTile() != null ? srcTile.getObjectOnTile().getBuilding() : "null"));

        if (srcTile == null) {
            return new Result(false, "Source tile is out of bounds.");
        }
        if (srcTile.getObjectOnTile() == null || srcTile.getObjectOnTile().getBuilding() == null) {
            return new Result(false, "No building found at the source tile.");
        }

        // 4. بررسی مقصد (در محدوده و خالی بودن)
        if (!kingdom.getUserMap().isInsideBounds(destX1 - 1, destY1 - 1)) {
            // اگر UserSubMap شما isInsideBounds انتظار 0-based دارد، با -1 چک کن؛
            // یا اگر isInsideBounds خودش 1-based است باید این چک را متناسب تغییر دهی.
            // اینجا مثال کلی آورده شده — در صورت نیاز آن را به قرارداد شما تغییر بده.
        }
        Tile destTile = kingdom.getUserMap().getTile(destX1, destY1);
        if (destTile == null) {
            return new Result(false, "Destination tile is out of bounds.");
        }
        if (!destTile.isEmpty() && destTile.getObjectOnTile() != null && destTile.getObjectOnTile().getBuilding() != null) {
            return new Result(false, "Destination is not empty.");
        }

        // 5. فراخوانی changeTilePosition با همان شی تایل منبع
        boolean moved = kingdom.getUserMap().changeTilePosition(srcTile, destX1, destY1);
        if (!moved) {
            return new Result(false, "Unable to move building to the target location.");
        }

        // 6. ذخیره‌ی وضعیت بازی
        GameFileUtility.saveFullGameState(currentGame);

        return new Result(true, "Building moved successfully.");
    }

    public Building getBuildingAt(int x, int y) {
        if (isGameStateInvalid()) return null;

        // ✅ Always fetch the authoritative user from the current game
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) return null;

        return userInGame.getKingdom().getUserMap().getBuildingAt(x, y);
    }

    public Result destroyBuildingAt(int x, int y) {
        if (isGameStateInvalid()) {
            return new Result(false, "Invalid game state.");
        }

        // --- CRITICAL FIX: get the user from currentGame ---
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) {
            return new Result(false, "Internal Error: Current user not found in the game instance.");
        }
        Kingdom kingdom = userInGame.getKingdom();

        Building buildingToDestroy = kingdom.getUserMap().getBuildingAt(x, y);

        if (buildingToDestroy == null) {
            return new Result(false, "There is no building at the selected location.");
        }

        if (buildingToDestroy instanceof Castle) {
            return new Result(false, "The Castle cannot be destroyed.");
        }
        if (buildingToDestroy instanceof WallsOfGate || buildingToDestroy instanceof Gate) {
            return new Result(false, "Walls and Gates cannot be destroyed from this menu.");
        }

        // Delegate to BuildingManager
        Result destroyResult = BuildingManager.destroy(kingdom, buildingToDestroy);

        if (destroyResult.isSuccess()) {
            // --- Update all relevant structures safely ---

            // 1. Remove from the map
            kingdom.getUserMap().removeBuilding(x, y);

            // 2. Remove from the general list
            kingdom.getBuildings().remove(buildingToDestroy);

            // 3. Remove from specialized lists if applicable
            if (buildingToDestroy instanceof Mine) {
                kingdom.getMines().remove((Mine) buildingToDestroy);
            } else if (buildingToDestroy instanceof Storage) {
                kingdom.getStorages().remove((Storage) buildingToDestroy);
            }
            // If you have other building-specific lists like Barrack, Camp etc.,
            // add similar "else if" blocks here.

            // 4. Save the updated game state
            GameFileUtility.saveFullGameState(currentGame);
        }

        return destroyResult;
    }


    /** Current amount of a resource ("Wood", "Stone", "Coin", "Food") of the player; 0 outside a game. */
    public int getResourceAmount(String type) {
        GameUser user = getCurrentGameUser();
        if (user == null || user.getKingdom() == null) return 0;
        return user.getKingdom().getTotalResources(type);
    }

    public String getResourceInfoWood() {
        if (currentUser == null || currentGame == null) return "0 / 0";
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "0 / 0";

        Kingdom kingdom = userInGame.getKingdom();
        int amount = kingdom.getTotalResources("WOOD");
        int capacity = kingdom.getTotalCapacity("WOOD");
        return String.format("%d/%d", amount, capacity);
    }

    public String getResourceInfoStone() {
        if (currentUser == null || currentGame == null) return "0 / 0";
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "0 / 0";

        Kingdom kingdom = userInGame.getKingdom();
        int amount = kingdom.getTotalResources("STONE");
        int capacity = kingdom.getTotalCapacity("STONE");
        return String.format("%d/%d", amount, capacity);
    }

    public String getResourceInfoFood() {
        if (currentUser == null || currentGame == null) return "0 / 0";
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "0 / 0";

        Kingdom kingdom = userInGame.getKingdom();
        int amount = kingdom.getTotalResources("FOOD");
        int capacity = kingdom.getTotalCapacity("FOOD");
        return String.format("%d/%d", amount, capacity);
    }

    public String getResourceInfoCoin() {
        if (currentUser == null || currentGame == null) return "0 / 0";
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "0 / 0";

        Kingdom kingdom = userInGame.getKingdom();
        int amount = kingdom.getTotalResources("COIN");
        int capacity = kingdom.getTotalCapacity("COIN");
        return String.format("%d/%d", amount, capacity);
    }


    public void editGameSettings() {
        sendMessage("Settings editing is not implemented yet.");
    }

    public Result getGameDetails(String gameName, int gameId) {
        if (gameName == null || gameId == -1) {
            return new Result(false, "Invalid game selection.");
        }

        // The Mediator is the bridge and is allowed to call persistence utilities.
        GameOfThrones gameInstance = GameFileUtility.loadGame(gameName, gameId);

        if (gameInstance != null) {
            return new Result(true, "Game details loaded successfully.", gameInstance);
        } else {
            return new Result(false, "Could not load game details from file.");
        }
    }

    public Result buildBuilding(String buildingType, String resourceType, int x, int y) {
        if (isGameStateInvalid() || buildingType == null) {
            return new Result(false, "Invalid request state.");
        }
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) {
            return new Result(false, "Internal Error: User not found in game instance.");
        }
        Kingdom kingdom = userInGame.getKingdom();
        GameRulesManager rules = currentGame.getRulesManager();

        if (!rules.canPerformNonWarAction(userInGame)) {
            return new Result(false, "Cannot build while in combat.");
        }

        // --- START OF FIX: محاسبه صحیح تعداد موجود قبل از ساخت ---
        int existingCount = 0;
        if (resourceType != null) { // برای ساختمان‌های وابسته به منبع
            if ("Mine".equals(buildingType)) {
                existingCount = kingdom.countMinesOfType(resourceType);
            } else if ("Storage".equals(buildingType)) {
                existingCount = kingdom.countStoragesOfType(resourceType);
            }
        } else { // برای ساختمان‌های عمومی
            existingCount = kingdom.countBuildingsOfType(buildingType);
        }
        // --- END OF FIX ---

        Building newBuilding;
        try {
            newBuilding = switch (buildingType) {
                case "Defensive Tower" -> new DefensiveTower(kingdom);
                case "Mine" -> new Mine(resourceType, kingdom);
                case "Storage" -> new Storage(resourceType, kingdom);
                case "Barrack" -> new Barrack(kingdom);
                case "Camp" -> new Camp(kingdom);
                case "CrowTower" -> new CrowTower(kingdom);
                case "Transport" -> new Transport(kingdom);
                default -> throw new IllegalArgumentException("Unknown building type: " + buildingType);
            };
        } catch (Exception e) {
            return new Result(false, "Internal error: Failed to create building instance.");
        }

        if (!kingdom.getUserMap().addBuildingInMap(newBuilding, x, y)) {
            return new Result(false, "This location is not available for construction.");
        }

        // پاس دادن تعداد صحیح به لایه منطق
        Result constructionResult = BuildingManager.construct(kingdom, newBuilding, resourceType, existingCount);

        if (constructionResult.isSuccess()) {
            kingdom.registerNewBuilding(newBuilding);
            GameFileUtility.saveFullGameState(currentGame);
            currentGame.setRulesManager(new GameRulesManager(currentGame));
            return constructionResult;
        } else {
            kingdom.getUserMap().removeBuilding(x, y); // بازگرداندن تغییر در صورت شکست
            return constructionResult;
        }
    }

    public MyLinkedList<TrainableUnitInfo> getTrainableUnits() {
        MyLinkedList<TrainableUnitInfo> unitInfos = new MyLinkedList<>();

        // --- START OF FIX ---
        // اطمینان از معتبر بودن وضعیت بازی
        if (isGameStateInvalid()) return unitInfos;
        // جایگزینی getFreshUser() با منطق صحیح برای دریافت کاربر به‌روز
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        // --- END OF FIX ---

        if (userInGame == null) return unitInfos;
        Kingdom kingdom = userInGame.getKingdom();

        for (KingdomSoldierProfile profile : KingdomSoldierRegistry.getAllProfiles()) {
            if (profile.getKingdom() == kingdom.getType()) {
                String cost = String.format("W:%d F:%d C:%d", profile.getBaseType().woodCost, profile.getBaseType().foodCost, profile.getBaseType().coinCost);
                unitInfos.add(new TrainableUnitInfo(profile.getBaseType(), profile.getDisplayName(), cost, profile.getCustomHealth(), profile.getCustomAttack(), true));
            }
        }
        return unitInfos;
    }

    public ShipInfo getShipInfo() {
        // --- START OF FIX ---
        if (isGameStateInvalid()) return null;
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        // --- END OF FIX ---

        if (userInGame == null) return null;
        Kingdom kingdom = userInGame.getKingdom();

        boolean hasPort = kingdom.hasPort();
        if (kingdom.getType() == KingdomType.IRON_ISLANDS) {
            BattleShip ship = kingdom.getBattleShip();
            String cost = String.format("W:%d, S:%d, F:%d, C:%d", 500 * ship.level, 600 * ship.level, 100 * ship.level, 500 * ship.level);
            return new ShipInfo(hasPort, true, false, ship.level, ship.attackPower, ship.soldiersCapacity, "Upgrade Ship", cost);
        } else {
            BattleShip ship = kingdom.getBattleShip();
            boolean hasRented = (ship != null && ship.isRented);
            String action = hasRented ? "N/A" : "Rent Ship";
            String cost = hasRented ? "" : "W:200, S:100, C:800";
            return new ShipInfo(hasPort, false, hasRented, 1, 0, (hasRented ? ship.soldiersCapacity : 0), action, cost);
        }
    }

    public Result performShipAction() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Kingdom kingdom = userInGame.getKingdom();

        ShipInfo info = getShipInfo();

        if (info.isIronIslands) {
            BattleShip ship = kingdom.getBattleShip();
            int woodCost = 500 * ship.level, stoneCost = 600 * ship.level, foodCost = 100 * ship.level, coinCost = 500 * ship.level;
            if (kingdom.canAffordResources(woodCost, stoneCost, coinCost, foodCost)) {
                kingdom.detuctResources(woodCost, stoneCost, coinCost, foodCost);
                ship.upgrade();
                GameFileUtility.saveFullGameState(currentGame);
                return new Result(true, "Ship successfully upgraded to level " + ship.level + "!");
            } else {
                return new Result(false, "Not enough resources to upgrade the ship.");
            }
        } else {
            if (info.hasRentedShip) {
                return new Result(false, "You have already rented a ship.");
            }
            if (kingdom.canAffordResources(200, 100, 800, 0)) {
                kingdom.detuctResources(200, 100, 800, 0);
                kingdom.setRentedBattleShip(new BattleShip(true));
                GameFileUtility.saveFullGameState(currentGame);
                return new Result(true, "A ship has been rented for your next attack!");
            } else {
                return new Result(false, "Not enough resources to rent a ship.");
            }
        }
    }

    public Result trainOrManageShip(java.util.Map<SoldierType, Integer> trainRequests, boolean manageShipAction) {
        if (isGameStateInvalid()) {
            return new Result(false, "Invalid game state.");
        }

        Kingdom kingdom = currentGame.getUser(currentUser.getId()).getKingdom();

        if (trainRequests != null && !trainRequests.isEmpty()) {
            Barrack activeBarrack = kingdom.getFirstAvailableBarrack();
            if (activeBarrack == null) {
                return new Result(false, "ERROR: You must build a Barrack first!");
            }

            // The loop used to RETURN inside its first iteration, so only the first requested type was ever queued.
            boolean everythingQueued = true;
            StringBuilder report = new StringBuilder();
            int totalQueued = 0;
            for (java.util.Map.Entry<SoldierType, Integer> entry : trainRequests.entrySet()) {
                SoldierType type = entry.getKey();
                int quantityToTrain = entry.getValue();
                int successfullyQueued = 0;

                for (int i = 0; i < quantityToTrain; i++) {
                    if (activeBarrack.trainSoldier(type, currentGame.getSpeed())) {
                        successfullyQueued++;
                    } else {
                        break;
                    }
                }
                totalQueued += successfullyQueued;
                if (successfullyQueued < quantityToTrain) everythingQueued = false;
                if (report.length() > 0) report.append(", ");
                report.append(successfullyQueued).append("/").append(quantityToTrain).append(" ").append(type.name());
            }

            GameFileUtility.saveFullGameState(currentGame);

            if (everythingQueued) {
                return new Result(true, "SUCCESS: " + report + " added to the training queue.");
            } else if (totalQueued > 0) {
                return new Result(false, "WARNING: Only part of the request could be queued (" + report + "). Check resources/capacity.");
            } else {
                return new Result(false, "ERROR: Failed to queue anything (" + report + "). Check resources/capacity.");
            }
        }

        return new Result(false, "No action was performed.");
    }


    public MyLinkedList<Transaction> getMarketTransactions() {
        if (isGameStateInvalid()) {
            return new MyLinkedList<>();
        }
        Bazar bazar = currentGame.getBazar();
        if (bazar != null) {
            bazar.checkBazar(currentGame.getSpeed());
            return bazar;
        }
        return new MyLinkedList<>();
    }

    public MyLinkedList<MissionInfo> getActiveMissionsInfo() {
        MyLinkedList<MissionInfo> missionInfos = new MyLinkedList<>();
        if (isGameStateInvalid()) return missionInfos;

        MyLinkedList<Mission> allMissions = currentGame.getMissions();
        for (Mission mission : allMissions) {
            if (mission == null) continue;
            if (mission.getAttacker() == null) { // the dead marching on this player
                if (mission.getDefender() != null && mission.getDefender().getId() == currentUser.getId()) {
                    int total = mission.getInitialDuration();
                    float progress = (total > 0) ? Math.min(1.0f, (float) mission.getElapsedTime() / total) : 0f;
                    missionInfos.add(new MissionInfo("WHITE WALKERS", mission.getDefender().getKingdom().getType().toString(),
                        Time.formatSeconds(mission.getRemainingTime()), progress));
                }
                continue;
            }
            if (mission.getAttacker().getId() == currentUser.getId()) {

                int totalDuration = mission.getInitialDuration();
                int timeElapsed = mission.getElapsedTime();
                float progress = (totalDuration > 0) ? Math.min(1.0f, (float) timeElapsed / totalDuration) : 0f;
                String remainingTime = Time.formatSeconds(mission.getRemainingTime());
                // کد جدید
                String attackerName = mission.getAttacker().getKingdom().getType().toString();
                String defenderName = mission.getDefender().getKingdom().getType().toString();
                missionInfos.add(new MissionInfo(attackerName, defenderName, remainingTime, progress)); }
        }
        return missionInfos;
    }

    public Result buyMarketTransaction(int transactionIndex) {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Bazar bazar = currentGame.getBazar();
        Transaction transaction = bazar.getTransactionByIndex(transactionIndex);

        if (transaction == null) {
            return new Result(false, "The selected offer is no longer available.");
        }

        Result buyResult = bazar.buy(transaction, userInGame);

        if (buyResult.isSuccess()) {
            GameFileUtility.saveFullGameState(currentGame);
        }

        return buyResult;
    }

    public Result sellResourcesOnMarket(MyLinkedList<ResourceItem> resourcesToGive, MyLinkedList<ResourceItem> resourcesToTake) {
        if (isGameStateInvalid()) return new Result(false, "Invalid transaction data.");

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Bazar bazar = currentGame.getBazar();

        Transaction newTransaction = new Transaction(userInGame, 0, resourcesToGive, null, resourcesToTake, null, null);

        Result result = bazar.listOffer(newTransaction);

        if (result.isSuccess()) {
            GameFileUtility.saveFullGameState(currentGame);
        }
        return result;
    }

    public Result sellSoldiersOnMarket(MyLinkedList<ArmyManager.TroopRequest> soldiersToSell, MyLinkedList<ResourceItem> resourcesToTake) {
        if (isGameStateInvalid()) return new Result(false, "Invalid transaction data.");

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Kingdom kingdom = userInGame.getKingdom();
        ArmyManager armyManager = kingdom.getArmyManager();
        Bazar bazar = currentGame.getBazar();

        if (soldiersToSell.isEmpty()) {
            return new Result(false, "No soldiers selected to sell.");
        }
        ArmyManager.TroopRequest request = soldiersToSell.getHead().data;

        SoldierUnit unitToSell = armyManager.removeAndPackageSoldiers(request.type, request.count);

        if (unitToSell == null) {
            return new Result(false, "Not enough " + request.type.name() + " available to sell.");
        }

        Transaction newTransaction = new Transaction(userInGame, 0, null, unitToSell, resourcesToTake, null, null);

        Result result = bazar.listOffer(newTransaction);

        if (result.isSuccess()) {
            GameFileUtility.saveFullGameState(currentGame);
        } else {
            armyManager.addSoldierPackage(unitToSell);
        }
        return result;
    }

    public MyLinkedList<ArmyManager.TroopRequest> getSellableSoldiers() {
        if (isGameStateInvalid()) return new MyLinkedList<>();

        MyLinkedList<ArmyManager.TroopRequest> available = new MyLinkedList<>();
        ArmyManager army = currentUser.getKingdom().getArmyManager();

        for (SoldierUnit unit : army.getDefenceManager().getDefenders()) {
            if (unit != null && unit.getCount() > 0) {
                available.add(new ArmyManager.TroopRequest(unit.getType(), unit.getCount()));
            }
        }
        return available;
    }

    public MyLinkedList<String> adminGetAllNoticeTitles() {
        return Admin.listAllNoticeTitles();
    }

    public MyLinkedList<BuildingManager.ConstructionOption> getConstructionOptions() {
        if (isGameStateInvalid()) return new MyLinkedList<>();
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) return new MyLinkedList<>();
        return BuildingManager.getConstructionOptions(userInGame);
    }

    public MyLinkedList<BuildingManager.UpgradeOption> getUpgradeOptions() {
        if (isGameStateInvalid()) return new MyLinkedList<>();

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) return new MyLinkedList<>();

        return BuildingManager.getUpgradeOptions(userInGame);
    }

    public MyLinkedList<BuildingManager.RepairOption> getRepairOptions() {
        if (isGameStateInvalid()) return new MyLinkedList<>();
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null) return new MyLinkedList<>();
        return BuildingManager.getRepairOptions(userInGame);
    }

    public Result adminAddNotice(int day, String title, String body) {
        return Admin.writeLetter(day, title, body);
    }

    public Result adminDeleteNotice(int index) {
        return Admin.deleteLetterByIndex(index);
    }

    public Properties adminGetGlobalConfig() {
        return GameConfig.getGlobalProps();
    }

    /** Research tree: queues the next level of a technology for the current player (saved at once). */
    public Result researchStart(com.yourname.got.logic.research.Tech tech) {
        if (currentUser == null || currentUser.getKingdom() == null) return new Result(false, "No kingdom.");
        Result r = com.yourname.got.logic.research.Research.enqueue(currentUser.getKingdom(), tech);
        if (r.isSuccess() && currentGame != null) GameFileUtility.saveFullGameState(currentGame);
        return r;
    }

    /** Research tree: cancels the queue entry at index (and later levels of the same technology). */
    public Result researchCancel(int index) {
        if (currentUser == null || currentUser.getKingdom() == null) return new Result(false, "No kingdom.");
        Result r = com.yourname.got.logic.research.Research.cancel(currentUser.getKingdom(), index);
        if (r.isSuccess() && currentGame != null) GameFileUtility.saveFullGameState(currentGame);
        return r;
    }

    /** The current player's kingdom for the beginner's guide (null when no game is running). */
    public Kingdom getCurrentKingdomForGuide() {
        if (isGameStateInvalid()) return null;
        return currentGame.getUser(currentUser.getId()).getKingdom();
    }

    // ------------------------------------------------------------------ diplomacy
    /** One row of the diplomacy screen: another player and what stands between you. */
    public static class DiplomacyRow {
        public final GameUser other;
        public final String status;
        public final boolean hasPact, incoming, outgoing;
        public final com.yourname.got.logic.system.Diplomacy.Type type;
        public DiplomacyRow(GameUser other, String status, boolean hasPact, boolean incoming, boolean outgoing,
                            com.yourname.got.logic.system.Diplomacy.Type type) {
            this.other = other; this.status = status; this.hasPact = hasPact; this.incoming = incoming;
            this.outgoing = outgoing; this.type = type;
        }
    }

    public boolean diplomacyEnabled() { return com.yourname.got.logic.system.Diplomacy.enabled(); }

    public java.util.List<DiplomacyRow> diplomacyRows() {
        java.util.List<DiplomacyRow> rows = new java.util.ArrayList<>();
        if (isGameStateInvalid() || !diplomacyEnabled()) return rows;
        com.yourname.got.logic.system.Diplomacy d = currentGame.getDiplomacy();
        int day = currentGame.getGameTime().getDay();
        d.prune(day);
        int me = currentUser.getId();
        for (GameUser u : currentGame.getUsers()) {
            if (u == null || u.getId() == me) continue;
            GameOfThrones.PlayerState st = currentGame.getPlayerState(u.getId());
            if (st != null && st.status == GameOfThrones.PlayerStatus.DEFEATED) continue;
            com.yourname.got.logic.system.Diplomacy.Pact pact = d.pactBetween(me, u.getId(), day);
            com.yourname.got.logic.system.Diplomacy.Proposal pr = d.proposalBetween(me, u.getId());
            String text = "No agreement";
            if (pact != null) {
                text = pact.type == com.yourname.got.logic.system.Diplomacy.Type.ALLIANCE ? "Allied"
                    : "Peace until day " + pact.endDay;
            } else if (pr != null) {
                text = (pr.to == me ? "Offers you " : "You offered ") + pr.type.name().toLowerCase();
            }
            rows.add(new DiplomacyRow(u, text, pact != null, pr != null && pr.to == me, pr != null && pr.from == me,
                pact != null ? pact.type : (pr != null ? pr.type : null)));
        }
        return rows;
    }

    public Result diplomacyPropose(GameUser target, com.yourname.got.logic.system.Diplomacy.Type type) {
        if (isGameStateInvalid() || target == null) return new Result(false, "Invalid player.");
        if (!diplomacyEnabled()) return new Result(false, "Diplomacy is switched off.");
        int day = currentGame.getGameTime().getDay();
        com.yourname.got.logic.system.Diplomacy d = currentGame.getDiplomacy();
        String err = d.propose(currentUser.getId(), target.getId(), type, day);
        if (err != null) return new Result(false, err);
        if (com.yourname.got.logic.login.BotUser.isBot(target)) {
            d.respond(currentUser.getId(), target.getId(), true, day); // bots only defend, so they always agree
            GameFileUtility.saveFullGameState(currentGame);
            return new Result(true, target.getUsername() + " accepted.");
        }
        currentGame.sendSystemNotificationToUser(target, "Diplomacy",
            currentUser.getUsername() + " offers you a " + type.name().toLowerCase()
                + ". Answer in Extra > Diplomacy within " + com.yourname.got.logic.system.Diplomacy.PROPOSAL_DAYS + " game days.");
        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, "Offer sent to " + target.getUsername() + ".");
    }

    public Result diplomacyRespond(GameUser from, boolean accept) {
        if (isGameStateInvalid() || from == null) return new Result(false, "Invalid player.");
        if (!diplomacyEnabled()) return new Result(false, "Diplomacy is switched off.");
        String err = currentGame.getDiplomacy().respond(from.getId(), currentUser.getId(), accept,
            currentGame.getGameTime().getDay());
        if (err != null) return new Result(false, err);
        currentGame.sendSystemNotificationToUser(from, "Diplomacy",
            currentUser.getUsername() + (accept ? " accepted" : " refused") + " your offer.");
        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, accept ? "Agreement made." : "Offer refused.");
    }

    public Result diplomacyCancel(GameUser other) {
        if (isGameStateInvalid() || other == null) return new Result(false, "Invalid player.");
        boolean had = currentGame.getDiplomacy().cancel(currentUser.getId(), other.getId(),
            currentGame.getGameTime().getDay());
        if (!had) return new Result(false, "There is no agreement to cancel.");
        currentGame.sendSystemNotificationToUser(other, "Diplomacy",
            currentUser.getUsername() + " ended the agreement with you.");
        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, "Agreement cancelled.");
    }

    /** Admin panel: value of a bot/event setting. */
    public int adminGetFeature(String key) {
        return com.yourname.got.logic.system.FeatureSettings.get(key);
    }

    /** Admin panel: validates and saves several bot/event/research settings in one write. */
    public Result adminSetFeatures(java.util.Map<String, Integer> values) {
        return com.yourname.got.logic.system.FeatureSettings.setMany(values);
    }

    /** Admin panel: validates and saves a bot/event setting (takes effect at once; bots join at game start). */
    public Result adminSetFeature(String key, String value) {
        return com.yourname.got.logic.system.FeatureSettings.set(key, value);
    }

    public Result adminUpdateGlobalConfig(String key, String value) {
        boolean success = GameConfig.updateGlobalConfig(key, value);
        if (success) {
            return new Result(true, "Global config updated successfully.");
        } else {
            return new Result(false, "Failed to update global config. Invalid value?");
        }
    }
    public void saveGame(){
        GameFileUtility.updateGame(currentGame);
    }

    public Properties getGameConfig(int gameId) {
        return GameConfig.loadGameConfig(gameId);
    }

    public Result updateGameConfig(int gameId, String key, String value) {
        boolean success = GameConfig.updateGameConfig(gameId, key, value);
        if (success) {
            return new Result(true, "Game setting '" + key + "' updated successfully.");
        } else {
            return new Result(false, "Failed to update '" + key + "'. The value '" + value + "' might be invalid.");
        }
    }

    public Result trainSoldiers(SoldierType type, int quantity, Kingdom kingdom, int gameSpeed) {
        return kingdom.getArmyManager().trainSoldiers(type, quantity, kingdom, gameSpeed);
    }

    public int getSoldierCountByType(SoldierType type) {
        if (isGameStateInvalid()) return 0;

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return 0;

        ArmyManager armyManager = userInGame.getKingdom().getArmyManager();
        if (armyManager == null || armyManager.getDefenceManager() == null) return 0;
        SoldierUnit unit = armyManager.getDefenceManager().findUnitInDefenders(type);
        return (unit != null) ? unit.getCount() : 0;
    }

    /** Second info line under "Total Soldiers": camp housing and mercenary quarters. */
    public String getArmyHousingInfo() {
        GameUser user = getCurrentGameUser();
        if (user == null || user.getKingdom() == null) return "";
        Kingdom k = user.getKingdom();
        return String.format("Camps %d/%d   Mercenaries %d/%d",
            k.getTrainedSoldierCount(), k.getTotalSoldierCapacity(), k.getMercenaryCount(), k.getMercenaryCapacity());
    }

    public String getTotalSoldierInfo() {
        if (isGameStateInvalid()) return "Total: 0 / 0";

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) return "Total: 0 / 0";

        Kingdom kingdom = userInGame.getKingdom();
        ArmyManager armyManager = kingdom.getArmyManager();

        int currentCount = kingdom.getOwnedSoldierCount();
        int totalCapacity = kingdom.getTotalSoldierCapacity(); // فراخوانی متد جدید

        int mercenaries = kingdom.getMercenaryCount();
        return String.format("Total Soldiers: %d", currentCount);
    }

    public static class AttackReadySoldierInfo {
        public final SoldierType type;
        public final String displayName;
        public final int availableCount;

        public AttackReadySoldierInfo(SoldierType type, String displayName, int availableCount) {
            this.type = type;
            this.displayName = displayName;
            this.availableCount = availableCount;
        }
    }

    public PreAttackBriefingInfo getBriefingInfo(GameUser defender) {
        if (isGameStateInvalid()) return null;
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || defender == null) return null;

        Kingdom attackerKingdom = userInGame.getKingdom();
        Kingdom defenderKingdom = defender.getKingdom();

        int soldierCount = defenderKingdom.getArmyManager().getTotalSoldier();
        String vagueSoldiers = (soldierCount == 0) ? "No Soldiers" : (soldierCount < 50) ? "Light Garrison" : (soldierCount < 200) ? "Medium Garrison" : "Heavy Garrison";
        int resourceSum = defenderKingdom.getTotalResources("WOOD") + defenderKingdom.getTotalResources("FOOD");
        String vagueResources = (resourceSum < 1000) ? "Low Stockpiles" : "Significant Stockpiles";

        return new PreAttackBriefingInfo(
            vagueSoldiers, vagueResources, attackerKingdom.hasSpy(),
            soldierCount, defenderKingdom.getTotalResources("WOOD"), defenderKingdom.getTotalResources("FOOD"),
            defenderKingdom.getArmyManager().getDefenceManager().getDefenseComplex().getTotalHealth(AttackType.GROUND),


            defenderKingdom.getUserMap().getMapGrid()
        );
    }

    public Result useSpy() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");

        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) {
            return new Result(false, "Internal error: Could not load fresh user state.");
        }
        Kingdom attackerKingdom = userInGame.getKingdom();

        if (attackerKingdom.getType() == KingdomType.DRAGONSTONE && attackerKingdom.hasSpy()) {
            Hero dragons = attackerKingdom.getHero();
            SoldierUnit realSpy = attackerKingdom.getArmyManager().getDefenceManager().findUnitInDefenders(SoldierType.SPY);
            if (realSpy == null || realSpy.getCount() <= 0) {
                if (dragons != null && dragons.isAvailable()) {
                    // The dragons scout the enemy and come back: nothing is consumed.
                    return new Result(true, "Your dragons scouted the enemy lands! Intelligence is now clear.");
                }
                return new Result(false, "Your dragons are not available right now (busy or fallen).");
            }
        }

        if (attackerKingdom.hasSpy()) {
            SoldierUnit spyUnit = attackerKingdom.getArmyManager().getDefenceManager().findUnitInDefenders(SoldierType.SPY);
            if (spyUnit != null) {
                spyUnit.removeSoldier(spyUnit.getSoldiers().getHead().data);
                if (spyUnit.getCount() == 0) {
                    attackerKingdom.getArmyManager().getDefenceManager().getDefenders().remove(spyUnit);
                }
                // A used spy must also leave the total army count (it stayed at 56 before).
                attackerKingdom.getArmyManager().reconcileAllSoldiers();
                GameFileUtility.saveFullGameState(currentGame);
                return new Result(true, "Spy deployed! Intelligence is now clear.");
            }
        }
        return new Result(false, "You have no spies available.");
    }


    public Result checkSeaAttackFeasibility() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");
        Kingdom kingdom = currentGame.getUser(currentUser.getId()).getKingdom();

        if (!kingdom.hasPort()) {
            return new Result(false, "You need a Port to launch Sea Attacks.");
        }
        BattleShip ship = kingdom.getBattleShip();
        if (ship == null) {
            return new Result(false, "You need to rent a ship from 'Fleet Management' for Sea Attacks.");
        }
        if (!ship.isAvailable()) {
            return new Result(false, "Your ship is currently on a mission and not available.");
        }
        return new Result(true, "Sea attack is possible.");
    }

    public Result checkGroundAttackFeasibility() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");
        Kingdom kingdom = currentGame.getUser(currentUser.getId()).getKingdom();

        if (kingdom.getType() == KingdomType.IRON_ISLANDS) {
            return new Result(false, "Iron Islands cannot perform Ground Attacks.");
        }
        return new Result(true, "Ground attack is possible.");
    }


    public MyLinkedList<AttackTargetInfo> getPossibleAttackTargets(AttackType attackType) {
        MyLinkedList<AttackTargetInfo> targets = new MyLinkedList<>();
        if (isGameStateInvalid()) return targets;

        GameUser attacker = currentGame.getUser(currentUser.getId());
        for (GameUser user : currentGame.getUsers()) {
            if (user == null || user.getId() == attacker.getId()) continue;

            Result canAttackResult = currentGame.getRulesManager().canAttack(attacker, user, attackType);

            if (canAttackResult.isSuccess()) {
                int distance;
                if (attackType == AttackType.SEA)
                    distance = currentGame.seaDistance(attacker, user);
                else
                    distance = currentGame.GroundDistance(attacker, user);

                Double speedMov = (attackType == AttackType.SEA && attacker.getKingdom().getBattleShip() != null) ?
                    attacker.getKingdom().getBattleShip().Speed :
                    attacker.getKingdom().getMarchSpeed();

                int durationSeconds = currentGame.getGameTime().getTimeForDistance(distance, speedMov);

                String displayText = String.format("%s (%s) - ETA: %ds",
                    user.getUsername(), user.getKingdom().getType().name(), durationSeconds);
                targets.add(new AttackTargetInfo(user, displayText));
            }
        }
        return targets;
    }

    public MyLinkedList<AttackReadySoldierInfo> getAvailableAttackSoldiers() {
        MyLinkedList<AttackReadySoldierInfo> soldierInfos = new MyLinkedList<>();
        if (isGameStateInvalid()) return soldierInfos;

        Kingdom kingdom = currentGame.getUser(currentUser.getId()).getKingdom();
        ArmyManager armyManager = kingdom.getArmyManager();
        DefenceManager defenceManager = armyManager.getDefenceManager();
        SoldierType[] types = {SoldierType.INFANTRY, SoldierType.ARCHER, SoldierType.CAVALRY, SoldierType.SPY};

        for (SoldierType type : types) {
            SoldierUnit unit = defenceManager.findUnitInDefenders(type);
            int availableCount = (unit != null) ? unit.getCount() : 0;
            if (availableCount > 0) {
                String displayName = KingdomSoldierRegistry.resolveDisplayName(kingdom.getType(), type);
                soldierInfos.add(new AttackReadySoldierInfo(type, displayName, availableCount));
            }
        }
        return soldierInfos;
    }

    private SoldierUnit findUnitByType(MyLinkedList<SoldierUnit> unitList, SoldierType type) {
        for (SoldierUnit unit : unitList) {
            if (unit.getType() == type) {
                return unit;
            }
        }
        return null;
    }
    public Result launchAttack(GameUser target, AttackType attackType, Map<SoldierType, Integer> troopCounts, MyLinkedList<SoldierType> formation) {
        // 1. اعتبارسنجی ورودی‌ها
        if (isGameStateInvalid() || target == null || troopCounts.isEmpty() || formation == null || formation.isEmpty()) {
            return new Result(false, "Invalid attack parameters.");
        }

        GameUser attacker = currentGame.getUser(currentUser.getId());
        Kingdom kingdom = attacker.getKingdom();

        // 1b. Validate EVERYTHING before a single soldier leaves the army. Soldiers used to be taken first, so any
        // error afterwards (not enough of one type, bad formation) made the already packaged soldiers vanish.
        Result rules = currentGame.getRulesManager().canAttack(attacker, target, attackType);
        if (!rules.isSuccess()) return rules;

        int totalSent = 0;
        for (Map.Entry<SoldierType, Integer> entry : troopCounts.entrySet()) {
            int wanted = entry.getValue() == null ? 0 : entry.getValue();
            if (wanted <= 0) {
                return new Result(false, "Every unit you send must have a positive number of soldiers.");
            }
            SoldierUnit home = kingdom.getArmyManager().getDefenceManager().findUnitInDefenders(entry.getKey());
            int available = (home != null) ? home.getCount() : 0;
            if (wanted > available) {
                return new Result(false, "Not enough " + entry.getKey().name() + ": you have " + available + ", you chose " + wanted + ".");
            }
            totalSent += wanted;
        }
        if (formation.getSize() != troopCounts.size()) {
            return new Result(false, "The formation must contain exactly the units you send.");
        }
        for (SoldierType inFormation : formation) {
            if (!troopCounts.containsKey(inFormation)) {
                return new Result(false, "The formation contains " + inFormation + " which you are not sending.");
            }
        }
        if (attackType == AttackType.SEA) {
            BattleShip ship = kingdom.getBattleShip();
            if (ship == null) {
                return new Result(false, "You need a ship for a sea attack.");
            }
            // The ship's capacity was displayed but never enforced.
            if (totalSent > ship.soldiersCapacity) {
                return new Result(false, "Your ship carries at most " + ship.soldiersCapacity + " soldiers (you chose " + totalSent + ").");
            }
        }

        // 2. بسته‌بندی ارتش (بدون تغییر)
        MyLinkedList<SoldierUnit> attackUnitsList = new MyLinkedList<>();
        for (Map.Entry<SoldierType, Integer> entry : troopCounts.entrySet()) {
            SoldierUnit packagedUnit = kingdom.getArmyManager().removeAndPackageSoldiers(entry.getKey(), entry.getValue());
            if (packagedUnit == null) {
                restorePackagedTroops(kingdom, attackUnitsList);
                return new Result(false, "Internal Error: Not enough " + entry.getKey().name() + " available during packaging.");
            }
            attackUnitsList.add(packagedUnit);
        }

        // 3. **** بخش حیاتی و بازنویسی شده: چینش آرایش نهایی ****
        // از یک Map برای دسترسی سریع و امن به یگان‌ها استفاده می‌کنیم
        Map<SoldierType, SoldierUnit> unitMap = new HashMap<>();
        for (SoldierUnit unit : attackUnitsList) {
            unitMap.put(unit.getType(), unit);
        }

        SoldierUnit[] attackingForce = new SoldierUnit[formation.getSize()];
        int i = 0;
        // ** به جای `for (int i=...; formation.get(i))` از حلقه for-each استفاده می‌کنیم **
        // این روش باگ کلاس MyLinkedList شما را دور می‌زند و کرش نمی‌کند
        for (SoldierType typeInFormation : formation) {
            SoldierUnit unit = unitMap.get(typeInFormation);
            if (unit == null) {
                restorePackagedTroops(kingdom, attackUnitsList);
                return new Result(false, "Internal Error: Mismatch in formation logic for type " + typeInFormation);
            }
            attackingForce[i] = unit;
            i++;
        }
        // **** پایان بخش بازنویسی شده ****

        // 4. محاسبه زمان و ایجاد مأموریت (بدون تغییر)
        int distance;
        double speedMov;
        if (attackType == AttackType.SEA) {
            distance = currentGame.seaDistance(attacker, target);
            speedMov = (kingdom.getBattleShip() != null) ? kingdom.getBattleShip().Speed : 1.0;
        } else {
            distance = currentGame.GroundDistance(attacker, target);
            speedMov = kingdom.getMarchSpeed();
        }
        int durationSeconds = currentGame.getGameTime().getTimeForDistance(distance, speedMov);

        Mission mission;
        try {
            mission = new Mission(attacker, target, attackingForce, durationSeconds, attackType);
            currentGame.getMissions().add(mission);

            // 5. به‌روزرسانی وضعیت کشتی (بدون تغییر)
            if (attackType == AttackType.SEA && kingdom.getBattleShip() != null) {
                kingdom.getBattleShip().setAvailable(false);
            }
        } catch (RuntimeException ex) {
            // The soldiers are already out of the army: never lose them because the mission could not be created.
            ex.printStackTrace();
            restorePackagedTroops(kingdom, attackUnitsList);
            target.setInFight(false);
            return new Result(false, "The attack could not be launched. Your soldiers stayed home.");
        }

        // 6. ذخیره وضعیت و ارسال پیام موفقیت (بدون تغییر)
        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, "Attack launched! ETA: " + Time.formatSeconds(durationSeconds));
    }
    /**
     * Totals of the army the player is about to send (the first N soldiers of every chosen unit, exactly the ones
     * launchAttack will take), the travel time and the rules that will apply. Does not change anything.
     */
    public AttackPreview getAttackPreview(GameUser target, AttackType attackType, Map<SoldierType, Integer> troopCounts) {
        if (isGameStateInvalid() || target == null || troopCounts == null) return null;
        GameUser attacker = currentGame.getUser(currentUser.getId());
        if (attacker == null || attacker.getKingdom() == null) return null;
        Kingdom kingdom = attacker.getKingdom();

        int soldiers = 0;
        long power = 0;
        long health = 0;
        for (Map.Entry<SoldierType, Integer> entry : troopCounts.entrySet()) {
            SoldierUnit home = kingdom.getArmyManager().getDefenceManager().findUnitInDefenders(entry.getKey());
            if (home == null || entry.getValue() == null) continue;
            int left = entry.getValue();
            for (Soldier soldier : home.getSoldiers()) {
                if (left <= 0) break;
                if (soldier == null) continue;
                soldiers++;
                power += soldier.getAttackPower();
                health += soldier.getHealth();
                left--;
            }
        }

        Hero hero = kingdom.getHero();
        boolean heroLeads = hero != null && hero.isAvailable();
        if (heroLeads) power = Math.round(power * Mission.heroAttackBuff()); // the same buff Mission applies to the whole army

        String note = null;
        if (target.getKingdom() != null) {
            if (target.getKingdom().getType() == KingdomType.WINTERFELL) {
                note = "Winterfell: your soldiers lose " + Mission.winterfellHealthPercentForDisplay() + "% health.";
            } else if (target.getKingdom().getType() == KingdomType.EYRIE) {
                note = "Eyrie: your attack power is reduced by " + Mission.eyrieAttackPercentForDisplay() + "%.";
            }
        }

        int distance;
        double speedMov;
        if (attackType == AttackType.SEA) {
            distance = currentGame.seaDistance(attacker, target);
            speedMov = (kingdom.getBattleShip() != null) ? kingdom.getBattleShip().Speed : 1.0;
        } else {
            distance = currentGame.GroundDistance(attacker, target);
            speedMov = kingdom.getMarchSpeed();
        }
        int eta = currentGame.getGameTime().getTimeForDistance(distance, speedMov);

        return new AttackPreview(soldiers, (int) Math.min(Integer.MAX_VALUE, power),
            (int) Math.min(Integer.MAX_VALUE, health), eta, heroLeads, note);
    }

    /** Puts soldiers that were packaged for an attack back into the army (used when a launch fails half-way). */
    private void restorePackagedTroops(Kingdom kingdom, MyLinkedList<SoldierUnit> packagedUnits) {
        if (kingdom == null || packagedUnits == null || packagedUnits.isEmpty()) return;
        SoldierUnit[] units = new SoldierUnit[packagedUnits.getSize()];
        packagedUnits.fillArray(units);
        kingdom.getArmyManager().returnSurvivorsToDefence(units);
        kingdom.getArmyManager().reconcileAllSoldiers();
    }

    public MyLinkedList<BattleHistoryInfo> getBattleHistory() {
        MyLinkedList<BattleHistoryInfo> historyList = new MyLinkedList<>();
        if (isGameStateInvalid() || currentGame.getBATTLE_LOG_FILE() == null) {
            return historyList;
        }

        String logFilePath = currentGame.getBATTLE_LOG_FILE();
        String currentUsername = currentUser.getUsername();

        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length < 10) continue;

                String attackerName = parts[0];
                String defenderName = parts[1];
                int attackerStars = Integer.parseInt(parts[2]);
                String troopComp = parts[3];
                String timestamp = parts[4];
                String replayPath = parts[5];
                int wood = Integer.parseInt(parts[6]);
                int stone = Integer.parseInt(parts[7]);
                int food = Integer.parseInt(parts[8]);
                int coin = Integer.parseInt(parts[9]);
                String type = (attackerStars == 3) ? "CONQUEST" : "RAID";

                if (attackerName.equals(currentUsername)) {
                    String result = (attackerStars > 0) ? "Victory" : "Failure";
                    String loot = String.format("Loot: [W: +%d] [S: +%d] [F: +%d] [C: +%d]", wood, stone, food, coin);
                    historyList.add(new BattleHistoryInfo(true, defenderName, attackerStars, result, loot, troopComp, timestamp, replayPath , type));

                } else if (defenderName.equals(currentUsername)) {
                    int defenseStars = 3 - attackerStars;
                    String result = (attackerStars == 3) ? "Total Defeat" : "Defense";
                    String loot = String.format("Lost: [W: -%d] [S: -%d] [F: -%d] [C: -%d]", wood, stone, food, coin);
                    historyList.add(new BattleHistoryInfo(false, attackerName, defenseStars, result, loot, "N/A", timestamp, replayPath , type));
                }
            }
        } catch (Exception e) {
            GameLog.error("Error reading or parsing detailed battle log: " + e.getMessage());
        }

        return historyList;
    }

    // این متد را در فایل GameMediator.java به طور کامل جایگزین کنید
    public ReplayData loadReplayLog(String replayFilePath) {
        if (replayFilePath == null || replayFilePath.equals("N/A")) {
            GameLog.error("ERROR [Mediator]: Replay file path is null or N/A.");
            return null;
        }

        GameLog.info("LOG [Mediator]: Attempting to load replay from relative path: " + replayFilePath);

        File replayFile = new File(replayFilePath);
        String absolutePath = replayFile.getAbsolutePath();
        GameLog.info("LOG [Mediator]: Resolved to absolute path: " + absolutePath);

        if (!replayFile.exists()) {
            GameLog.error("FATAL ERROR [Mediator]: Replay file does NOT exist at the resolved path!");
            return null;
        }

        GameLog.info("SUCCESS [Mediator]: Replay file found. Attempting to deserialize...");

        try (FileInputStream fis = new FileInputStream(replayFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            Object obj = ois.readObject();
            if (obj instanceof ReplayData) {
                ReplayData data = (ReplayData) obj;
                GameLog.info("LOAD DEBUG: Loaded ReplayData with events = " +
                    (data.getEvents() != null ? data.getEvents().size() : "null"));
                return data;

            } else {
                GameLog.error("ERROR [Mediator]: Unexpected object in replay file: "
                    + obj.getClass().getName());
            }

        } catch (Exception e) {
            GameLog.error("FATAL ERROR [Mediator]: An exception occurred while reading the replay file.");
            e.printStackTrace();
        }

        return null;
    }


    public DefenseUpgradeInfo getDefenseComplexUpgradeInfo() {
        if (isGameStateInvalid()) return new DefenseUpgradeInfo(false, 0, 0, 0, 0, 0, "Invalid game state.");

        Kingdom kingdom = currentUser.getKingdom();
        DefenseComplex defenseComplex = kingdom.getArmyManager().getDefenceManager().getDefenseComplex();
        Gate gate = defenseComplex.getGate();
        Port port = defenseComplex.getPort();
        MyLinkedList<WallsOfGate> walls = defenseComplex.getWall();

        if ((gate != null && gate.getStatus() == Status.upgrading) || (port != null && port.getStatus() == Status.upgrading)) {
            return new DefenseUpgradeInfo(false, 0, 0, 0, 0, 0, "A part of the defense complex is already upgrading.");
        }
        if (walls != null) {
            for (WallsOfGate wall : walls) {
                if (wall != null && wall.getStatus() == Status.upgrading) {
                    return new DefenseUpgradeInfo(false, 0, 0, 0, 0, 0, "A wall is already upgrading.");
                }
            }
        }
        int currentLevel = -1;
        if (gate != null) currentLevel = gate.getLevel();
        else if (port != null) currentLevel = port.getLevel();
        else if (walls != null && !walls.isEmpty()) currentLevel = walls.get(0).getLevel();

        if (currentLevel == -1) return new DefenseUpgradeInfo(false, 0, 0, 0, 0, 0, "No defense components found.");


        int nextLevel = currentLevel + 1;
        int totalWoodCost = 0, totalStoneCost = 0, totalCoinCost = 0, totalTime = 0;
        int wallCount = (walls != null) ? walls.size() : 0;

        if (gate != null) {
            totalWoodCost += BuildingManager.getUpgradeCost("GATE", nextLevel, 1, "WOOD", gate);
            totalStoneCost += BuildingManager.getUpgradeCost("GATE", nextLevel, 1, "STONE", gate);
            totalCoinCost += BuildingManager.getUpgradeCost("GATE", nextLevel, 1, "COIN", gate);
            totalTime += GameBalance.getMilitaryBuildTime("GATE", nextLevel);
        }

        if (walls != null) {
            for (WallsOfGate wall : walls) {
                if (wall != null) {
                    totalWoodCost += BuildingManager.getUpgradeCost("WALLSOFGATE", nextLevel, wallCount, "WOOD", wall);
                    totalStoneCost += BuildingManager.getUpgradeCost("WALLSOFGATE", nextLevel, wallCount, "STONE", wall);
                    totalCoinCost += BuildingManager.getUpgradeCost("WALLSOFGATE", nextLevel, wallCount, "COIN", wall);
                }
            }
        }

        if (port != null) {
            totalWoodCost += BuildingManager.getUpgradeCost("PORT", nextLevel, 1, "WOOD", port);
            totalStoneCost += BuildingManager.getUpgradeCost("PORT", nextLevel, 1, "STONE", port);
            totalCoinCost += BuildingManager.getUpgradeCost("PORT", nextLevel, 1, "COIN", port);
            totalTime += GameBalance.getMilitaryBuildTime("PORT", nextLevel);
        }

        return new DefenseUpgradeInfo(true, nextLevel, totalWoodCost, totalStoneCost, totalCoinCost, totalTime, "Ready to upgrade.");
    }

    public Result executeDefenseComplexUpgrade() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");

        DefenseUpgradeInfo info = getDefenseComplexUpgradeInfo();
        if (!info.canUpgrade) return new Result(false, info.statusMessage);

        Kingdom kingdom = currentUser.getKingdom();
        if (!kingdom.canAffordResources(info.totalWoodCost, info.totalStoneCost, info.totalCoinCost, 0)) {
            return new Result(false, "Not enough resources for the upgrade.");
        }

        kingdom.detuctResources(info.totalWoodCost, info.totalStoneCost, info.totalCoinCost, 0);

        DefenseComplex defenseComplex = kingdom.getArmyManager().getDefenceManager().getDefenseComplex();
        // Safely upgrade components
        if (defenseComplex.getGate() != null) defenseComplex.getGate().upgrade(currentGame.getSpeed());
        if (defenseComplex.getPort() != null) defenseComplex.getPort().upgrade(currentGame.getSpeed());
        if(defenseComplex.getWall() != null) {
            for (WallsOfGate wall : defenseComplex.getWall()) {
                if(wall != null) wall.upgrade(currentGame.getSpeed());
            }
        }

        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, "Defense complex upgrade has started.");
    }

    public DefenseRepairInfo getDefenseComplexRepairInfo() {
        if (isGameStateInvalid()) return new DefenseRepairInfo(false, 0, 0, 0, 0);

        Kingdom kingdom = currentUser.getKingdom();
        DefenseComplex defenseComplex = kingdom.getArmyManager().getDefenceManager().getDefenseComplex();
        Gate gate = defenseComplex.getGate();
        Port port = defenseComplex.getPort();
        MyLinkedList<WallsOfGate> walls = defenseComplex.getWall();
        int totalWoodCost = 0, totalStoneCost = 0, totalCoinCost = 0, totalTime = 0;
        boolean needsRepair = false;

        if (gate != null && gate.isDestroyed() && gate.getStatus() != Status.underRepair) {
            needsRepair = true;
            totalWoodCost += BuildingManager.getRepairCost("GATE", gate.getLevel(), 1, "WOOD", gate);
            totalStoneCost += BuildingManager.getRepairCost("GATE", gate.getLevel(), 1, "STONE", gate);
            totalCoinCost += BuildingManager.getRepairCost("GATE", gate.getLevel(), 1, "COIN", gate);
            totalTime += GameBalance.getMilitaryRepairTime("GATE", gate.getLevel());
        }

        if (walls != null) {
            for (WallsOfGate wall : walls) {
                if (wall != null && wall.isDestroyed() && wall.getStatus() != Status.underRepair) {
                    needsRepair = true;
                    totalWoodCost += BuildingManager.getRepairCost("WALLSOFGATE", wall.getLevel(), 1, "WOOD", wall);
                    totalStoneCost += BuildingManager.getRepairCost("WALLSOFGATE", wall.getLevel(), 1, "STONE", wall);
                    totalCoinCost += BuildingManager.getRepairCost("WALLSOFGATE", wall.getLevel(), 1, "COIN", wall);
                    totalTime += GameBalance.getMilitaryRepairTime("WALLSOFGATE", wall.getLevel());
                }
            }
        }

        if (port != null && port.isDestroyed() && port.getStatus() != Status.underRepair) {
            needsRepair = true;
            totalWoodCost += BuildingManager.getRepairCost("PORT", port.getLevel(), 1, "WOOD", port);
            totalStoneCost += BuildingManager.getRepairCost("PORT", port.getLevel(), 1, "STONE", port);
            totalCoinCost += BuildingManager.getRepairCost("PORT", port.getLevel(), 1, "COIN", port);
            totalTime += GameBalance.getMilitaryRepairTime("PORT", port.getLevel());
        }

        return new DefenseRepairInfo(needsRepair, totalWoodCost, totalStoneCost, totalCoinCost, totalTime);
    }

    public Result executeDefenseComplexRepair() {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");

        DefenseRepairInfo info = getDefenseComplexRepairInfo();
        if (!info.needsRepair) return new Result(false, "The defense complex does not need repairs.");

        Kingdom kingdom = currentUser.getKingdom();
        if (!kingdom.canAffordResources(info.totalWoodCost, info.totalStoneCost, info.totalCoinCost, 0)) {
            return new Result(false, "Not enough resources for the repair.");
        }

        kingdom.detuctResources(info.totalWoodCost, info.totalStoneCost, info.totalCoinCost, 0);

        DefenseComplex defenseComplex = kingdom.getArmyManager().getDefenceManager().getDefenseComplex();
        Gate gate = defenseComplex.getGate();
        Port port = defenseComplex.getPort();
        MyLinkedList<WallsOfGate> walls = defenseComplex.getWall();

        if (gate != null && gate.isDestroyed()) gate.repair(currentGame.getSpeed());
        if (port != null && port.isDestroyed()) port.repair(currentGame.getSpeed());
        if (walls != null) {
            for (WallsOfGate wall : walls) {
                if (wall != null && wall.isDestroyed()) wall.repair(currentGame.getSpeed());
            }
        }

        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, "Defense complex repair has started.");
    }

    public ArcherDeploymentInfo getArcherDeploymentInfo() {
        if (isGameStateInvalid()) return new ArcherDeploymentInfo(0, 0);
        GameUser userInGame = currentGame.getUser(currentUser.getId());

        if (userInGame == null) return new ArcherDeploymentInfo(0, 0);
        Kingdom kingdom = userInGame.getKingdom();

        DefenceManager defenceManager = kingdom.getArmyManager().getDefenceManager();
        SoldierUnit fieldArchers = defenceManager.findUnitInDefenders(SoldierType.ARCHER);
        SoldierUnit wallArchers = defenceManager.getDefenseComplex().getDefensiveArchers();

        int available = (fieldArchers != null) ? fieldArchers.getCount() : 0;
        int onWall = (wallArchers != null) ? wallArchers.getCount() : 0;

        return new ArcherDeploymentInfo(available, onWall);
    }
// In GameMediator.java

    // The method now accepts the defeated user's ID
    private final java.util.Set<Integer> defeatPanelShownFor = new java.util.HashSet<>();

    public void triggerDefeatPanelDisplay(int defeatedUserId) {
        // Only trigger the panel if the currently logged-in user is the one who was defeated.
        if (panelPresenter != null && currentUser != null && currentUser.getId() == defeatedUserId) {
            // The game tick calls this on EVERY tick while the castle is destroyed: open the panel once only.
            if (!defeatPanelShownFor.add(defeatedUserId)) return;
            Gdx.app.postRunnable(() -> panelPresenter.showDefeatPanel());
        }
    }
    public Result deployArchersToWall(int quantity) {
        if (isGameStateInvalid()) {
            return new Result(false, "Invalid game state.");
        }

        // --- FIX: همیشه از آبجکت اصلی و به‌روز بازی استفاده می‌کنیم ---
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Kingdom kingdom = userInGame.getKingdom();
        ArmyManager armyManager = kingdom.getArmyManager();
        DefenseComplex defenseComplex = armyManager.getDefenceManager().getDefenseComplex();

        // ۱. اعتبارسنجی مجدد در سمت منطق
        ArcherDeploymentInfo info = getArcherDeploymentInfo();
        if (quantity <= 0) {
            return new Result(false, "Quantity must be a positive number.");
        }
        if (quantity > info.availableArchers) {
            return new Result(false, "Not enough available archers to deploy.");
        }

        // ۲. --- استفاده از متد صحیح برای انتقال سربازان ---
        // این متد سربازان را از تمام لیست‌های مربوطه (allSoldiers و defenders) حذف می‌کند.
        SoldierUnit archersToMove = armyManager.removeAndPackageSoldiers(SoldierType.ARCHER, quantity);

        if (archersToMove == null) {
            return new Result(false, "An internal error occurred during soldier transfer.");
        }

        // ۳. افزودن سربازان منتقل شده به پادگان دیوار
        for (Soldier archer : archersToMove.getSoldiers()) {
            defenseComplex.addDefensiveArcherUnit(archer);
        }

        GameFileUtility.saveFullGameState(currentGame);
        return new Result(true, quantity + " archers were successfully deployed to the wall.");
    }

    public Result helpTheWall(int wood, int stone, int coin, int food) {
        if (isGameStateInvalid()) {
            return new Result(false, "Invalid game state.");
        }

        Wall wall = currentGame.getWall();
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        Kingdom kingdom = userInGame.getKingdom();

        if (wall == null || wall.isDestroyed()) {
            return new Result(false, "The Wall is destroyed and cannot be reinforced.");
        }
        if (wood < 0 || stone < 0 || coin < 0 || food < 0) {
            return new Result(false, "ERROR: Resource amounts cannot be negative.");
        }
        if (wood == 0 && stone == 0 && coin == 0 && food == 0) {
            return new Result(false, "You must contribute at least one resource.");
        }
        if (!kingdom.canAffordResources(wood, stone, coin, food)) {
            return new Result(false, "ERROR: You do not have enough resources for this contribution.");
        }

        Result result = currentGame.helpWall(kingdom, wood, stone, coin, food);


        if (result.isSuccess()) {
            GameFileUtility.saveFullGameState(currentGame);
        }

        return result;
    }

    public WallInfo getWallInfo() {
        if (isGameStateInvalid() || currentGame.getWall() == null) {
            return new WallInfo("N/A", 0, 0, true);
        }
        Wall wall = currentGame.getWall();
        String health = String.format("%d / %d", wall.getHealth(), wall.getInitialHealth());
        return new WallInfo(health, wall.getLastAttackDay(), wall.getTotalHelpCount(), wall.isDestroyed());
    }

    public MyLinkedList<ContributionInfo> getWallContributions() {
        MyLinkedList<ContributionInfo> dtoList = new MyLinkedList<>();
        if (isGameStateInvalid() || currentGame.getWall() == null) {
            return dtoList;
        }

        MyLinkedList<Wall.ContributionRecord> records = currentGame.getWall().getContributions();
        if (records != null) {
            for (Wall.ContributionRecord record : records) {
                if (record != null && record.getKingdom() != null) {
                    String kingdomName = record.getKingdom().getType().toString();
                    String userName = record.getKingdom().getOwner().getUsername();
                    dtoList.add(new ContributionInfo(kingdomName, userName, record.getAmount()));
                }
            }
        }
        return dtoList;
    }

    public WhiteWalkerInfo getWhiteWalkerInfo() {
        if (isGameStateInvalid() || currentGame.getWhiteWalkerManager() == null) {
            return new WhiteWalkerInfo(0, 0, 0, "INACTIVE", false);
        }
        WhiteWalkerManager wwm = currentGame.getWhiteWalkerManager();
        return new WhiteWalkerInfo(
            wwm.getStartDay(),
            wwm.getFinalDay(),
            wwm.getAttackInterval(),
            wwm.getStrategy().name(),
            wwm.isFinalWaveTriggered()
        );
    }

    public int getCurrentDragonGlass() {
        if (isGameStateInvalid()) {
            return 0;
        }
        GameUser userInGame = currentGame.getUser(currentUser.getId());
        if (userInGame == null || userInGame.getKingdom() == null) {
            return 0;
        }
        return userInGame.getKingdom().getDragonGlass();
    }

    // In file: GameMediator.java

    public MyLinkedList<NoticeInfo> getNoticesForCurrentUser() {
        MyLinkedList<NoticeInfo> allNotices = new MyLinkedList<>();
        if (isGameStateInvalid()) return allNotices;

        MyLinkedList<String> adminBlocks = Admin.readAllLetters();
        int currentDay = currentGame.getGameTime().getDay();

        for (String block : adminBlocks) {
            int noticeDay = Admin.extractDay(block);
            if (noticeDay > 0 && noticeDay <= currentDay) {
                String title = Admin.extractTitle(block);
                String body = Admin.extractBody(block);
                // FIX #1: Added 'null' for the receiver argument in ADMIN notices.
                allNotices.add(new NoticeInfo(
                    block.hashCode(),
                    title,
                    "ADMIN",
                    null, // Admin notices have no specific receiver
                    body,
                    "Day " + noticeDay,
                    false,
                    true
                ));
            }
        }

        Post postOffice = currentGame.getPostOffice();
        if (postOffice != null) {
            for (Notice notice : postOffice) {
                if (notice.receiver == null || notice.receiver.getId() == currentUser.getId()) {
                    if (LocalDateTime.now().isAfter(notice.sendTime.plusSeconds(notice.durationSeconds))) {
                        // FIX #2: Added 'notice.receiver' to the constructor call.
                        allNotices.add(new NoticeInfo(
                            notice.letterNumber,
                            notice.Title,
                            notice.getSenderUsername(),
                            notice.receiver, // Pass the receiver object from the original notice
                            notice.note,
                            notice.sendTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            notice.isRead(),
                            notice.sender == null
                        ));
                    }
                }
            }
        }

        return allNotices;
    }

    public MyLinkedList<MessageRecipientInfo> getAvailableRecipients() {
        MyLinkedList<MessageRecipientInfo> recipients = new MyLinkedList<>();
        if (isGameStateInvalid()) return recipients;

        for (GameUser user : currentGame.getUsers()) {
            if (user != null && !BotUser.isBot(user) && user.getId() != currentUser.getId() &&
                user.isActive() && !user.isInFight() &&
                user.getKingdom() != null) {

                String displayText = String.format("%s (%s)", user.getUsername(), user.getKingdom().getType().name());
                recipients.add(new MessageRecipientInfo(user.getId(), displayText));
            }
        }
        return recipients;
    }

    public Result sendMessage(int recipientId, String title, String body) {
        if (isGameStateInvalid()) return new Result(false, "Invalid game state.");
        if (title.trim().isEmpty() || body.trim().isEmpty()) {
            return new Result(false, "Title and body cannot be empty.");
        }

        GameUser sender = currentGame.getUser(currentUser.getId());
        GameUser recipient = currentGame.getUser(recipientId);

        if (recipient == null) {
            return new Result(false, "Recipient not found or is no longer available.");
        }

        Kingdom senderKingdom = sender.getKingdom();
        CrowTower crowTower = senderKingdom.getCrowTower();

        if (crowTower == null) {
            return new Result(false, "You must build a Crow Tower before you can send messages.");
        }

        int distance = currentGame.GroundDistance(sender, recipient);
        int durationSeconds = currentGame.getGameTime().getTimeForDistance(distance, crowTower.getCrowSpeed());

        Notice newNotice = new Notice(durationSeconds, sender, recipient, title, body);
        currentGame.getPostOffice().add(newNotice);

        GameFileUtility.saveFullGameState(currentGame);

        return new Result(true, "Message sent! It will arrive in " + durationSeconds + " seconds.");
    }

    public void markNoticeAsRead(int noticeId) {
        if (isGameStateInvalid() || currentGame.getPostOffice() == null) return;

        for(Notice notice : currentGame.getPostOffice()){
            if(notice.letterNumber == noticeId){
                notice.markAsRead();
                break;
            }
        }
    }

    // File: GameMediator.java

    // >> ADD THIS NEW METHOD TO THE CLASS <<
    public void triggerUIRefresh(int userId) {
        // Only trigger the refresh if the currently logged-in user is the one whose state changed.
        if (panelPresenter != null && currentUser != null && currentUser.getId() == userId) {
            // Use postRunnable to ensure the UI update happens on the main rendering thread.
            Gdx.app.postRunnable(() -> panelPresenter.refreshMap());
        }
    }
    public void triggerFloatingText(int userId, Building building, String text, String color) {
        // فقط اگر کاربری که لاگین کرده همان صاحب ساختمان باشد، متن را نشان بده
        if (panelPresenter != null && currentUser != null && currentUser.getId() == userId) {

            // تبدیل مختصات جهانی ساختمان به مختصات محلی (سطر و ستون گرید)
            // ما نیاز داریم بدانیم این ساختمان در کجای گرید ۱۱x۱۱ فعلی نمایش داده شده است
            Gdx.app.postRunnable(() -> {
                try {
                    GameUser userInGame = currentGame.getUser(userId);
                    if (userInGame == null) return;

                    Kingdom kingdom = userInGame.getKingdom();
                    UserSubMap map = kingdom.getUserMap();
                    Position localPos = map.globalToLocal(building.getPosition());

                    if (localPos != null) {
                        // محاسبه آفست برای پیدا کردن ایندکس آرایه دکمه‌ها
                        // این منطق باید با GamePlayScreen.renderTilesAndUpdate هماهنگ باشد
                        int mapRows = map.getLength();
                        int mapCols = map.getWidth();
                        int GRID_SIZE = 11;

                        int offsetRow = (mapRows - GRID_SIZE) / 2;
                        int offsetCol = (mapCols - GRID_SIZE) / 2;

                        // تبدیل مختصات محلی نقشه به ایندکس آرایه tileButtons[r][c]
                        // localPos.getX() معادل Row در لاجیک شماست (طبق کدهای قبلی)
                        int r = localPos.getX() - offsetRow;
                        int c = localPos.getY() - offsetCol;

                        panelPresenter.showFloatingText(r, c, text, color);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
