package com.yourname.got.logic.model;

import java.io.*;
import com.yourname.got.logic.Config.GameConfig;
import com.yourname.got.logic.enums.Status;
import com.yourname.got.logic.login.*;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
// import java.util.Scanner; // <--- حذف کامل وابستگی به Scanner
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.TerrainType;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Castle;
import com.yourname.got.logic.system.GameLoopManager;
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
    MyLinkedList<PlayerState> playerStates;
    private  GameLoopManager gameLoopManager;
    public String REPLAYS_FOLDER_PATH;

    public enum PlayerStatus {
        ACTIVE,
        DEFEATED
    }
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int userId;
        public PlayerStatus status;

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
// In file: GameOfThrones.java
// Replace the entire endGame method with this one

//    public void endGame(GameRecord.GameResult reason) {
//        // 1. جلوگیری از اجرای مجدد (منطق شما حفظ شده)
//        if (this.isFinished) return;
//        this.isFinished = true;
//        System.out.println("GAME OVER: The game '" + this.name + "' has ended. Reason: " + reason);
//
//        // 2. ساخت آبجکت GameRecord (منطق شما حفظ شده)
//        String duration = this.gameTime.getFormattedElapsedTime();
//        GameRecord finalRecord = new GameRecord(this.name, this.config, duration, reason);
//
//        // --- START OF NEW & CORRECTED LOGIC ---
//        // 3. ایجاد و پر کردن لیست رده‌بندی بازیکنان
//        System.out.println("Calculating final player rankings...");
//        MyLinkedList<PlayerResult> rankedResults = new MyLinkedList<>();
//
//        // ابتدا برنده(ها) یا بازمانده‌ها را اضافه می‌کنیم
//        for (GameUser user : this.User) {
//            if (user == null) continue;
//            PlayerState state = getPlayerState(user.getId());
//            if (state != null && state.status == PlayerStatus.ACTIVE) {
//                PlayerResult.PlayerStatus finalStatus = PlayerResult.PlayerStatus.DEFEATED; // Default
//                if (reason == GameRecord.GameResult.LAST_PLAYER_STANDING) {
//                    finalStatus = PlayerResult.PlayerStatus.WINNER;
//                } else if (reason == GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT) {
//                    finalStatus = PlayerResult.PlayerStatus.SURVIVED;
//                }
//                // رتبه و امتیاز موقتا ۰ در نظر گرفته می‌شود و بعداً اصلاح خواهد شد
//                rankedResults.add(new PlayerResult(user.getUsername(), 0, finalStatus, 0));
//            }
//        }
//
//        // سپس بازیکنان شکست‌خورده را اضافه می‌کنیم
//        for (GameUser user : this.User) {
//            if (user == null) continue;
//            PlayerState state = getPlayerState(user.getId());
//            if (state != null && state.status == PlayerStatus.DEFEATED) {
//                rankedResults.add(new PlayerResult(user.getUsername(), 0, PlayerResult.PlayerStatus.DEFEATED, 0));
//            }
//        }
//
//        // حالا لیست نهایی را به GameRecord اضافه می‌کنیم (در اینجا می‌توان منطق مرتب‌سازی پیشرفته‌تری هم اضافه کرد)
//        // در این نسخه ساده، ترتیب بر اساس وضعیت است
//        int rank = 1;
//        for (PlayerResult result : rankedResults) {
//            // سازنده PlayerResult شما rank را می‌گیرد، پس ما یک نسخه جدید با رتبه صحیح می‌سازیم
//            finalRecord.addPlayerResult(new PlayerResult(result.getUsername(), result.getScore(), result.getStatus(), rank));
//            System.out.println("  > Rank " + rank + ": " + result.getUsername() + " as " + result.getStatus());
//            rank++;
//        }
//        // --- END OF NEW & CORRECTED LOGIC ---
//
//        // 4. ذخیره رکورد برای همه بازیکنان (منطق شما حفظ شده)
//        System.out.println("Finalizing and saving records for all players...");
//        for (GameUser u : this.User) {
//            if (u == null) continue;
//            try {
//                cleanupAndResetUser(u);
//
//            } catch (Exception e) {
//                System.err.println("ERROR: Failed to save record or update user state for " + u.getUsername() + ": " + e.getMessage());
//            }
//        }
//
//        // 5. ذخیره وضعیت نهایی بازی و اطلاع به UI (منطق شما حفظ شده)
//        GameFileUtility.saveFullGameState(this);
//        if (GameMediator.getInstance() != null) {
//            GameMediator.getInstance().triggerResultsPanelDisplay();
//        }
//
//        System.out.println("Game finalization complete.");
//    }
// در فایل GameOfThrones.java

