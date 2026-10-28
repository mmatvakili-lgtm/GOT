package com.yourname.got.logic.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * نسخه دیباگ SoundManager برای پیدا کردن دقیق مشکل
 */
public class SoundManager {

    private final ObjectMap<String, Sound> soundMap;
    private final String LOG_TAG = "SOUND_DEBUG";

    public SoundManager() {
        soundMap = new ObjectMap<>();
        Gdx.app.log(LOG_TAG, "✅ SoundManager has been created.");
    }

    /**
     * یک فایل صوتی را بارگذاری می‌کند و موفقیت یا شکست را برمی‌گرداند.
     * @return true اگر فایل با موفقیت بارگذاری شود، در غیر این صورت false.
     */
    public boolean loadSound(String name, String filePath) {
        Gdx.app.log(LOG_TAG, "------------------------------------------");
        Gdx.app.log(LOG_TAG, "🔎 Attempting to load sound '" + name + "' from path: " + filePath);
        if (soundMap.containsKey(name)) {
            Gdx.app.log(LOG_TAG, "⚠️ Sound with this name already loaded. Skipping.");
            return true;
        }

        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
            soundMap.put(name, sound);
            Gdx.app.log(LOG_TAG, "✅✅✅ SUCCESS: Sound '" + name + "' loaded successfully!");
            Gdx.app.log(LOG_TAG, "------------------------------------------");
            return true;
        } catch (Exception e) {
            Gdx.app.error(LOG_TAG, "🔥🔥🔥 FAILED to load sound '" + name + "'. The file was not found or is corrupt.", e);
            Gdx.app.error(LOG_TAG, "------------------------------------------");
            return false;
        }
    }

    /**
     * یک افکت صوتی را پخش می‌کند و موفقیت یا شکست را برمی‌گرداند.
     * @return true اگر دستور پخش با موفقیت ارسال شود، در غیر این صورت false.
     */
    public boolean playSound(String name, float volume) {
        Gdx.app.log(LOG_TAG, "🔊 Attempting to play sound '" + name + "'...");
        Sound soundToPlay = soundMap.get(name);

        if (soundToPlay != null) {
            soundToPlay.play(volume);
            Gdx.app.log(LOG_TAG, "✅ SUCCESS: Play command for '" + name + "' sent!");
            return true;
        } else {
            Gdx.app.error(LOG_TAG, "❌ FAILED to play '" + name + "'. Reason: Sound was never loaded successfully.");
            return false;
        }
    }

    public void dispose() {
        for (Sound sound : soundMap.values()) {
            sound.dispose();
        }
        soundMap.clear();
        Gdx.app.log(LOG_TAG, "SoundManager disposed.");
    }
}
