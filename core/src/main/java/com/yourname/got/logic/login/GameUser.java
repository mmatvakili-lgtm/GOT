package com.yourname.got.logic.login;


import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.model.*;
import java.io.Serializable;

public class GameUser  implements Serializable{
    private String username;
    private String password;
    private int id;
    private GameOfThrones game;
    private Kingdom kingdom;
    public Boolean isInGame;
    public UserSubMap userMap;
    private  long lastVisit;
    private boolean isLose;
    protected boolean inFight;

    public GameUser(String username, String password, int id) {
        this.username = username;
        this.password = password;
        this.id = id;
        isInGame=false;
        game=null;
        kingdom=null;
        lastVisit=System.currentTimeMillis();
        isLose=false;
        inFight=false;

    }

    public long getLastVisit() {
        return lastVisit;
    }

    public String getUsername() {
        return username;
    }
    public  void updateLastVisit(){
        lastVisit=System.currentTimeMillis();
    }
    public boolean isActive() {

        return true;
    }

    public void setKingdom(Kingdom kingdom) {
        this.kingdom = kingdom;
        kingdom.setOwner(this);
        this.userMap=kingdom.getUserMap();
    }

    public void setInFight(boolean inFight) {
        this.inFight = inFight;
    }

    public boolean isInFight() {
        return inFight;
    }

    public String getPassword() {
        return password;
    }


    public Boolean getInGame() {
        return isInGame;
    }

    public void setGame(GameOfThrones game) {
        this.game = game;
    }

    public  GameOfThrones getGame(){
        return game;
    }
    public int getId(){
        return this.id;
    }

    @Override
    public String toString() {
        return username+" :"+this.kingdom.getType();
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public boolean isLose() {
        return isLose;
    }

    public void setInGame(Boolean inGame) {
        isInGame = inGame;
    }

    public long getTime() {
        return lastVisit;
    }

    public void receiveMessage(String message) {
        GameLog.info("[GAME MESSAGE to " + username + "]: " + message);
    }

    public void setPassword(String newPassword) {
        this.password = newPassword;
    }
}
