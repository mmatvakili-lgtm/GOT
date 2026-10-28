package com.yourname.got.logic.login;

import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.MyLinkedList;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BattleLogger {

    // این متد را در فایل BattleLogger.java به طور کامل جایگزین کنید
    public static void logBattle(Mission mission, int stars, String logFileName, String replayFilePath,
                                 int woodLoot, int stoneLoot, int foodLoot, int coinLoot) {

        // ماموریت و مدافع حتما باید وجود داشته باشند
        if (mission == null || mission.getDefender() == null || logFileName == null || logFileName.isEmpty()) {
            System.err.println("خطا در لاگ کردن نبرد: اطلاعات ماموریت یا نام فایل ناقص است.");
            return;
        }

        try (FileWriter fw = new FileWriter(logFileName, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String attackerName;
            // ---- START OF FIX ----
            // اگر مهاجم null بود (یعنی وایت واکرها حمله کرده‌اند)، از یک نام جایگزین استفاده کن
            if (mission.getAttacker() == null) {
                attackerName = "White Walkers";
            } else {
                attackerName = mission.getAttacker().getUsername();
            }
            // ---- END OF FIX ----

            String defenderName = mission.getDefender().getUsername();
            String troopComposition = getTroopComposition(mission.getAttackerTroops());
            String timestamp = mission.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String record = String.join(";",
                attackerName,
                defenderName,
                String.valueOf(stars),
                troopComposition,
                timestamp,
                (replayFilePath != null ? replayFilePath : "N/A"), // اطمینان از null نبودن مسیر ریپلی
                String.valueOf(woodLoot),
                String.valueOf(stoneLoot),
                String.valueOf(foodLoot),
                String.valueOf(coinLoot)
            );

            pw.println(record);

        } catch (IOException e) {
            System.err.println("خطا در نوشتن لاگ نبرد در فایل " + logFileName + ": " + e.getMessage());
        }
    }
    private static String getTroopComposition(SoldierUnit[] troops) {
        if (troops == null) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (SoldierUnit unit : troops) {
            if (unit != null && unit.getCount() > 0) {
                if (sb.length() > 0) sb.append(",");
                sb.append(unit.getType().name()).append(":").append(unit.getCount());
            }
        }
        return sb.toString();
    }

    // در فایل BattleLogger.java
    public static String saveReplayLog(MyLinkedList<BattleEvent> replayLog, GameUser attacker) {
        // این متد دیگر استفاده نخواهد شد، اما برای جلوگیری از خطا آن را نگه می‌داریم
        // و یک نسخه جدید با ورودی صحیح می‌سازیم

        return "N/A";
    }

    // **** این متد جدید و اصلاح شده است ****
    public static String saveReplayLog(MyLinkedList<BattleEvent> replayLog, GameOfThrones game, Mission mission) {
        if (replayLog == null || game == null || mission == null) {
            return "N/A";
        }

        String replaysFolder = game.getReplaysFolderPath();
        if (replaysFolder == null || replaysFolder.isEmpty()) {
            System.err.println("ERROR: Replays folder path is not set in the game object!");
            return "N/A";
        }

        File folder = new File(replaysFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = "replay_" + UUID.randomUUID().toString() + ".rep";
        String filePath = replaysFolder + File.separator + fileName;

        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            ReplayData replayData = new ReplayData();
            replayData.events = replayLog;
            replayData.attackerName = (mission.getAttacker() != null ? mission.getAttacker().getUsername() : "White Walkers");
            replayData.defenderName = mission.getDefender().getUsername();
            replayData.initialTroops = mission.getAttackerTroops();
            System.out.println("SAVE DEBUG: Events to save = " + (replayLog != null ? replayLog.size() : "null"));

            oos.writeObject(replayData);
            return filePath;
        } catch (IOException e) {
            System.err.println("Error saving battle replay file: " + e.getMessage());
            return "N/A";
        }
    }
}
