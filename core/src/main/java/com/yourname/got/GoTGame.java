package com.yourname.got;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.util.SoundManager;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.mediator.GameMessageListener;
import com.yourname.got.screens.LoginScreen;
import com.yourname.got.screens.RegisterScreen;
import com.yourname.got.screens.GameLoadingScreen;
import com.yourname.got.logic.util.MusicManager;
import com.badlogic.gdx.audio.Sound; // این import را در بالای فایل اضافه کنید


public class GoTGame extends Game implements GameMessageListener {

    private static GoTGame instance;

    public AssetManager assetManager;
    private GameMediator mediator;
    private MusicManager musicManager;
    public SoundManager soundManager; // <-- این خط را اضافه کنید

    public GoTGame() {
        instance = this;
    }

    public static GoTGame getInstance() {
        return instance;
    }

    @Override
    public void create() {
        // ========== کد تست صدا ==========
        // In file: GoTGame.java -> inside the create() method

// این خط را با بلوک کد زیر جایگزین کنید

        assetManager = new AssetManager();
        mediator = new GameMediator(this);
        soundManager = new SoundManager(); // <-- این خط را اضافه کنید

        musicManager = new MusicManager();

        musicManager.loadMusic("main_menu", "audio/main_menu_theme.mp3");
        musicManager.loadMusic("gameplay_snow", "audio/winterfell_theme.mp3");
        musicManager.loadMusic("gameplay_sea", "audio/iron_islands_theme.mp3");
        musicManager.loadMusic("gameplay_rock", "audio/lannister_theme.mp3");

        musicManager.loadMusic("gameplay_winter", "audio/gameplay_winter.mp3");
        musicManager.loadMusic("gameplay_eyrie", "audio/gameplay_eyrie.mp3");
        musicManager.loadMusic("gameplay_dragon", "audio/gameplay_dragon.mp3");
        musicManager.loadMusic("gameplay_storm", "audio/gameplay_storm.mp3");
        musicManager.loadMusic("gameplay_highgarden", "audio/gameplay_highgarden.mp3");
        musicManager.loadMusic("default_gameplay", "audio/default_game_theme.mp3");
        musicManager.loadMusic("login_intro", "audio/got.mp3");
        musicManager.loadMusic("music_defeat", "audio/music_defeat.mp3");
        musicManager.loadMusic("music_victory", "audio/music_victory.mp3");
        musicManager.loadMusic("music_game_over", "audio/music_game_over.mp3");
// کد اصلاح‌شده
// در فایل GoTGame.java
        soundManager.loadSound("notification", "sounds/notification.wav");        setScreen(new LoginScreen(this, mediator));

    }

    public MusicManager getMusicManager() {
        return musicManager;
    }


    @Override
    public void dispose() {
        super.dispose();

        if (musicManager != null) {
            musicManager.dispose();
        }
        if (soundManager != null) {
            soundManager.dispose(); // <-- این خط را اضافه کنید
        }

        if (assetManager != null) {
            assetManager.dispose();
        }
    }

    public GameMediator getMediator() {
        return mediator;
    }

    public void showMessageToScreen(String message) {
        if (getScreen() instanceof GameMessageListener receiver) {
            receiver.onMessage(message);
        }
    }
    // یک متد برای دسترسی راحت‌تر اضافه کنید
    public SoundManager getSoundManager() {
        return soundManager;
    }
    public void goToRegisterScreen() {
        setScreen(new RegisterScreen(this, mediator));
    }

    public void goToLoginScreen() {
        setScreen(new LoginScreen(this, mediator));
    }

    public void goToGameLoadingScreen(GameUser user) {
        setScreen(new GameLoadingScreen(this, mediator, user));
    }

    @Override
    public void onMessage(String text) {
    }
}
