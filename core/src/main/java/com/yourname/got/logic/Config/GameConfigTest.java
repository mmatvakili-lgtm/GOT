package com.yourname.got.logic.Config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class GameConfigTest {

    private static final String GLOBAL_CONFIG_PATH = "config.properties";
    private static Properties globalProps = new Properties();

    public static void main(String[] args) {
        System.out.println("🧪 شروع تست کانفیگ...");

        File file = new File(GLOBAL_CONFIG_PATH);

        // مرحله 1: ساخت فایل اگر وجود ندارد
        if (!file.exists()) {
            System.out.println("⚠ فایل کانفیگ وجود ندارد. در حال ساخت...");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("""
                    allowedDifficulties=easy,medium,hard
                    allowedSpeeds=1,2,5,10,100,1000
                    defaultDifficulty=medium
                    defaultSpeed=1
                    game.speed=10.0
                    game.turn.duration=30
                    resource.generation.interval=5
                    whitewalker.attack.interval=100
                    """);
                System.out.println("✅ فایل کانفیگ ساخته شد.");
            } catch (IOException e) {
                System.err.println("⛔ خطا در ساخت فایل کانفیگ:");
                e.printStackTrace();
                return;
            }
        }

        // مرحله 2: بارگذاری فایل
        try (FileReader reader = new FileReader(file)) {
            globalProps.load(reader);
            System.out.println("✅ فایل کانفیگ با موفقیت بارگذاری شد.");
        } catch (IOException e) {
            System.err.println("⛔ خطا در بارگذاری فایل کانفیگ:");
            e.printStackTrace();
            return;
        }

        printKey("defaultSpeed");
        printKey("defaultDifficulty");
        printKey("allowedSpeeds");
        printKey("allowedDifficulties");
        printKey("game.turn.duration");
        printKey("resource.generation.interval");
        printKey("whitewalker.attack.interval");

        System.out.println("🧪 تست کانفیگ به پایان رسید.");
    }

    private static void printKey(String key) {
        String value = globalProps.getProperty(key);
        if (value == null) {
            System.out.println("❌ کلید '" + key + "' یافت نشد.");
        } else {
            System.out.println("🔑 " + key + " = " + value);
        }
    }
}
