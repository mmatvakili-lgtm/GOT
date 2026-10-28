package com.yourname.got.lwjgl3;//package lwjgl3.src.main.java.com.yourname.got.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.yourname.got.GoTGame;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (com.yourname.got.lwjgl3.StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new GoTGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("got");

        // ۱. قفل کردن تغییر سایز تا هیچ کدام از اسکرین‌ها و بک‌گراندها به هم نریزند
        configuration.setResizable(false);

        // ۲. استفاده از Vsync برای روانی فریم‌ریت و ضبط بدون لگ در OBS
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        // ۳. تعیین اندازه استاندارد پنجره که با طراحی اصلی بازی همخوان است:
        // (اگر رزولوشن اصلی بازی شما 1920x1080 یا 1280x720 است، همان را قرار دهید)
        configuration.setWindowedMode(1920, 1080);

        // آیکون‌های پنجره (اگر در کد فعلی‌تان وجود دارد دست نزنید)
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }}
