package com.yourname.got.logic.login;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.yourname.got.logic.Config.GameConfig;
import com.yourname.got.logic.model.GameOfThrones;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

public class GameFileUtility {

    private static final String MAIN_FILE = "games.txt";
    private static final String GAME_FOLDER = "GAMES";
    private static int currentId = 1;

    static {
        ensureMainFileAndFolder();
        initializeIdFromFile();
    }

    private static void ensureMainFileAndFolder() {
        try {
            File mainFile = new File(MAIN_FILE);
            if (!mainFile.exists()) mainFile.createNewFile();

            File folder = new File(GAME_FOLDER);
            if (!folder.exists()) folder.mkdir();

        } catch (IOException e) {
            System.err.println("خطا در ساخت فایل/پوشه: " + e.getMessage());
        }
    }
    public static String getGameFolder(int gameId) {
        return GAME_FOLDER + File.separator + "game_" + gameId;
    }

    private static void initializeIdFromFile() {
        int maxIdFromTxt = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(MAIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        int id = Integer.parseInt(parts[2]);
                        if (id > maxIdFromTxt) {
                            maxIdFromTxt = id;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("خطا در خواندن فایل games.txt برای تعیین ID: " + e.getMessage());
        }

        int maxIdFromFolder = 0;
        File gamesDir = new File(GAME_FOLDER);
        File[] gameFolders = gamesDir.listFiles(File::isDirectory);
        if (gameFolders != null) {
            for (File folder : gameFolders) {
                if (folder.getName().startsWith("game_")) {
                    try {
                        int id = Integer.parseInt(folder.getName().substring(5));
                        if (id > maxIdFromFolder) {
                            maxIdFromFolder = id;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }

        currentId = Math.max(maxIdFromTxt, maxIdFromFolder) + 1;
        System.out.println("INFO: ID بعدی برای بازی جدید روی " + currentId + " تنظیم شد.");
    }

    // این متد را در فایل GameFileUtility.java به طور کامل جایگزین کنید

    public static void saveGame(GameOfThrones game) {
        // ۱. اطلاعات پایه بازی را آماده می‌کنیم
        int id = currentId++;
        String fileName = game.getName() + "_" + id + ".ser";
        String folder = "game_" + id;
        String folderPath = GAME_FOLDER + File.separator + folder;

        System.out.println("LOG: Initializing new game structure for ID: " + id + " at path: " + folderPath);

        // ۲. پوشه اصلی بازی را می‌سازیم
        File gameFolder = new File(folderPath);
        if (!gameFolder.exists()) {
            boolean created = gameFolder.mkdirs();
            if (!created) {
                System.err.println("FATAL ERROR: Could not create the main game directory: " + folderPath);
                System.err.println("Please check permissions for the application folder.");
                return; // اگر پوشه اصلی ساخته نشود، ادامه نمی‌دهیم
            }
        }

        // ۳. تمام مسیرهای لازم را روی آبجکت بازی تنظیم می‌کنیم (قبل از ذخیره نهایی)
        game.setId(id);
        game.setFileName(fileName);

        // ۳.الف: مسیر فایل گزارش متنی نبردها
        String battleLogPath = folderPath + File.separator + "battle_log.txt";
        game.setBATTLE_LOG_FILE(battleLogPath);

        // ۳.ب: مسیر پوشه ریپلی‌ها
        String replaysFolderPath = folderPath + File.separator + "replays";
        game.REPLAYS_FOLDER_PATH = replaysFolderPath; // این فیلد public است

        // ۴. فایل گزارش متنی و پوشه ریپلی‌ها را روی دیسک ایجاد می‌کنیم
        try {
            // ساخت پوشه ریپلی
            File replaysDir = new File(replaysFolderPath);
            if (!replaysDir.exists()) {
                replaysDir.mkdirs();
            }
            // ساخت فایل خالی گزارش نبرد
            File recordFile = new File(battleLogPath);
            if (!recordFile.exists()) {
                recordFile.createNewFile();
            }
            System.out.println("SUCCESS: Battle log file and replays folder created successfully.");
        } catch (IOException e) {
            System.err.println("ERROR: Failed to create battle log/replay folder structure: " + e.getMessage());
        }

        // ۵. آبجکت بازی را که حالا مسیرهای صحیح را دارد، در فایل .ser ذخیره می‌کنیم
        String filePath = folderPath + File.separator + fileName;
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(game);
            System.out.println("SUCCESS: Game object saved successfully to: " + filePath);
        } catch (IOException e) {
            System.err.println("ERROR: Could not save the game .ser file: " + e.getMessage());
            e.printStackTrace();
        }

        // ۶. اطلاعات بازی را در لیست لابی (games.txt) ثبت می‌کنیم
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MAIN_FILE, true))) {
            writer.write(game.getName() + "," + game.getPassword() + "," + id + "," + filePath + "," + game.getNumberOfPlayers());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("ERROR: Could not write game info to the main lobby file: " + e.getMessage());
        }

        // ۷. کانفیگ مربوط به بازی را بارگذاری می‌کنیم
        GameConfig.loadGameConfig(game.getId());
    }

    public static GameOfThrones loadGame(String name, int id) {
        String filePath = GAME_FOLDER + File.separator + "game_" + id + File.separator + name + "_" + id + ".ser";
        File gameFile = new File(filePath);

        // --- DIAGNOSTIC LOG 1: Print absolute path on load attempt ---
        System.out.println("<<<<< [LOAD] Attempting to load game from: " + gameFile.getAbsolutePath());

        if (!gameFile.exists()) {
            System.err.println("<<<<< [LOAD] ERROR: Game file not found at the specified path.");
            return null;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(gameFile))) {
            GameOfThrones game = (GameOfThrones) in.readObject();

            if (game != null) {
                game.postLoadUpdate();
            }

            // --- DIAGNOSTIC LOG 2: Print building count immediately after loading ---
            if (game != null && game.getOwner() != null && game.getOwner().getKingdom() != null) {
                int buildingCount = game.getOwner().getKingdom().getBuildings().size();
                System.out.println("<<<<< [LOAD] SUCCESS: Game loaded. Found " + buildingCount + " buildings in the kingdom.");
            } else {
                System.out.println("<<<<< [LOAD] SUCCESS: Game loaded, but owner or kingdom is null.");
            }
            return game;

        } catch (Exception e) {
            System.err.println("<<<<< [LOAD] FATAL ERROR: Exception during deserialization.");
            e.printStackTrace();
            return null;
        }
    }

