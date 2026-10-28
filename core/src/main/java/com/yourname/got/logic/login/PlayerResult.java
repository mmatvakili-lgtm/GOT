package com.yourname.got.logic.login;

import java.io.Serializable;

public class PlayerResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private int score;
    private PlayerStatus status;
    private int finalRank;

    public String getUsername() {
        return username;
    }

    public enum PlayerStatus {
        DEFEATED,
        SURVIVED,
        WINNER
    }

    public int getScore() {
        return score;
    }

    public PlayerResult(String username, int score, PlayerStatus status, int rank) {
        this.username = username;
        this.score = score;
        this.status = status;
        this.finalRank = rank;
    }

    public PlayerStatus getStatus(){
        return this.status;
    }

    public int getFinalRank(){
        return this.finalRank;
    }
    @Override
    public String toString() {
        return "بازیکن: " + username + " | امتیاز: " + score + " | وضعیت: " + status;
    }
}