// در فایل GameOfThrones.java متد را جایگزین کنید
    public void endGame(GameRecord.GameResult reason) {
        if (this.isFinished) return;
        this.isFinished = true;

        System.out.println("GAME OVER: Reason: " + reason);

        // ۱. ساخت رکورد نهایی بازی
        String duration = this.gameTime.getFormattedElapsedTime();
        GameRecord finalRecord = new GameRecord(this.name, this.config, duration, reason);

        // ۲. رتبه‌بندی بازیکنان
        MyLinkedList<PlayerResult> rankedResults = new MyLinkedList<>();

        // اول زنده‌ها/برنده‌ها
        for (GameUser user : this.User) {
            if (user == null) continue;
            PlayerState state = getPlayerState(user.getId());
            if (state != null && state.status == PlayerStatus.ACTIVE) {
                PlayerResult.PlayerStatus finalStatus = (reason == GameRecord.GameResult.LAST_PLAYER_STANDING) ?
                    PlayerResult.PlayerStatus.WINNER : PlayerResult.PlayerStatus.SURVIVED;
                rankedResults.add(new PlayerResult(user.getUsername(), 0, finalStatus, 0));
            }
        }
        // دوم بازنده‌ها
        for (GameUser user : this.User) {
            if (user == null) continue;
            PlayerState state = getPlayerState(user.getId());
            if (state != null && state.status == PlayerStatus.DEFEATED) {
                rankedResults.add(new PlayerResult(user.getUsername(), 0, PlayerResult.PlayerStatus.DEFEATED, 0));
            }
        }

        // افزودن به رکورد با رتبه صحیح
        int rank = 1;
        for (PlayerResult res : rankedResults) {
            finalRecord.addPlayerResult(new PlayerResult(res.getUsername(), res.getScore(), res.getStatus(), rank));
            rank++;
        }

        // ۳. ذخیره رکورد در فایل‌های شخصی همه کاربران (چه آنلاین چه آفلاین)
        for (GameUser u : this.User) {
            if (u == null) continue;
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
        GameUser lastAlive = null;

        for (GameUser u : this.User) {
            if (u == null) continue;
            PlayerState st = getPlayerState(u.getId());
            if (st != null && st.status == PlayerStatus.ACTIVE) {
                activePlayers++;
                lastAlive = u;
            }
        }

        // شرط ۱: وایت واکرها همه را کشتند
        if (activePlayers == 0) {
            endGame(GameRecord.GameResult.WHITE_WALKERS_WON);
            return;
        }

        // شرط ۲: فقط یک نفر مانده (Last Man Standing)
        // نکته: فقط اگر موج آخر وایت واکرها شروع شده باشد یا بازی به روزهای آخر رسیده باشد
        if (activePlayers == 1 && numberOfPlayer > 1) {
            endGame(GameRecord.GameResult.LAST_PLAYER_STANDING);
            return;
        }

        // شرط ۳: روز ۱۰۰ فرا رسیده (پایان زمان)
        if (this.getGameTime().getDay() >= getWhitewalkerFinalDay()) {
            endGame(GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT);
            return;
        }
    }

//    public void checkGameOver() {
//        checkUserActivity();
//        checkAllPlayersStatus();
//        if (isFinished) return;
//        int activePlayers = 0;
//        GameUser lastAlive = null;
//
//
//        for (GameUser u : this.User) {
//            if (u == null) continue;
//            PlayerState st = getPlayerState(u.getId());
//            if (st != null && st.status == PlayerStatus.ACTIVE) {
//                activePlayers++;
//                lastAlive = u;
//            }
//        }
//
//        if (this.getGameTime().getDay() <= this.getWhitewalkerFinalDay()&& activePlayers!=0) {
//            return;
//        }
//
//        if (activePlayers == 0) {
//            endGame(GameRecord.GameResult.WHITE_WALKERS_WON);
//            return;
//        }
//
//        if (activePlayers == 1 && lastAlive != null && whiteWalkerManager.isFinalWaveTriggered()) {
//            endGame(GameRecord.GameResult.LAST_PLAYER_STANDING);
//            return;
//        }
//
//        if (this.getGameTime().getDay() >= getWhitewalkerFinalDay() && whiteWalkerManager.isFinalWaveTriggered()) {
//            endGame(GameRecord.GameResult.TIME_UP);
//            return;
//        }
//    }

    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
        // این متد برای اطمینان از سازگاری نسخه‌های مختلف کلاس هنگام ذخیره و بازیابی است
        // با انتقال منطق ساخت فایل به GameFileUtility، دیگر نیازی به کد اضافی در اینجا نیست
        in.defaultReadObject();
    }
    // In file: GameOfThrones.java

    public void handlePlayerDefeat(GameUser defeatedUser) {
        if (defeatedUser == null) return;

        PlayerState state = getPlayerState(defeatedUser.getId());
        // اگر از قبل شکست خورده، کاری انجام نده
        if (state == null || state.status == PlayerStatus.DEFEATED) {
            return;
        }

        System.out.println("⚔️ بازیکن " + defeatedUser.getUsername() + " شکست خورد.");

        // --- START OF THE DEFINITIVE FIX ---
        // ۱. وضعیت بازیکن را به طور قطعی به DEFEATED تغییر بده
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
        System.out.println("State for user " + user.getUsername() + " has been successfully cleaned and reset.");
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
    public void displayBattleReportsForUser(GameUser user) {
        if (user == null) return;

        if (BATTLE_LOG_FILE == null || BATTLE_LOG_FILE.isEmpty()) {
            System.out.println("❌ مسیر فایل گزارش نبردها تعریف نشده است.");
            System.out.println("   (این مشکل ممکن است برای بازی‌های قدیمی که قبل از این آپدیت ذخیره شده‌اند رخ دهد.)");
            return;
        }

        String currentUsername = user.getUsername();
        System.out.println("===== گزارش نبردها برای کاربر: " + currentUsername + " =====");

        try (BufferedReader br = new BufferedReader(new FileReader(BATTLE_LOG_FILE))) {
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length < 5) continue;

                String attackerName = parts[0];
                String defenderName = parts[1];

                if (attackerName.equals(currentUsername)) {
                    found = true;
                    System.out.println("\n-- وضعیت: هجوم --");
                    System.out.println("  > حریف: " + defenderName);
                    System.out.println("  > نتیجه: " + parts[2] + " ستاره");
                    System.out.println("  > ترکیب ارتش: " + parts[3]);
                    System.out.println("  > زمان: " + parts[4]);
                } else if (defenderName.equals(currentUsername)) {
                    found = true;
                    System.out.println("\n-- وضعیت: دفاع --");
                    System.out.println("  > حریف: " + attackerName);
                    System.out.println("  > نتیجه: شکست با " + parts[2] + " ستاره از دست رفته");
                    System.out.println("  > زمان: " + parts[4]);
                }
            }
            if (!found) {
                System.out.println("هیچ نبردی برای این کاربر ثبت نشده است.");
            }
        } catch (IOException e) {
            System.out.println("فایل گزارش نبردها در مسیر '" + BATTLE_LOG_FILE + "' یافت نشد!");
        }
        System.out.println("==========================================");
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
            System.err.println("⚠️ مقدار سرعت نامعتبر است. استفاده از مقدار پیش‌فرض 1.");
            return 1;
        }
    }

    public String getDifficulty() {
        return GameConfig.getGameProperty(id,"game.difficulty");
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

        assignRandomKingdomsToUsers();
        for (GameUser user : this.User) {
            if (user != null) {
                playerStates.add(new PlayerState(user.getId()));
            }
        }

        Admin.checkInfoFile();

        wall = new Wall(2000);// مقدار سلامت اولیه دیوار
        this.map.placeWallAtTopRow();

        addKingdomsRood();
        fillEmptyWithSea();

        gameTime.setGameSpeed(getSpeed());
        GameFileUtility.saveFullGameState(this);
        GameFileUtility.removeGameFromLobbyList(this);

        System.out.println("🎮 بازی با موفقیت آغاز شد. دیوار ساخته شد و بازی در وضعیت آماده قرار گرفت.");

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

                UserSystem.updateUser(User[i]);
                used[index] = true;
                GameFileUtility.updateGame(this);
            }
        }
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

            long elapsedMillis = System.currentTimeMillis() - currentUser.getLastVisit();
            long daysElapsed = elapsedMillis / (1000 * 60 * 60 * 24);

            if (daysElapsed < 25) {
                this.User[activePlayerCount] = currentUser;
                activePlayerCount++;
            }
            else {
                System.out.println("کاربر " + currentUser.getUsername() + " (مالک قلعه: " + currentUser.getKingdom().getType() + ") به علت عدم فعالیت طولانی‌مدت از بازی حذف شد.");

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
    public GameUser selectTarget(GameUser attacker, AttackType attackType) {
        Scanner input = new Scanner(System.in);

        // مرحله ۱: ساخت یک آرایه موقت فقط از اهداف معتبر
        // ابتدا تعداد اهداف معتبر را می‌شماریم
        int targetCount = 0;
        // فرض می‌کنیم آرایه اصلی شما allUsers نام دارد
        for (GameUser user : this.User) {
            if (user != null &&  !(user.getId()==attacker.getId())&&!user.isInFight()) {
                if ((attackType==AttackType.GROUND&&user.getKingdom().getType()==KingdomType.IRON_ISLANDS))
                    continue;


                targetCount++;
            }
        }

        if (targetCount == 0) {
            System.out.println("هیچ هدف معتبری برای حمله یافت نشد.");
            return null;
        }

        // حالا آرایه‌ای با اندازه دقیق می‌سازیم
        GameUser[] validTargets = new GameUser[targetCount];
        int index = 0;
        for (GameUser user : this.User) {
            if (user != null && !(user.getId()==attacker.getId())&&!user.isInFight()) {
                if ((attackType==AttackType.GROUND&&user.getKingdom().getType()==KingdomType.IRON_ISLANDS))
                    continue;
                validTargets[index] = user;
                index++;
            }
        }
        int distance;
        Double speedMov=1.0;
        if(attackType==AttackType.SEA)
            speedMov=attacker.getKingdom().getBattleShip().Speed;
        else attacker.getKingdom().getArmyManager().getSoldierSpeed();
        // مرحله ۲: نمایش لیست شماره‌بندی شده و دریافت انتخاب عددی کاربر
        while (true) {
            System.out.println("--- لطفاً هدف خود را با وارد کردن شماره آن انتخاب کنید ---");
            for (int i = 0; i < validTargets.length; i++) {
                if(attackType==AttackType.SEA)
                    distance=seaDistance(attacker,validTargets[i]);
                else distance=GroundDistance(attacker,validTargets[i]);
                System.out.println((i + 1)+"کینگدوم:"+validTargets[i].getKingdom().getType()+"فاصله:"+distance+"زمان بر حسب فاصله:"+this.getGameTime().getTimeForDistance(distance,speedMov));

            }

            System.out.print("شماره هدف (یا 'exit' برای بازگشت): ");
            String choiceStr = input.next();

            if (choiceStr.equalsIgnoreCase("exit")) {
                return null;
            }

            try {
                int choice = Integer.parseInt(choiceStr);
                if (choice > 0 && choice <= validTargets.length) {
                    return validTargets[choice - 1]; // بازگرداندن کاربر انتخاب شده
                } else {
                    System.out.println("❌ شماره وارد شده خارج از محدوده است.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ ورودی نامعتبر. لطفاً یک عدد وارد کنید.");
            }
        }
    }
    // این متد را به انتهای کلاس GameOfThrones.java اضافه کنید
    public String getReplaysFolderPath() {
        return REPLAYS_FOLDER_PATH;
    }
    public void printSpyReport(GameUser defndar,AttackType attackType){
        defndar.getKingdom().getUserMap().printUserMap();
        defndar.getKingdom().getArmyManager().printDefenders();
        System.out.println("مقاومت دفاعی:"+defndar.getKingdom().getArmyManager().getDefenceManager().DefenseComplex.getTotalHealth(attackType));

    }
    public void printMission(GameUser attacker){
        int counter=0     ;
        for(Mission mission:missions){
            if(mission.getAttacker().getId()==attacker.getId())
                System.out.println((++counter)+""+attacker.getKingdom()+"attack to---->"+mission.getDefender());

        }
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

        System.out.println("\n--- Checking " + missions.getSize() + " missions at " + java.time.LocalTime.now() + " ---");

        MyLinkedList<Mission> completedMissions = new MyLinkedList<>();
        Node<Mission> missionNode = missions.getHead();

        while (missionNode != null) {
            Mission mission = missionNode.data;

            // --- بخش دیباگ ---
            boolean isReady = mission.isReadyToAttack();
            long remaining = mission.getDurationSeconds() - mission.getElapsedTime();
            System.out.println("  > Mission to " + mission.getDefender().getUsername() +
                ": isReady? " + isReady + " (Remaining: " + remaining + "s)");
            // --- پایان بخش دیباگ ---

            if (isReady) {
                System.out.println("    ✅ Mission is ready! Starting attack...");
                if (mission.startAttack()) {
                    completedMissions.add(mission);
                }
            }
            missionNode = missionNode.next;
        }

        if (!completedMissions.isEmpty()) {
            System.out.println("    🧹 Clearing " + completedMissions.getSize() + " completed missions.");
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
        System.out.println("--- Finished checking missions ---");
    }
    public  void setAlert() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - gameTime.startTime;

        long elapsedSeconds = elapsedMillis / 1000;

        long secondsInCurrentDay = elapsedSeconds % (24* 60 * 60);
        alert = ( 24*60 * 60 - secondsInCurrentDay) <= 3600;
    }

    public  void printUserList(GameUser userRequest){
        for (int i=0;i<7;i++){
            if(User[i]!=null&&User[i].getId()!=userRequest.getId()&&User[i].isActive()&&!User[i].isInFight()){
                int distance=Math.round(userRequest.getKingdom().getEntryPoint().distance(User[i].getKingdom().getEntryPoint()));
                System.out.println(i+"کینگدوم:"+User[i].getKingdom().getType()+"ایدی یوزر:"+User[i].getId()+"فاصله:"+distance+"زمان بر حسب فاصله:"+Time.getTimeForDistance(distance));
            }
        }
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

        System.out.println("یک اعلان برای " + user.getUsername() + " ارسال شد.");
    }

    public void sendSystemNotificationToAll(String title, String body) {
        System.out.println("ارسال اعلان عمومی برای تمام بازیکنان...");

        Notice publicNotification = new Notice(0, null, null, title, body);

        this.getPostOffice().add(publicNotification);

        for (GameUser user : this.getUsers()) {
            if (user != null && user.getKingdom() != null) {
                user.getKingdom().incrementUnreadNotifications();
            }
        }
        System.out.println("اعلان عمومی با موفقیت ارسال شد.");
    }

    public int userPostList(GameUser userReq) {
        System.out.println("\n--- شروع بررسی بازیکنان برای ارسال پیام ---");
        System.out.println("کاربر درخواست دهنده: " + userReq.getUsername() + " (ID: " + userReq.getId() + ")");
        System.out.println("\n👥 بازیکنان قابل ارسال پیام (با برج کلاغ):");
        int foundCount = 0;
        int displayIndex = 1;

        if (User == null) {
            System.out.println("DEBUG: آرایه کاربران (User[]) نال است!");
            return 0;
        }
        System.out.println("DEBUG: تعداد کل اسلات های کاربران: " + User.length);

        for (int i = 0; i < User.length; i++) {
            GameUser u = User[i];
            System.out.println("\nDEBUG: در حال بررسی کاربر در ایندکس " + i);

            if (u == null) {
                System.out.println("DEBUG: کاربر در ایندکس " + i + " نال است. رد شد.");
                continue;
            }

            System.out.println("DEBUG: کاربر: " + u.getUsername() + " (ID: " + u.getId() + ")");
            System.out.println("DEBUG: فعال بودن: " + u.isActive() + ", در نبرد: " + u.isInFight());

            if (u.getId() == userReq.getId()) {
                System.out.println("DEBUG: این کاربر، درخواست دهنده است. رد شد.");
                continue;
            }

            if (u.getKingdom() == null) {
                System.out.println("DEBUG: پادشاهی کاربر " + u.getUsername() + " نال است. رد شد.");
                continue;
            }
            System.out.println("DEBUG: پادشاهی کاربر " + u.getUsername() + ": " + u.getKingdom().getType());
            System.out.println("DEBUG: آیا برج کلاغ دارد؟ " + u.getKingdom().hasCrowTower());

            if (u.isActive() && !u.isInFight() && u.getKingdom().hasCrowTower()) {
                foundCount++;
                System.out.println("DEBUG: کاربر " + u.getUsername() + " شرایط را دارد.");
                System.out.println(displayIndex + ". نام کاربری: " + u.getUsername() + ", شناسه: " + u.getId() + ", پادشاهی: " + u.getKingdom().getType());
                displayIndex++;
            } else {
                System.out.println("DEBUG: کاربر " + u.getUsername() + " شرایط را ندارد. (یک یا چند شرط نقض شد)");
            }
        }

        if (foundCount == 0) {
            System.out.println("❗ هیچ بازیکنی برای ارسال پیام در دسترس نیست یا فاقد برج کلاغ هستند.");
        }
        System.out.println("--- پایان بررسی بازیکنان برای ارسال پیام. تعداد یافته شده: " + foundCount + " ---");
        return foundCount;
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
            if (u != null && u.getId() == id && u.getId() != user.getId() && u.isActive() && !u.isInFight() && u.getKingdom().hasCrowTower() ) {
                return u;
            }
        }
        System.out.println("❌ بازیکن با شناسه فوق یافت نشد، قادر به دریافت پیام نیست یا فاقد برج کلاغ است."); //
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

    public void printTopContributors() {
        System.out.println("🔍 تعداد کمک‌ها: " + (wall.getContributions() == null ? "null" : wall.getContributions().size()));
        if (wall.getContributions() == null || wall.getContributions().isEmpty()) {
            System.out.println("ℹ️ هنوز هیچ کمکی به دیوار ثبت نشده.");
            return;
        }


        System.out.println("\n🏆 مشارکت‌کنندگان برتر دیوار:");

        Wall.ContributionRecord[] array = new Wall.ContributionRecord[wall.getContributions().size()];
        wall.getContributions().fillArray(array);

        for (int i = 0; i < array.length - 1; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[j].getAmount() > array[i].getAmount()) {
                    Wall.ContributionRecord temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                }
            }
        }

        for (int i = 0; i < Math.min(array.length, 5); i++) {
            KingdomType name = array[i].getKingdom().getType();
            int amount = (int) array[i].getAmount();
            System.out.println((i + 1) + ". " + name + " → " + amount + " واحد کمک");
        }
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

    public void printDragonGlassReport() {
        System.out.println("\n🔮 وضعیت DragonGlass در اقلیم‌ها:");
        for (GameUser u : getActiveUsers()) {
            Kingdom k = u.getKingdom();
            System.out.println("- " + k.getType() + ": " + k.getDragonGlass() + " شیشه | " + k.getEquippedSoldierCount() + " سرباز مجهز");
        }
    }


    public void postLoadUpdate() {
        if (this.gameTime != null && this.gameTime.gameSpeed <= 0) {
            System.out.println("LOG: سرعت بازی نامعتبر پس از بارگذاری شناسایی شد. در حال اصلاح...");
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