    private static void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    public static void removeGameFromLobbyList(GameOfThrones game) {
        if (game == null) {
            System.err.println("خطا: تلاش برای حذف یک بازی null از لیست.");
            return;
        }

        File inputFile = new File(MAIN_FILE);
        File tempFile = new File("temp.txt");

        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                // اگر ID بازی با ID خط فعلی یکی نبود، آن را در فایل موقت می‌نویسیم
                if (parts.length < 3 || Integer.parseInt(parts[2]) != game.getId()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("خطا در حذف بازی از لیست لابی: " + e.getMessage());
        }

        inputFile.delete();
        tempFile.renameTo(inputFile);
        System.out.println("INFO: بازی '" + game.getName() + "' از لیست لابی حذف شد و به وضعیت 'در حال اجرا' درآمد.");
    }
    public static void deleteGameFromList(String lineToRemove) {
        File inputFile = new File(MAIN_FILE);
        File tempFile = new File("temp.txt");

        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (!currentLine.equals(lineToRemove)) {
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
        } catch (IOException e) {
            System.err.println("خطا در حذف خط از فایل: " + e.getMessage());
        }

        inputFile.delete();
        tempFile.renameTo(inputFile);
    }

    public static void deleteGame(GameOfThrones game) {
        if (game == null) return;

        String folderPath = GAME_FOLDER + File.separator + "game_" + game.getId();
        File gameFolder = new File(folderPath);

        if (gameFolder.exists()) {
            deleteDirectory(gameFolder);
            System.out.println("INFO: پوشه بازی '" + game.getName() + "' (ID: " + game.getId() + ") با موفقیت حذف شد.");
        }

        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if(game.getUsers()[i] != null){
                game.getUsers()[i].setGame(null);
                game.getUsers()[i].setInGame(false);
                game.getOwner().isInGame = false;
                UserSystem.updateUser(game.getUsers()[i]);
            }
        }

