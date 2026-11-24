package com.yourname.got.logic.model;


import com.yourname.got.logic.util.GameLog;
import java.io.*;
import com.yourname.got.logic.Config.GameConfig;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.login.*;
import java.util.Properties;
import java.util.Random;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.TerrainType;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Castle;
import com.yourname.got.logic.system.GameRulesManager;
import com.yourname.got.logic.util.*;
import com.yourname.got.logic.WhiteWalker.*;
import com.yourname.got.mediator.GameMediator;

public class GameOfThrones implements Serializable {
    private  static final int maxPlayer=7;
    protected  Time gameTime;
    private  boolean alert;
    protected static final long serialVersionUID = 1L;
    protected int id;
    protected  String name;
    protected  String filename;
    protected  Map map;
    protected GameUser[] User;
    protected int numberOfPlayer;
    protected int password;
    protected  boolean gameReady;
    protected GameUser owner;
    protected Bazar bazar;
    protected Post postOffice;
    protected MyLinkedList<Mission> missions;
    protected MyLinkedList<Notice>  letter;
    protected MyLinkedList<Notice>  transactions;
    private WhiteWalkerManager whiteWalkerManager;
    protected Wall wall;
    private GameRulesManager rulesManager;
    public   String BATTLE_LOG_FILE ;
    private Properties config;
    private boolean isFinished = false;
    /** The last game day a seasonal event was rolled for (see SeasonalEvents). Old saves start at 0. */
    private int lastEventDay = 0;
    MyLinkedList<PlayerState> playerStates;
    public String REPLAYS_FOLDER_PATH;

    public enum PlayerStatus {
        ACTIVE,
        DEFEATED
    }
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int userId;
        public PlayerStatus status;
        /** 0 while the player is alive; 1 for the first player to fall, 2 for the next, ... */
        public int defeatOrder = 0;
        /** Score snapshot taken at the moment the player was defeated. */
        public int finalScore = 0;

