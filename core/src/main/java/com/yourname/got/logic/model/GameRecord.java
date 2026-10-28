package com.yourname.got.logic.model;

import com.yourname.got.logic.login.PlayerResult;
import com.yourname.got.logic.util.MyLinkedList;

import java.io.Serializable;
import java.util.Properties;

public class GameRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameName;
    private Properties gameConfig;
    private String gameDuration;
    private MyLinkedList<PlayerResult> playerResults;
    private GameResult finalGameResult; // نتیجه کلی بازی

    public enum GameResult {
        LAST_PLAYER_STANDING,
        WHITE_WALKERS_WON,
        TIME_UP,
        SURVIVED_THE_LONG_NIGHT // <<<< وضعیت جدید
    }

    public GameRecord(String gameName, Properties config, String duration, GameResult result) {
        this.gameName = gameName;
        this.gameConfig = config;
        this.gameDuration = duration;
        this.finalGameResult = result;
        this.playerResults = new MyLinkedList<>();
    }

    public void addPlayerResult(PlayerResult result) {
        playerResults.add(result);
    }

    public String getGameName() {
        return gameName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("   📜 سابقه بازی: ").append(gameName).append(" 📜\n");
        sb.append("========================================\n");
        sb.append("🕒 مدت زمان بازی: ").append(gameDuration).append("\n");
        sb.append("⚙️ نتیجه نهایی: ").append(finalGameResult).append("\n\n");
        sb.append("--- 🏆 رتبه‌بندی بازیکنان 🏆 ---\n");

        int rank = 1;
        for (PlayerResult pr : playerResults) {
            sb.append(rank++).append(". ").append(pr.toString()).append("\n");
        }
        sb.append("-------------------------------------\n");
        return sb.toString();
    }

    public GameResult getFinalGameResult() {
        return finalGameResult;
    }

    public String getGameDuration() {
        return gameDuration;
    }

    public MyLinkedList<PlayerResult> getPlayerResults() {
        return playerResults;
    }
}