        File inputFile = new File(MAIN_FILE);
        File tempFile = new File("temp.txt");

        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3 || Integer.parseInt(parts[2]) != game.getId()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("خطا در حذف بازی از لیست: " + e.getMessage());
        }

        inputFile.delete();
        tempFile.renameTo(inputFile);
    }


    public static void updateGame(GameOfThrones game) {
        String folder = "game_" + game.getId();
        String newPath = GAME_FOLDER + File.separator + folder + File.separator + game.getName() + "_" + game.getId() + ".ser";
        File gameFile = new File(newPath);

        // --- DIAGNOSTIC LOG 3: Print building count just before saving ---
        int buildingCountBeforeSave = 0;
        if (game.getOwner() != null && game.getOwner().getKingdom() != null) {
            buildingCountBeforeSave = game.getOwner().getKingdom().getBuildings().size();
        }
        System.out.println(">>>>> [SAVE] Preparing to save game to: " + gameFile.getAbsolutePath());
        System.out.println(">>>>> [SAVE] Building count in memory BEFORE save: " + buildingCountBeforeSave);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(gameFile))) {
            out.writeObject(game);
            System.out.println(">>>>> [SAVE] SUCCESS: ObjectOutputStream has successfully written the game object.");
        } catch (IOException e) {
            System.err.println(">>>>> [SAVE] FATAL ERROR: Could not write to the file. See stack trace below.");
            e.printStackTrace();
        }

    }

    public static LinkedList<String> searchGame(String name) {
        LinkedList<String> results = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(MAIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (name.isEmpty() || line.startsWith(name + ",")) {
                    results.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("خطا در جستجو در فایل: " + e.getMessage());
        }
        return results;
    }


    public static void checkGameActivity(LinkedList<String> games) {
        Iterator<String> iterator = games.iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] parts = line.split(",");
            if (parts.length < 3) {
                System.out.println("هشدار: خط نامعتبر در games.txt پیدا و حذف شد: " + line);
                iterator.remove();
                deleteGameFromList(line);
                continue;
            }
            String name = parts[0];
            int id = Integer.parseInt(parts[2]);

            GameOfThrones game = GameFileUtility.loadGame(name, id);

            if (game == null) {
                System.out.println("هشدار: فایل ذخیره برای بازی '" + name + "' (ID: " + id + ") یافت نشد. این رکورد از لیست بازی‌ها پاک می‌شود.");
                iterator.remove();
                deleteGameFromList(line);
                continue;
            }

            long now = System.currentTimeMillis();
            long startTime = game.getGameTime().startTime;
            long daysElapsed = (now - startTime) / (1000 * 60 * 60 * 24);

            if (daysElapsed >= 25) {
                System.out.println("بازی '" + name + "' به دلیل عدم فعالیت (بیش از 25 روز) حذف می‌شود.");
                GameFileUtility.deleteGame(game);
                iterator.remove();
            }
        }
    }

    public static void saveFullGameState(GameOfThrones game) {
        if (game == null) {
            System.err.println("خطا: تلاش برای ذخیره یک بازی null.");
            return;
        }
        updateGame(game);
        syncUsersFromGame(game);
        System.out.println("✅ وضعیت کامل بازی با موفقیت ذخیره و همگام‌سازی شد.");
    }

    public static void syncUsersFromGame(GameOfThrones game) {
        if (game == null) {
            System.out.println("Cannot sync users: game is null.");
            return;
        }
        GameUser[] players = game.getUsers();
        if (players == null) {
            System.out.println("Game has no players.");
            return;
        }
        for (GameUser user : players) {
            if (user != null) {
                UserSystem.updateUser(user);
            }
        }
        System.out.println("Users synced from game successfully.");
    }



    public static Texture getBuildingTexture(String name, int level) {
        if (name == null || name.isEmpty()) {
            return new Texture(Gdx.files.internal("icon/default-0.png"));
        }

        String normalized = normalizeBuildingName(name);

        String fileName = "icon/" + normalized + "-" + level + ".png";

        try {
            return new Texture(Gdx.files.internal(fileName));
        } catch (Exception e) {
            return new Texture(Gdx.files.internal("icon/default-0.png"));
        }
    }

    // این متد را در فایل GameFileUtility.java به طور کامل جایگزین کنید

    private static String normalizeBuildingName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "default"; // Return a base name for default handling
        }
        String normalized = rawName.trim().toUpperCase();

        // --- START OF FIX ---
        // این بخش اضافه شده تا نام پادشاهی‌ها را به درستی شناسایی کند
        switch (normalized) {
            case "WINTERFELL":
            case "STORMS_END":
            case "DRAGONSTONE":
            case "HIGHGARDEN":
            case "EYRIE":
                return normalized.toLowerCase(); // e.g., "winterfell"
            case "IRON_ISLANDS":
                return "iron-islands"; // e.g., "iron-islands"
            case "CASTERLY_ROCK":
                return "casterly-rock"; // e.g., "casterly-rock"
        }
        // --- END OF FIX ---

        // کدهای قبلی برای ساختمان‌ها و منابع بدون تغییر باقی می‌مانند
        if (normalized.equalsIgnoreCase("Wood")) {
            return "Wood";
        }
        if (normalized.equalsIgnoreCase("Stone")) {
            return "Stone";
        }
        if (normalized.equalsIgnoreCase("Food")) {
            return "Food";
        }
        if (normalized.equalsIgnoreCase("Coin")) {
            return "Coin";
        }
        if (normalized.equalsIgnoreCase("Port")) {
            return "port";
        }
        if (normalized.equalsIgnoreCase("Ship")) {
            return "ship";
        }
        if (normalized.equalsIgnoreCase("Castle")) {
            return "castle";
        }
        if (normalized.equalsIgnoreCase("Defensive Tower")) {
            return "defensive-tower";
        }
        if (normalized.equalsIgnoreCase("CrowTower")) {
            return "crowtower";
        }
        if (normalized.equalsIgnoreCase("Camp")) {
            return "camp";
        }
        if (normalized.equalsIgnoreCase("Barrack")) {
            return "barrack";
        }
        if (normalized.equalsIgnoreCase("Transport")) {
            return "transport";
        }
        if (normalized.toLowerCase().startsWith("mine")) {
            String resource = normalized.replaceAll("(?i)mine(\\s*\\(|\\s*of\\s*)", "")
                .replace(")", "")
                .trim();
            return "mine-" + resource.toLowerCase();
        }
        if (normalized.toLowerCase().startsWith("storage")) {
            String resource = normalized.replaceAll("(?i)storage(\\s*\\(|\\s*of\\s*)", "")
                .replace(")", "")
                .trim();
            return "storage-" + resource.toLowerCase();
        }
        return normalized.toLowerCase().replace(" ", "-");
    }
}
