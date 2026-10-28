package com.yourname.got.logic.util;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.ObjectMap;

public class MusicManager {

    private final ObjectMap<String, Music> musicMap;
    private Music currentlyPlaying;
    private String currentlyPlayingName;
    private float volume = 0.5f;

    public MusicManager() {
        musicMap = new ObjectMap<>();
        currentlyPlaying = null;
        currentlyPlayingName = null;
    }

    public void loadMusic(String name, String filePath) {
        if (musicMap.containsKey(name)) return;

        try {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(filePath));
            musicMap.put(name, music);
        } catch (Exception e) {
            Gdx.app.error("MusicManager", "Could not load music file: " + filePath, e);
        }
    }

    public void playMusic(String name) {
        if (currentlyPlayingName != null && currentlyPlayingName.equals(name)) {
            return;
        }

        stopMusic();

        Music musicToPlay = musicMap.get(name);
        if (musicToPlay != null) {
            musicToPlay.setLooping(true);
            musicToPlay.setVolume(this.volume);
            musicToPlay.play();
            currentlyPlaying = musicToPlay;
            currentlyPlayingName = name;
        } else {
            Gdx.app.log("MusicManager", "Music not found: " + name);
        }
    }

    public void stopMusic() {
        if (currentlyPlaying != null) {
            currentlyPlaying.stop();
            currentlyPlaying = null;
            currentlyPlayingName = null;
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (currentlyPlaying != null) {
            currentlyPlaying.setVolume(this.volume);
        }
    }

    public void dispose() {
        stopMusic();
        for (Music music : musicMap.values()) {
            music.dispose();
        }
        musicMap.clear();
    }
}