        public PlayerState(int userId) {
            this.userId = userId;
            this.status = PlayerStatus.ACTIVE;
        }
    }


    public void setBATTLE_LOG_FILE(String BATTLE_LOG_FILE) {
        this.BATTLE_LOG_FILE = BATTLE_LOG_FILE;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public int getLastEventDay() {
        return lastEventDay;
    }

    public void setLastEventDay(int day) {
        this.lastEventDay = day;
    }
    public void endGame(GameRecord.GameResult reason) {
        if (this.isFinished) return;
        this.isFinished = true;

        GameLog.info("GAME OVER: Reason: " + reason);

        // ۱. ساخت رکورد نهایی بازی
        String duration = this.gameTime.getFormattedElapsedTime();
        GameRecord finalRecord = new GameRecord(this.name, this.config, duration, reason);

        // ۲. رتبه‌بندی بازیکنان
        // Final ranking: survivors first (highest score first), then the defeated players
        // (the player who fell last ranks best among them).
        java.util.List<GameUser> survivors = new java.util.ArrayList<>();
        java.util.List<GameUser> fallen = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> survivorScores = new java.util.HashMap<>();
        for (GameUser user : this.User) {
            if (user == null) continue;
            PlayerState state = getPlayerState(user.getId());
            if (state == null) continue;
            if (state.status == PlayerStatus.ACTIVE) {
                survivors.add(user);
                survivorScores.put(user.getId(), com.yourname.got.logic.system.ScoreCalculator.calculate(user));
            } else if (state.status == PlayerStatus.DEFEATED) {
                fallen.add(user);
            }
        }
        survivors.sort((x, y) -> {
            int c = Integer.compare(survivorScores.get(y.getId()), survivorScores.get(x.getId()));
            return c != 0 ? c : x.getUsername().compareToIgnoreCase(y.getUsername());
        });
        fallen.sort((x, y) -> Integer.compare(
            getPlayerState(y.getId()).defeatOrder, getPlayerState(x.getId()).defeatOrder));

        int rank = 1;
        for (GameUser user : survivors) {
            PlayerResult.PlayerStatus finalStatus =
                (reason == GameRecord.GameResult.LAST_PLAYER_STANDING && rank == 1)
                    ? PlayerResult.PlayerStatus.WINNER : PlayerResult.PlayerStatus.SURVIVED;
            finalRecord.addPlayerResult(new PlayerResult(
                user.getUsername(), survivorScores.get(user.getId()), finalStatus, rank++));
        }
        for (GameUser user : fallen) {
            finalRecord.addPlayerResult(new PlayerResult(
                user.getUsername(), getPlayerState(user.getId()).finalScore,
                PlayerResult.PlayerStatus.DEFEATED, rank++));
        }

        try {
            for (GameUser u : this.User) {
                if (u == null || BotUser.isBot(u)) continue;
                PlayerState st = getPlayerState(u.getId());
                boolean won = reason == GameRecord.GameResult.LAST_PLAYER_STANDING
                    && !survivors.isEmpty() && survivors.get(0) == u;
                int score = survivorScores.containsKey(u.getId())
                    ? survivorScores.get(u.getId()) : (st == null ? 0 : st.finalScore);
                com.yourname.got.logic.system.Leaderboard.record(u.getUsername(), score, won);
            }
        } catch (RuntimeException e) {
            GameLog.error("Leaderboard update failed (the game still ends): " + e);
        }

        for (GameUser u : this.User) {
            if (u == null || BotUser.isBot(u)) continue; // bots are not stored in users/
            UserSystem.saveGameRecord(u, finalRecord);

            // اصلاح مهم: از آنجایی که بازی تمام شده، وضعیت IN GAME بازیکن را فالس می‌کنیم
            // تا دفعه بعد که لاگین کرد، بدون کرش مستقیم به صفحه تاریخچه یا لابی برود.
            u.setInGame(false);
            u.setGame(null);
            u.setInFight(false);
            UserSystem.updateUser(u);
        }

        // ۴. ذخیره وضعیت نهایی بازی
        GameFileUtility.saveFullGameState(this);

        // ۵. اگر کسی آنلاین است، پنل را در ترد UI نشان بده
        if (GameMediator.getInstance() != null) {
            // استفاده از postRunnable بسیار حیاتی است تا با ترد گرافیکی تداخل نداشته باشد
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                GameMediator.getInstance().triggerResultsPanelDisplay();
            });
        }
    }
    // متد بررسی پایان بازی را هم به‌روز کنید
    public void checkGameOver() {
        if (isFinished) return;

        // حذف بازیکنان غیرفعال
        checkUserActivity();

        // بررسی شکست‌خوردگان (کسانی که قلعه‌شان نابود شده)
        checkAllPlayersStatus();

        int activePlayers = 0;
        int activeHumans = 0;
        boolean hasBots = false;
        GameUser lastAlive = null;

        for (GameUser u : this.User) {
            if (u == null) continue;
            if (BotUser.isBot(u)) hasBots = true;
            PlayerState st = getPlayerState(u.getId());
            if (st != null && st.status == PlayerStatus.ACTIVE) {
                activePlayers++;
                if (!BotUser.isBot(u)) activeHumans++;
                lastAlive = u;
            }
        }

        // شرط ۱: وایت واکرها همه را کشتند
        if (activePlayers == 0) {
            endGame(GameRecord.GameResult.WHITE_WALKERS_WON);
            return;
        }

        // Every human has fallen but bots are still alive: nobody is left to play, so the game ends here
        // (otherwise bots would keep an empty game running until day 100).
        if (hasBots && activeHumans == 0) {
            endGame(GameRecord.GameResult.LAST_PLAYER_STANDING);
            return;
        }

        // شرط ۲: فقط یک نفر مانده (Last Man Standing)
        // نکته: فقط اگر موج آخر وایت واکرها شروع شده باشد یا بازی به روزهای آخر رسیده باشد
        if (activePlayers == 1 && numberOfPlayer > 1) {
            endGame(GameRecord.GameResult.LAST_PLAYER_STANDING);
            return;
        }

        // شرط ۳: روز ۱۰۰ فرا رسیده (پایان زمان)
        boolean finalWaveDone = (whiteWalkerManager == null || whiteWalkerManager.isFinalWaveTriggered())
            && !hasPendingWhiteWalkerMissions();
        if (this.getGameTime().getDay() >= getWhitewalkerFinalDay() && finalWaveDone) {
            endGame(GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT);
            return;
        }
    }


    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
        // این متد برای اطمینان از سازگاری نسخه‌های مختلف کلاس هنگام ذخیره و بازیابی است
        // با انتقال منطق ساخت فایل به GameFileUtility، دیگر نیازی به کد اضافی در اینجا نیست
        in.defaultReadObject();
    }
    // In file: GameOfThrones.java

    /** True while at least one White Walker attack is still waiting to be resolved. */
    private boolean hasPendingWhiteWalkerMissions() {
        if (missions == null) return false;
        for (Mission m : missions) {
            if (m instanceof com.yourname.got.logic.WhiteWalker.WhiteWalkerMission && !m.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    public void handlePlayerDefeat(GameUser defeatedUser) {
        if (defeatedUser == null) return;

        PlayerState state = getPlayerState(defeatedUser.getId());
        // اگر از قبل شکست خورده، کاری انجام نده
        if (state == null || state.status == PlayerStatus.DEFEATED) {
            return;
        }

        GameLog.info("⚔️ بازیکن " + defeatedUser.getUsername() + " شکست خورد.");

        // --- START OF THE DEFINITIVE FIX ---
        // ۱. وضعیت بازیکن را به طور قطعی به DEFEATED تغییر بده
        // Remember when (in which order) the player fell and what they had, for the final ranking.
        int alreadyDefeated = 0;
        for (PlayerState ps : playerStates) {
            if (ps != null && ps.status == PlayerStatus.DEFEATED) alreadyDefeated++;
        }
        state.finalScore = com.yourname.got.logic.system.ScoreCalculator.calculate(defeatedUser);
        state.defeatOrder = alreadyDefeated + 1;
        state.status = PlayerStatus.DEFEATED;

        // ۲. وضعیت قلعه را نیز (بدون آسیب زدن) به نابود شده تغییر بده تا همه جا یکپارچه باشد
        if (defeatedUser.getKingdom() != null && defeatedUser.getKingdom().getCastle() != null) {
            defeatedUser.getKingdom().getCastle().setStatus(Status.destroyed);
        }
        // --- END OF THE DEFINITIVE FIX ---

        // ارسال اعلان برای سایر بازیکنان (مثل قبل)
        String title = "یک فرمانروا سقوط کرد!";
        String body = "فرمانروای " + defeatedUser.getKingdom().getType() + "، " + defeatedUser.getUsername() + "، در نبرد شکست خورد و از دور رقابت‌ها کنار رفت.";

        // فراخوانی پنل شکست فقط برای بازیکن شکست‌خورده (مثل قبل)
        if (GameMediator.getInstance() != null) {
            GameMediator.getInstance().triggerDefeatPanelDisplay(defeatedUser.getId());
        }

        // ارسال اعلان سیستمی (مثل قبل)
        for (GameUser user : this.User) {
            if (user != null && user.getId() != defeatedUser.getId()) {
                sendSystemNotificationToUser(user, title, body);
            }
        }

        // ذخیره نهایی وضعیت بازی
        GameFileUtility.saveFullGameState(this);
    }
// In file: GameOfThrones.java

    /**
     * این متد کمکی، وضعیت یک بازیکن را به طور کامل پاک‌سازی و بازنشانی می‌کند.
     * اطلاعات بازیکن را به‌روزرسانی کرده و کش مدیاTور را نیز اصلاح می‌کند.
     * @param user بازیکنی که وضعیتش باید پاک شود.
     */
    public void cleanupAndResetUser(GameUser user) {
        if (user == null) return;

        // ۱. بازنشانی وضعیت‌های مرتبط با بازی برای بازیکن
        user.setInGame(false);
        user.setGame(null);
        user.setInFight(false); // اطمینان از ریست شدن وضعیت نبرد

        // ۲. ذخیره تغییرات در فایل اصلی بازیکن
        UserSystem.updateUser(user);

        // ۳. به‌روزرسانی اجباری آبجکت بازیکن در مدیاTور (برای جلوگیری از نمایش اطلاعات قدیمی در UI)
        GameMediator mediator = GameMediator.getInstance();
        if (mediator != null && mediator.getCurrentUser() != null && mediator.getCurrentUser().getId() == user.getId()) {
            GameUser mediatorUser = mediator.getCurrentUser();
            mediatorUser.setInGame(false);
            mediatorUser.setGame(null);
            mediatorUser.setInFight(false);
        }
        GameLog.info("State for user " + user.getUsername() + " has been successfully cleaned and reset.");
    }
    public void checkAllPlayersStatus() {
        for (GameUser user : this.User) {
            if (user == null) continue;

            PlayerState state = getPlayerState(user.getId());

            if (state != null && state.status == PlayerStatus.ACTIVE) {
                Castle userCastle = user.getKingdom().getCastle();
                if (userCastle != null && userCastle.isDestroyed()) {
                    handlePlayerDefeat(user);
                }
            }
        }
    }

    public void handleSpoilsOfWar(Mission mission, int stars) {
    }

    public PlayerState getPlayerState(int userId) {
        for (PlayerState state : playerStates) {
            if (state.userId == userId) {
                return state;
            }
        }
        return null;
    }
    public String getBATTLE_LOG_FILE() {
        return BATTLE_LOG_FILE;
    }

    public void setConfig(Properties config){
        this.config=config;
    }

    public int getId() {
        return id;
    }

    public GameUser getOwner() {
        return owner;
    }

    public Time getGameTime() {
        return gameTime;
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GameUser[] getUsers() {
        return User;
    }

    public GameRulesManager getRulesManager() {
        return rulesManager;
    }

    public void setRulesManager(GameRulesManager rulesManager) {
        this.rulesManager = rulesManager;
    }

    public int getPassword() {
        return password;
    }

    public boolean isGameReady() {
        return gameReady;
    }

    public String getName() {
        return name;
    }

    public static int getMaxPlayer() {
        return maxPlayer;
    }
    public  int getNumberOfPlayer() {
        return numberOfPlayer;
    }
    public Result addPlayer(GameUser user) {
        if (gameReady) {
            return new Result(false,"-");
        } else {
            this.User[numberOfPlayer++] = user;
            user.isInGame = true;
            user.setInFight(false);
            user.setGame(this);
            UserSystem.updateUser(user);
            if (numberOfPlayer == maxPlayer) {
                startGame();
            } else GameFileUtility.updateGame(this);

            return new Result(true,"-");

        }
    }
    public int getSpeed() {
        try {
            String raw = GameConfig.getGameProperty(id, "game.speed");
            return (int) Double.parseDouble(raw); // تبدیل اعشاری به صحیح
        } catch (Exception e) {
            GameLog.error("⚠️ مقدار سرعت نامعتبر است. استفاده از مقدار پیش‌فرض 1.");
            return 1;
        }
    }

    public String getDifficulty() {
        // A missing/unreadable config must not crash mines and White Walker waves (they call toLowerCase on it).
        String difficulty = GameConfig.getGameProperty(id, "game.difficulty");
        return (difficulty == null || difficulty.isBlank()) ? "medium" : difficulty;
    }

    public int getTurnDuration() {
        return Integer.parseInt(GameConfig.getGameProperty(id,"game.turn.duration"));
    }

    public int getResourceGenerationInterval() {
        return Integer.parseInt(GameConfig.getGameProperty(id,"resource.generation.interval"));
    }

    public int getWhitewalkerAttackInterval() {
        return Integer.parseInt(GameConfig.getGameProperty(id, "whitewalker.attack.interval"));
    }

    public int getWhitewalkerStartDay() { return 10; }
    public int getWhitewalkerFinalDay() { return 100; }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    public GameOfThrones(String name, int pass , GameUser user){
        User = new GameUser[maxPlayer];
        user.isInGame = true;
        gameTime = new Time(System.currentTimeMillis() );
        User[0] = user;
        this.name = name;
        password = pass;
        this.numberOfPlayer = 1;
        this.gameReady = false;
        user.setGame(this);
        UserSystem.updateUser(user);
        playerStates= new MyLinkedList<>();

        map=new Map(59,31);

        for(int i=0 ; i<59 ; i++){
            for(int j=0 ; j<31 ; j++){
                Tile tile = map.getMap()[i][j];
                tile.setTerrainType(TerrainType.LAND);
            }
        }
        owner = user;
        missions = new MyLinkedList<>();
        this.config = GameConfig.loadGameConfig(id);

        this.rulesManager = new GameRulesManager(this);

    }
    public void startGame() {

        this.gameReady = true;
        long now = System.currentTimeMillis();
        gameTime.setStartTime(now);
        whiteWalkerManager = new WhiteWalkerManager(this);

        this.transactions = new MyLinkedList<>();
        this.letter = new MyLinkedList<>();
        this.bazar = new Bazar();
        this.postOffice = new Post();

        fillWithBots();
        assignRandomKingdomsToUsers();
        for (GameUser user : this.User) {
            if (user != null) {
                playerStates.add(new PlayerState(user.getId()));
            }
        }
        int garrison = configuredBotGarrison();
        for (GameUser user : this.User) {
            if (user instanceof BotUser bot) bot.equipGarrison(garrison);
        }

        Admin.checkInfoFile();

        wall = new Wall(2000);// مقدار سلامت اولیه دیوار
        this.map.placeWallAtTopRow();

        addKingdomsRood();
        fillEmptyWithSea();

        gameTime.setGameSpeed(getSpeed());
        GameFileUtility.saveFullGameState(this);
        GameFileUtility.removeGameFromLobbyList(this);

        GameLog.info("🎮 بازی با موفقیت آغاز شد. دیوار ساخته شد و بازی در وضعیت آماده قرار گرفت.");

    }


    public  void assignRandomKingdomsToUsers() {
        if (!gameReady) {
            throw new IllegalArgumentException("game is not ready.");
        }
        else {
            Kingdom[] kingdoms = {
                new Kingdom(KingdomType.CASTERLY_ROCK, map, 30, 1, 13, 13,36,13,36,1),
                new Kingdom(KingdomType.DRAGONSTONE, map, 30, 17, 13, 13,36,17,36,29),
                new Kingdom(KingdomType.EYRIE, map, 15, 17, 13, 13,21,17,21,29),
                new Kingdom(KingdomType.HIGHGARDEN, map, 45, 1, 13, 13,51,13,51,1),
                new Kingdom(KingdomType.IRON_ISLANDS, map, 15, 1, 13, 13,21,13,21,1),
                new Kingdom(KingdomType.STORMS_END, map, 45, 17, 13, 13,51,17,51,29),
                new Kingdom(KingdomType.WINTERFELL, map, 1, 9, 13, 13,13,15,7,21)

            };

            boolean[] used = new boolean[7];
            Random rand = new Random();

            for (int i = 0; i < numberOfPlayer; i++) {
                int index;
                do {
                    index = rand.nextInt(7);
                } while (used[index]);

                User[i].setKingdom(kingdoms[index]);

                if (!BotUser.isBot(User[i])) UserSystem.updateUser(User[i]);
                used[index] = true;
                GameFileUtility.updateGame(this);
            }
        }
    }

    /** How many bots a new game gets (balance.properties: bots.count, default 0 = none). */
    public static int configuredBots() {
        return com.yourname.got.logic.Balance.BalanceConfig.integer("bots.count", 0, 0, maxPlayer - 1);
    }

    /** Soldiers in a bot's home guard (balance.properties: bots.garrison, default 30). */
    public static int configuredBotGarrison() {
        return com.yourname.got.logic.Balance.BalanceConfig.integer("bots.garrison", 30, 0, 500);
    }

    public int getBotCount() {
        int n = 0;
        for (GameUser u : this.User) if (BotUser.isBot(u)) n++;
        return n;
    }

    /** Adds the configured number of bots into the free player slots (never more than there are kingdoms). */
    private void fillWithBots() {
        if (getBotCount() > 0) return; // already filled (the game was started before)
        int wanted = configuredBots();
        int added = 0;
        while (added < wanted && numberOfPlayer < maxPlayer) {
            BotUser bot = new BotUser(-(added + 1));
            bot.setGame(this);
            bot.isInGame = true;
            this.User[numberOfPlayer++] = bot;
            added++;
        }
        if (added > 0) GameLog.info("INFO: " + added + " bot(s) joined the game.");
    }

    public void checkUserActivity() {
        if (!this.gameReady) {
            return;
        }

        int activePlayerCount = 0;

        for (int i = 0; i < this.numberOfPlayer; i++) {
            GameUser currentUser = this.User[i];

            if (currentUser == null) {
                continue;
            }
            if (BotUser.isBot(currentUser)) { // a bot is always "online"
                this.User[activePlayerCount] = currentUser;
                activePlayerCount++;
                continue;
            }

            long elapsedMillis = System.currentTimeMillis() - currentUser.getLastVisit();
            long daysElapsed = elapsedMillis / (1000 * 60 * 60 * 24);

            if (daysElapsed < 25) {
                this.User[activePlayerCount] = currentUser;
                activePlayerCount++;
            }
            else {
                GameLog.info("کاربر " + currentUser.getUsername() + " (مالک قلعه: " + currentUser.getKingdom().getType() + ") به علت عدم فعالیت طولانی‌مدت از بازی حذف شد.");

                currentUser.setKingdom(null);
                currentUser.setInGame(false);
                currentUser.setInFight(false);
                currentUser.setGame(null);
                UserSystem.updateUser(currentUser);
            }
        }

        for (int i = activePlayerCount; i < this.numberOfPlayer; i++) {
            this.User[i] = null;
        }
        this.numberOfPlayer = activePlayerCount;
    }

    public MyLinkedList<Mission> getMissions() {
        return missions;
    }
    public int GroundDistance(GameUser request, GameUser target) {
        if (target == null)
            return 25 * 300;
        // محاسبه مسافت زمینی باید بر اساس دروازه/نقطه ورود (EntryPoint) باشد نه بندر
        return (int) Math.round(request.getKingdom().getEntryPoint().distance(target.getKingdom().getEntryPoint())) * 300;
    }
    public int seaDistance(GameUser request,GameUser target){int distance=0;
        if(target==null)
            return 100000;
        if(request.getKingdom().getType()==KingdomType.WINTERFELL||target.getKingdom().getType()==KingdomType.WINTERFELL){
            distance=100;}
        else {if(request.getKingdom().getPortPoint().getX()==target.getKingdom().getPortPoint().getX())
        {distance=request.getKingdom().getPortPoint().distance(target.getKingdom().getPortPoint())+2;}else
        {distance=32+2*this.getMap().mainMap.length-target.getKingdom().getPortPoint().getX()-request.getKingdom().getPortPoint().getX();}}return distance*100;}


    public Map getMap() {
        return map;
    }
    // این متد را به انتهای کلاس GameOfThrones.java اضافه کنید
    public String getReplaysFolderPath() {
        return REPLAYS_FOLDER_PATH;
    }
    public void addKingdomsRood(){
        for(int i=14 ; i<58 ;i++){
            for(int j=14 ; j<17 ; j++){
                Tile tile = map.mainMap[i][j];
                if((tile.isEmpty)) {
                    tile.setObjectOnTile(new ObjectOnTile("Land"));
                    tile.setTerrainType(TerrainType.LAND);
                    tile.setEmpty(false);
                }
            }
        }
        for(int i = 28; (i < 30) ; i++){
            for(int j=1 ; j<30 ; j++){
                Tile tile = map.mainMap[i][j];
                if((tile.isEmpty)) {
                    tile.setObjectOnTile(new ObjectOnTile("Land"));
                    tile.setTerrainType(TerrainType.LAND);
                    tile.setEmpty(false);
                }
            }
        }
        for(int i = 43; (i < 45) ; i++){
            for(int j=1 ; j<30 ; j++){
                Tile tile = map.mainMap[i][j];
                if((tile.isEmpty)) {
                    tile.setObjectOnTile(new ObjectOnTile("Land"));
                    tile.setTerrainType(TerrainType.LAND);
                    tile.setEmpty(false);
                }
            }
        }
    }
    public void fillEmptyWithSea() {
        for (int i = 0; i < map.mainMap.length; i++) {
            for (int j = 0; j < map.mainMap[0].length; j++) {
                Tile tile = map.mainMap[i][j];
                if (tile.getObjectOnTile() == null) {
                    tile.setTerrainType(TerrainType.SEA);
                    tile.setObjectOnTile(new ObjectOnTile("Sea"));
                    tile.setEmpty(false);
                }
            }
        }
    }


    public void checkMissions() {
        if (missions == null || missions.isEmpty()) {
            return; // اگر مأموریتی نیست، خارج شو
        }

        GameLog.info("\n--- Checking " + missions.getSize() + " missions at " + java.time.LocalTime.now() + " ---");

        MyLinkedList<Mission> completedMissions = new MyLinkedList<>();
        Node<Mission> missionNode = missions.getHead();

        while (missionNode != null) {
            Mission mission = missionNode.data;

            // --- بخش دیباگ ---
            boolean isReady = mission.isReadyToAttack();
            long remaining = mission.getDurationSeconds() - mission.getElapsedTime();
            GameLog.info("  > Mission to " + mission.getDefender().getUsername() +
                ": isReady? " + isReady + " (Remaining: " + remaining + "s)");
            // --- پایان بخش دیباگ ---

            if (isReady) {
                GameLog.info("    ✅ Mission is ready! Starting attack...");
                if (mission.startAttack()) {
                    completedMissions.add(mission);
                }
            }
            missionNode = missionNode.next;
        }

        if (!completedMissions.isEmpty()) {
            GameLog.info("    🧹 Clearing " + completedMissions.getSize() + " completed missions.");
            Node<Mission> completedNode = completedMissions.getHead();
            while (completedNode != null) {
                Mission mission = completedNode.data;
                if (mission.getDefender() != null) {
                    mission.getDefender().setInFight(false);
                }
                missions.remove(mission);
                completedNode = completedNode.next;
            }
            GameFileUtility.saveFullGameState(this);
        }
        GameLog.info("--- Finished checking missions ---");
    }
    public  void setAlert() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - gameTime.startTime;

        long elapsedSeconds = elapsedMillis / 1000;

        long secondsInCurrentDay = elapsedSeconds % (24* 60 * 60);
        alert = ( 24*60 * 60 - secondsInCurrentDay) <= 3600;
    }

    public  int getDistanceTime(GameUser request,GameUser recever){
        int distance=Math.round(request.getKingdom().getEntryPoint().distance(recever.getKingdom().getEntryPoint()));
        return Time.getTimeForDistance(distance);

    }
    public  GameUser getUser(int id) {

        {for (int i=0;i<numberOfPlayer;i++)
            if (User[i] != null && User[i].getId() ==id ) {
                return User[i];
            }
        }
        return null;

    }
    public Bazar getBazar() {
        return bazar;
    }


    public Post getPostOffice() {
        return postOffice;
    }

    public void addLetter(Notice notice) {
        postOffice.add(notice);
    }


    public void sendSystemNotificationToUser(GameUser user, String title, String body) {
        if (user == null) return;

        Notice notification = new Notice(0, null, user, title, body);

        this.getPostOffice().add(notification); // فرض بر اینکه لیست پیام‌ها در PostOffice است

        user.getKingdom().incrementUnreadNotifications();

        GameLog.info("یک اعلان برای " + user.getUsername() + " ارسال شد.");
    }

    public void sendSystemNotificationToAll(String title, String body) {
        GameLog.info("ارسال اعلان عمومی برای تمام بازیکنان...");

        Notice publicNotification = new Notice(0, null, null, title, body);

        this.getPostOffice().add(publicNotification);

        for (GameUser user : this.getUsers()) {
            if (user != null && user.getKingdom() != null) {
                user.getKingdom().incrementUnreadNotifications();
            }
        }
        GameLog.info("اعلان عمومی با موفقیت ارسال شد.");
    }

// In file: GameOfThrones.java

    public Result removePlayer(GameUser userToRemove) {
        // محدودیت قبلی که جلوی خروج پس از شروع بازی را می‌گرفت حذف شد.
        // اکنون این متد می‌تواند برای پاک‌سازی بازیکن در هر مرحله‌ای استفاده شود.
        int userIndex = -1;
        for (int i = 0; i < numberOfPlayer; i++) {
            if (User[i] != null && User[i].getId() == userToRemove.getId()) {
                userIndex = i;
                break;
            }
        }

        if (userIndex != -1) {
            // حذف بازیکن از لیست بازی و جابجایی سایر بازیکنان
            for (int i = userIndex; i < numberOfPlayer - 1; i++) {
                User[i] = User[i + 1];
            }
            User[numberOfPlayer - 1] = null;
            numberOfPlayer--;

            // *** استفاده از متد جدید برای پاک‌سازی کامل وضعیت بازیکن ***
            cleanupAndResetUser(userToRemove);

            // ذخیره وضعیت جدید بازی (با یک بازیکن کمتر)
            GameFileUtility.updateGame(this);
            return new Result(true, "بازیکن " + userToRemove.getUsername() + " با موفقیت از بازی خارج و وضعیت او پاک‌سازی شد.");

        } else {
            return new Result(false, "خطا: بازیکن مورد نظر در این بازی یافت نشد.");
        }
    }
    public GameUser userPostAvailable(int id, GameUser user) {
        for (int i = 0; i < User.length; i++) {
            GameUser u = User[i];
            if (u != null && u.getId() == id && u.getId() != user.getId() && !BotUser.isBot(u) && u.isActive() && !u.isInFight() && u.getKingdom().hasCrowTower() ) {
                return u;
            }
        }
        GameLog.info("❌ بازیکن با شناسه فوق یافت نشد، قادر به دریافت پیام نیست یا فاقد برج کلاغ است."); //
        return null;
    }

    public WhiteWalkerManager getWhiteWalkerManager() {
        return whiteWalkerManager;
    }

    public void broadcastWallHelpNotice() {
        int delaySeconds = 0;
        String title = "درخواست کمک به دیوار";
        String note = "🧱 لطفاً به دیوار کمک کنید! در صورت سقوط آن، همه اقلیم‌ها آسیب می‌بینند.";

        for (GameUser user : User) {
            if (user != null && user.getKingdom() != null) {
                Notice notice = new Notice(delaySeconds, null, user, title, note);
                postOffice.add(notice);
            }
        }
    }

    public Wall getWall() {
        return wall;
    }

    public Result helpWall(Kingdom k, int wood, int stone, int coin, int food) {
        if (wall == null || wall.isDestroyed()) {
            return new Result(false, "The Wall is already destroyed.");
        }
        if (!k.canAffordResources(wood, stone, coin, food)) {
            return new Result(false, "You do not have enough resources.");
        }

        k.detuctResources(wood, stone, coin,food);
        int total = wood + stone + coin + food;

        int repairAmount = total / 10;
        int dragonGlassReward = Math.min(total / 50, 10);

        wall.reinforce(k, repairAmount, getGameTime().getDay());

        StringBuilder successMessage = new StringBuilder("SUCCESS: Your contribution has been registered!");

        if (dragonGlassReward > 0) {
            k.addDragonGlass(dragonGlassReward);
            successMessage.append("\n<You have been rewarded with ").append(dragonGlassReward).append(" DragonGlass!>");
        }

        return new Result(true, successMessage.toString());
    }


    public int getNumberOfPlayers() {
        return numberOfPlayer;
    }
    public MyLinkedList<Notice> getLetter() {
        return letter;
    }

    public GameUser[] getActiveUsers() {
        int count = 0;
        for (GameUser u : User) {
            if (u != null && u.isActive() && u.getKingdom() != null) {
                count++;
            }
        }

        GameUser[] active = new GameUser[count];
        int index = 0;
        for (GameUser u : User) {
            if (u != null && u.isActive() && u.getKingdom() != null) {
                active[index++] = u;
            }
        }
        return active;
    }


    public int getNumActiveUsers() {
        int count = 0;
        for (GameUser u : User) {
            if (u != null && u.isActive() && u.getKingdom() != null) {
                count++;
            }
        }
        return count;
    }

    public int getTotalDragonGlassInGame() {
        int total = 0;
        for (GameUser u : User) {
            if (u != null && u.getKingdom() != null) {
                total += u.getKingdom().getDragonGlass();
            }
        }
        return total;
    }



    public void postLoadUpdate() {
        if (this.gameTime != null && this.gameTime.gameSpeed <= 0) {
            GameLog.info("LOG: سرعت بازی نامعتبر پس از بارگذاری شناسایی شد. در حال اصلاح...");
            this.gameTime.setGameSpeed(this.getSpeed());
        }
    }
    /**
     * این متد به طور خودکار پس از بارگذاری بازی از فایل فراخوانی می‌شود
     * و تضمین می‌کند که مسیرهای لاگ برای بازی‌های قدیمی نیز تنظیم شوند.
     */
    /**
     * این متد به طور خودکار پس از بارگذاری بازی از فایل فراخوانی می‌شود
     * و تضمین می‌کند که مسیرهای لاگ برای بازی‌های قدیمی نیز تنظیم شوند.
     */

}
