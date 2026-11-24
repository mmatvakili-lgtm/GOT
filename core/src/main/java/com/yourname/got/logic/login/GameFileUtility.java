package com.yourname.got.logic.login;


import com.yourname.got.logic.util.GameLog;
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
    /** Backup copy kept next to every save (see writeObjectAtomically). */
    private static final String BACKUP_SUFFIX = ".bak";
    /** The backup is refreshed at most this often, so frequent saves do not copy the whole file every time. */
    private static final long BACKUP_REFRESH_MILLIS = 60_000L;

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
            GameLog.error("خطا در ساخت فایل/پوشه: " + e.getMessage());
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
            GameLog.error("خطا در خواندن فایل games.txt برای تعیین ID: " + e.getMessage());
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
        GameLog.info("INFO: ID بعدی برای بازی جدید روی " + currentId + " تنظیم شد.");
    }

    // این متد را در فایل GameFileUtility.java به طور کامل جایگزین کنید

    public static void saveGame(GameOfThrones game) {
        // ۱. اطلاعات پایه بازی را آماده می‌کنیم
        int id = currentId++;
        String fileName = game.getName() + "_" + id + ".ser";
        String folder = "game_" + id;
        String folderPath = GAME_FOLDER + File.separator + folder;

        GameLog.info("LOG: Initializing new game structure for ID: " + id + " at path: " + folderPath);

        // ۲. پوشه اصلی بازی را می‌سازیم
        File gameFolder = new File(folderPath);
        if (!gameFolder.exists()) {
            boolean created = gameFolder.mkdirs();
            if (!created) {
                GameLog.error("FATAL ERROR: Could not create the main game directory: " + folderPath);
                GameLog.error("Please check permissions for the application folder.");
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
            GameLog.info("SUCCESS: Battle log file and replays folder created successfully.");
        } catch (IOException e) {
            GameLog.error("ERROR: Failed to create battle log/replay folder structure: " + e.getMessage());
        }

        // ۵. آبجکت بازی را که حالا مسیرهای صحیح را دارد، در فایل .ser ذخیره می‌کنیم
        String filePath = folderPath + File.separator + fileName;
        try {
            writeObjectAtomically(new File(filePath), game);
            GameLog.info("SUCCESS: Game object saved successfully to: " + filePath);
        } catch (IOException e) {
            GameLog.error("ERROR: Could not save the game .ser file: " + e.getMessage());
            e.printStackTrace();
        }

        // ۶. اطلاعات بازی را در لیست لابی (games.txt) ثبت می‌کنیم
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MAIN_FILE, true))) {
            writer.write(game.getName() + "," + game.getPassword() + "," + id + "," + filePath + "," + game.getNumberOfPlayers());
            writer.newLine();
        } catch (IOException e) {
            GameLog.error("ERROR: Could not write game info to the main lobby file: " + e.getMessage());
        }

        // ۷. کانفیگ مربوط به بازی را بارگذاری می‌کنیم
        GameConfig.loadGameConfig(game.getId());
    }

    public static GameOfThrones loadGame(String name, int id) {
        String filePath = GAME_FOLDER + File.separator + "game_" + id + File.separator + name + "_" + id + ".ser";
        File gameFile = new File(filePath);
        File backupFile = new File(filePath + BACKUP_SUFFIX);

        // --- DIAGNOSTIC LOG 1: Print absolute path on load attempt ---
        GameLog.info("<<<<< [LOAD] Attempting to load game from: " + gameFile.getAbsolutePath());

        GameOfThrones game = gameFile.exists() ? readGameFile(gameFile) : null;
        if (game != null) return game;

        // The main save is missing or unreadable (e.g. the disk was full or the PC lost power): use the backup
        // instead of dropping the whole game.
        if (backupFile.exists()) {
            GameLog.error("<<<<< [LOAD] WARNING: Main save unusable, trying the backup: " + backupFile.getAbsolutePath());
            game = readGameFile(backupFile);
            if (game != null) {
                GameLog.error("<<<<< [LOAD] Recovered the game from its backup (the next save rewrites the main file).");
                return game;
            }
        }
        if (!gameFile.exists()) {
            GameLog.error("<<<<< [LOAD] ERROR: Game file not found at the specified path.");
        }
        return null;
    }

    /** Reads one serialized game; null if the file cannot be read. */
    private static GameOfThrones readGameFile(File gameFile) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(gameFile))) {
            GameOfThrones game = (GameOfThrones) in.readObject();

            if (game != null) {
                game.postLoadUpdate();
            }

            // --- DIAGNOSTIC LOG 2: Print building count immediately after loading ---
            if (game != null && game.getOwner() != null && game.getOwner().getKingdom() != null) {
                int buildingCount = game.getOwner().getKingdom().getBuildings().size();
                GameLog.info("<<<<< [LOAD] SUCCESS: Game loaded. Found " + buildingCount + " buildings in the kingdom.");
            } else {
                GameLog.info("<<<<< [LOAD] SUCCESS: Game loaded, but owner or kingdom is null.");
            }
            return game;

        } catch (Exception e) {
            GameLog.error("<<<<< [LOAD] FATAL ERROR: Exception during deserialization of " + gameFile.getName());
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
            GameLog.error("خطا: تلاش برای حذف یک بازی null از لیست.");
            return;
        }

        File inputFile = new File(MAIN_FILE);
        File tempFile = new File("temp.txt");
        boolean listWritten = false;

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
            listWritten = true;
        } catch (IOException e) {
            listWritten = false; // closing the writer failed too: the temp file may be incomplete
            GameLog.error("خطا در حذف بازی از لیست لابی: " + e.getMessage());
        }

        if (listWritten) {
            com.yourname.got.logic.util.SafeFiles.replaceQuietly(tempFile, inputFile);
        } else {
            tempFile.delete(); // keep the old, complete list
        }
        GameLog.info("INFO: بازی '" + game.getName() + "' از لیست لابی حذف شد و به وضعیت 'در حال اجرا' درآمد.");
    }
    public static void deleteGameFromList(String lineToRemove) {
        File inputFile = new File(MAIN_FILE);
        File tempFile = new File("temp.txt");
        boolean listWritten = false;

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
            listWritten = true;
        } catch (IOException e) {
            listWritten = false; // closing the writer failed too: the temp file may be incomplete
            GameLog.error("خطا در حذف خط از فایل: " + e.getMessage());
        }

        if (listWritten) {
            com.yourname.got.logic.util.SafeFiles.replaceQuietly(tempFile, inputFile);
        } else {
            tempFile.delete(); // keep the old, complete list
        }
    }

    public static void deleteGame(GameOfThrones game) {
        if (game == null) return;

        String folderPath = GAME_FOLDER + File.separator + "game_" + game.getId();
        File gameFolder = new File(folderPath);

        if (gameFolder.exists()) {
            deleteDirectory(gameFolder);
            GameLog.info("INFO: پوشه بازی '" + game.getName() + "' (ID: " + game.getId() + ") با موفقیت حذف شد.");
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
        boolean listWritten = false;

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
            listWritten = true;
        } catch (IOException e) {
            listWritten = false; // closing the writer failed too: the temp file may be incomplete
            GameLog.error("خطا در حذف بازی از لیست: " + e.getMessage());
        }

        if (listWritten) {
            com.yourname.got.logic.util.SafeFiles.replaceQuietly(tempFile, inputFile);
        } else {
            tempFile.delete(); // keep the old, complete list
        }
    }


    /**
     * games.txt holds one line per game: name,password,id,path,playerCount.
     * The count was written once when the lobby was created and never updated, so the game list kept
     * showing "1/7" however many players had joined. This rewrites the last field with the real number.
     */
    public static synchronized void syncLobbyEntry(GameOfThrones game) {
        if (game == null) return;
        File f = new File(MAIN_FILE);
        if (!f.exists()) return;
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                int id;
                try { id = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { continue; }
                if (id != game.getId()) continue;
                String updated = line.substring(0, line.lastIndexOf(',') + 1) + game.getNumberOfPlayers();
                if (!updated.equals(line)) { lines.set(i, updated); changed = true; }
            }
            if (changed) java.nio.file.Files.write(f.toPath(), lines);
        } catch (IOException e) {
            GameLog.error("Could not update the lobby list: " + e.getMessage());
        }
    }

    /** The same line as it should currently read (used to refresh the list shown in the lobby). */
    public static String lobbyLineFor(String oldLine, GameOfThrones game) {
        return oldLine.substring(0, oldLine.lastIndexOf(',') + 1) + game.getNumberOfPlayers();
    }

    /** Copies the current (still valid) save to "<name>.bak" before it is replaced; at most once a minute. */
    private static void refreshBackup(File target) {
        try {
            if (!target.exists() || target.length() == 0) return;
            File backup = new File(target.getPath() + BACKUP_SUFFIX);
            if (backup.exists() && System.currentTimeMillis() - backup.lastModified() < BACKUP_REFRESH_MILLIS) return;
            java.nio.file.Files.copy(target.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            GameLog.error("Could not refresh the save backup: " + e.getMessage());
        }
    }

    /**
     * Writes the object to a temporary file next to the target and only then replaces the target.
     * A crash or a full disk in the middle of a save used to leave a half-written (unreadable) .ser file,
     * i.e. the whole game was lost. Now the previous save stays intact until the new one is complete.
     */
    private static void writeObjectAtomically(File target, Object object) throws IOException {
        File tmp = new File(target.getPath() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmp))) {
            out.writeObject(object);
        } catch (IOException | RuntimeException e) {
            tmp.delete();
            throw e;
        }
        refreshBackup(target);
        try {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
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
        GameLog.info(">>>>> [SAVE] Preparing to save game to: " + gameFile.getAbsolutePath());
        GameLog.info(">>>>> [SAVE] Building count in memory BEFORE save: " + buildingCountBeforeSave);

        try {
            writeObjectAtomically(gameFile, game);
            GameLog.info(">>>>> [SAVE] SUCCESS: ObjectOutputStream has successfully written the game object.");
            syncLobbyEntry(game);
        } catch (IOException e) {
            GameLog.error(">>>>> [SAVE] FATAL ERROR: Could not write to the file. See stack trace below.");
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
            GameLog.error("خطا در جستجو در فایل: " + e.getMessage());
        }
        return results;
    }


    public static void checkGameActivity(LinkedList<String> games) {
        java.util.ListIterator<String> iterator = games.listIterator();

        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] parts = line.split(",");
            if (parts.length < 3) {
                GameLog.info("هشدار: خط نامعتبر در games.txt پیدا و حذف شد: " + line);
                iterator.remove();
                deleteGameFromList(line);
                continue;
            }
            String name = parts[0];
            int id = Integer.parseInt(parts[2]);

            GameOfThrones game = GameFileUtility.loadGame(name, id);

            if (game == null) {
                GameLog.info("هشدار: فایل ذخیره برای بازی '" + name + "' (ID: " + id + ") یافت نشد. این رکورد از لیست بازی‌ها پاک می‌شود.");
                iterator.remove();
                deleteGameFromList(line);
                continue;
            }

            if (parts.length >= 5) { // show (and store) the real number of players
                iterator.set(lobbyLineFor(line, game));
                syncLobbyEntry(game);
            }

            long now = System.currentTimeMillis();
            long startTime = game.getGameTime().startTime;
            long daysElapsed = (now - startTime) / (1000 * 60 * 60 * 24);

            if (daysElapsed >= 25) {
                GameLog.info("بازی '" + name + "' به دلیل عدم فعالیت (بیش از 25 روز) حذف می‌شود.");
                GameFileUtility.deleteGame(game);
                iterator.remove();
            }
        }
    }

    // --- throttled saving -------------------------------------------------------------------------------
    // Saving the whole game (plus every user file) takes a noticeable moment and TrainingTask did it for EVERY
    // trained soldier. Hot paths use saveFullGameStateSoon(): it saves at most every 2 seconds and the game loop
    // flushes what is left (flushPendingSave), so at most a couple of seconds of progress are at risk.
    private static long lastFullSaveMillis = 0;
    private static GameOfThrones pendingSave = null;

    public static synchronized void saveFullGameStateSoon(GameOfThrones game) {
        if (game == null) return;
        if (System.currentTimeMillis() - lastFullSaveMillis >= 2000) {
            saveFullGameState(game);
        } else {
            pendingSave = game;
        }
    }

    public static synchronized void flushPendingSave() {
        if (pendingSave != null) {
            saveFullGameState(pendingSave);
        }
    }

    public static void saveFullGameState(GameOfThrones game) {
        if (game == null) {
            GameLog.error("خطا: تلاش برای ذخیره یک بازی null.");
            return;
        }
        lastFullSaveMillis = System.currentTimeMillis();
        if (pendingSave == game) pendingSave = null;
        updateGame(game);
        syncUsersFromGame(game);
        GameLog.info("✅ وضعیت کامل بازی با موفقیت ذخیره و همگام‌سازی شد.");
    }

    public static void syncUsersFromGame(GameOfThrones game) {
        if (game == null) {
            GameLog.info("Cannot sync users: game is null.");
            return;
        }
        GameUser[] players = game.getUsers();
        if (players == null) {
            GameLog.info("Game has no players.");
            return;
        }
        for (GameUser user : players) {
            if (user != null) {
                UserSystem.updateUser(user);
            }
        }
        GameLog.info("Users synced from game successfully.");
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
