package com.yourname.got.logic.util;

import com.yourname.got.logic.login.GameUser;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Added import for DateTimeFormatter


public class Notice  implements Serializable{
    public static int counter=1;
    public int letterNumber;
    public GameUser sender;
    public String Title;
    public String note;
    public GameUser receiver;
    public  int durationSeconds;
    public LocalDateTime sendTime;
    public Boolean isRead;


    public Notice(int sec, GameUser sender, GameUser receiver, String title, String note) {
        this.note = note;
        this.durationSeconds = sec;
        this.sender = sender;
        this.receiver = receiver;
        this.Title = title;
        letterNumber = counter++;
        sendTime = LocalDateTime.now();
        this.isRead = false;
    }

    public Notice(String title) {
        this.Title=title;
    }

    public boolean isRead() {
        return isRead;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public String getSenderUsername() {

        return (sender != null) ? sender.getUsername() : "سیستم بازی";
    }
    public String getReceiverUsername() {
        return (receiver != null) ? receiver.getUsername() : "All Kingdoms";
    }
}
