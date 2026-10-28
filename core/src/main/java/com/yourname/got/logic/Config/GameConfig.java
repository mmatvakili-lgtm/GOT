package com.yourname.got.logic.Config;

import com.yourname.got.logic.util.MyLinkedList;

import java.io.*;
import java.util.Properties;

public class GameConfig {

    private static final String GLOBAL_CONFIG_PATH = "Config.properties";
    private static final String GAME_FOLDER = "GAMES";

    private static Properties globalProps = new Properties();
    public static Properties loadGlobalConfig() {
        Properties globalProps = new Properties();
        File file = new File("config.properties");

        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("""
                allowedDifficulties=easy,medium,hard
                allowedSpeeds=1,2,5,10,100,1000
                defaultDifficulty=medium
                defaultSpeed=1
                game.turn.duration=30
                resource.generation.interval=5
                whitewalker.attack.interval=10
                """);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(file)) {
            globalProps.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return globalProps;
    }

    static {
        try {
            File file = new File(GLOBAL_CONFIG_PATH);
            if (!file.exists()) {

                globalProps.setProperty("allowedSpeeds", "1,2,5,10,100,1000");
                globalProps.setProperty("allowedDifficulties", "easy,medium,hard");
                globalProps.setProperty("defaultSpeed", "1");
                globalProps.setProperty("defaultDifficulty", "medium");
                globalProps.setProperty("game.turn.duration", "30");
                globalProps.setProperty("resource.generation.interval", "5");
                globalProps.setProperty("whitewalker.attack.interval", "10");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    globalProps.store(fos, "Global Configuration");
                }
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    globalProps.load(fis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ بارگذاری GlobalConfig با مشکل مواجه شد", e);
        }
    }
    // در فایل GameConfig.java
    public static Properties loadGameConfig(int gameId) {
        if (gameId <= 0) {
            return new Properties(globalProps);
        }

        Properties gameProps = new Properties();
        File gameConfigFile = new File(GAME_FOLDER + File.separator + "game_" + gameId + File.separator + "config.properties");

        if (!gameConfigFile.exists()) {
            try {
                gameConfigFile.getParentFile().mkdirs();
                gameConfigFile.createNewFile();

                setDefaults(gameProps);

                try (FileOutputStream fos = new FileOutputStream(gameConfigFile)) {
                    gameProps.store(fos, "Game-specific Configuration");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileInputStream fis = new FileInputStream(gameConfigFile)) {
                gameProps.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean updated = false;

            updated |= setIfMissing(gameProps, "game.speed", globalProps.getProperty("defaultSpeed"));
            updated |= setIfMissing(gameProps, "game.difficulty", globalProps.getProperty("defaultDifficulty"));
            updated |= setIfMissing(gameProps, "game.turn.duration", globalProps.getProperty("game.turn.duration"));
            updated |= setIfMissing(gameProps, "resource.generation.interval", globalProps.getProperty("resource.generation.interval"));
            updated |= setIfMissing(gameProps, "whitewalker.attack.interval", globalProps.getProperty("whitewalker.attack.interval"));

            if (updated) {
                try (FileOutputStream fos = new FileOutputStream(gameConfigFile)) {
                    gameProps.store(fos, "Updated missing values");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return gameProps;
    }

    private static boolean setIfMissing(Properties props, String key, String defaultValue) {
        if (props.getProperty(key) == null && defaultValue != null) {
            props.setProperty(key, defaultValue);
            return true;
        }
        return false;
    }

    private static void setDefaults(Properties props) {
        props.setProperty("game.speed", globalProps.getProperty("defaultSpeed"));
        props.setProperty("game.difficulty", globalProps.getProperty("defaultDifficulty"));
        props.setProperty("game.turn.duration", globalProps.getProperty("game.turn.duration"));
        props.setProperty("resource.generation.interval", globalProps.getProperty("resource.generation.interval"));
        props.setProperty("whitewalker.attack.interval", globalProps.getProperty("whitewalker.attack.interval"));
    }

    public static boolean isValidConfig(String speed, String difficulty) {
        MyLinkedList<String> allowedSpeeds = new MyLinkedList<>();
        for (String s : globalProps.getProperty("allowedSpeeds").split(",")) {
            allowedSpeeds.add(s);
        }

        MyLinkedList<String> allowedDifficulties = new MyLinkedList<>();
        for (String d : globalProps.getProperty("allowedDifficulties").split(",")) {
            allowedDifficulties.add(d);
        }

        return allowedSpeeds.contains(speed) && allowedDifficulties.contains(difficulty);
    }

    public static boolean updateGlobalConfig(String key, String value) {
        globalProps.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(GLOBAL_CONFIG_PATH)) {
            globalProps.store(fos, "Global Configuration Updated");
            return true;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return false;
    }

    public static boolean updateGameConfig(int gameId, String key, String value) {
        Properties gameProps = loadGameConfig(gameId);

        if (key.equals("game.speed")) {
            MyLinkedList<String> allowedSpeeds = new MyLinkedList<>();
            for (String s : globalProps.getProperty("allowedSpeeds").split(",")) {
                allowedSpeeds.add(s);
            }
            if (!allowedSpeeds.contains(value)) return false;
        }

        if (key.equals("game.difficulty")) {
            MyLinkedList<String> allowedDifficulties = new MyLinkedList<>();
            for (String d : globalProps.getProperty("allowedDifficulties").split(",")) {
                allowedDifficulties.add(d);
            }
            if (!allowedDifficulties.contains(value)) return false;
        }

        gameProps.setProperty(key, value);

        try (FileOutputStream fos = new FileOutputStream(GAME_FOLDER + "/game_" + gameId + "/config.properties")) {
            gameProps.store(fos, "Updated Game Config");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static Properties getGlobalProps() {
        return globalProps;
    }
    public static String getGameProperty(int gameId, String key) {
        try {
            Properties props = loadGameConfig(gameId);
            return props.getProperty(key);
        } catch (Exception e) {
            System.out.println("❌ خطا در خواندن فایل تنظیمات بازی: " + e.getMessage());
            return null;
        }
    }
    public static String getGlobalProperty(String key) {
        try {
            Properties globalProps = loadGlobalConfig();
            return globalProps.getProperty(key);
        } catch (Exception e) {
            System.out.println("❌ خطا در خواندن GlobalConfig: " + e.getMessage());
            return null;
        }
    }
    public static int adjustTime(int baseTimeInMinutes, int speed) {
        return Math.max(5, (int) Math.ceil((double) baseTimeInMinutes * 60 / speed));
    }

    public static String formatAdjustedTimeVerbose(int baseTimeInMinutes, int speed) {
        int originalSeconds = baseTimeInMinutes * 60;
        int adjustedSeconds = adjustTime(baseTimeInMinutes, speed);
        int minutes = adjustedSeconds / 60;
        int seconds = adjustedSeconds % 60;
        int reduction = 100 - (adjustedSeconds * 100 / originalSeconds);
        return String.format("⏱ %02d:%02d (Speed: %dx, -%d%%)", minutes, seconds, speed, reduction);
    }

}
