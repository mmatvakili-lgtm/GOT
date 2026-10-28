package com.yourname.got.logic.login;

import com.yourname.got.logic.enums.Seasons;
import java.io.Serializable;

public class Time implements Serializable {
    private static final long serialVersionUID = 2128945817388447409L;
    public   Long startTime;
    public   int day;
    public Seasons season;
    public int gameSpeed;
    public Time(long createGameTime) {
        startTime=createGameTime;
        day=1;
        season=Seasons.SPRING;

    }

    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = gameSpeed;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public int getDay() {
        return day;
    }
    public  int getTimeForDistance(int distance,Double speedMov){
        return (int)(distance/(speedMov*gameSpeed));//معادل20 دقیقه به ازای هر کاشی
    }
    public void updateTime() {
        day = gameFormatTimeDay(startTime, gameSpeed);

        try {
            int seasonIndex = (day / 25) % 4;
            season = Seasons.fromInt(seasonIndex);
            // >> END: کد اصلاح شده <<
        } catch (Exception e) {
            System.out.println("Game is out of date");
        }
    }

    public void setDay(int day) {
        this.day = day;
    }
    public static int gameFormatTimeDay(Long start,int gameSpeed) {
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - start) / 1000;
        return (int)((elapsedSeconds * gameSpeed) / (24 * 60 * 60)) + 1;
    }

    public String getFormattedElapsedTime() {
        if (startTime == null) {
            return "هنوز شروع نشده";
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        long elapsedRealSeconds = elapsedMillis / 1000;

        long totalGameSeconds = elapsedRealSeconds * gameSpeed;

        long days = totalGameSeconds / (24 * 60 * 60);
        long remainingSecondsAfterDays = totalGameSeconds % (24 * 60 * 60);
        long hours = remainingSecondsAfterDays / 3600;
        long remainingSecondsAfterHours = remainingSecondsAfterDays % 3600;
        long minutes = remainingSecondsAfterHours / 60;

        return String.format("%d روز, %d ساعت و %d دقیقه", days, hours, minutes);
    }
    public Long getStartTime() {
        return startTime;
    }

    public Seasons getSeason() {
        return season;
    }
    public void showGameTime(){
        updateTime();
        System.out.println("season:"+season+"\nday"+day);
    }
    public String getCalendarString() {
        int year = (day - 1) / 100 + 1;
        int dayOfYear = (day - 1) % 100 + 1;
        return String.format("Year %d, Day %d, Season %s", year, dayOfYear, season);
    }

    public static int getTimeForDistance(int distance){
        return distance*60*20;
    }

    public int getSeasonDay() {
        return (day - 1) % 25 + 1;
    }

    public boolean isWinter() {
        return season == Seasons.WINTER;
    }
    public static String formatSeconds(int totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }}
