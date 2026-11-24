package com.yourname.got.logic.util;


import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.login.GameUser;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public class Post extends MyLinkedList<Notice> implements Serializable {
    public Post(){
        super();
    }

    public int listAvailableLetters(GameUser user) {
        Node<Notice> current=getHead();
        int displayCounter = 0;
        GameLog.info("\n📬 نامه‌های شما:");
        while (current!=null){
            if((current.data.receiver==null || (current.data.receiver != null && current.data.receiver.getId()==user.getId())) &&
                    Duration.between(current.data.sendTime, LocalDateTime.now()).getSeconds() >= current.data.durationSeconds){
                GameLog.info(displayCounter + "- " + current.data);
                displayCounter++;
            }
            current=current.next;
        }
        if (displayCounter == 0) {
            GameLog.info("❗ شما نامه جدیدی ندارید.");
        }
        return displayCounter;
    }

    public  void getLetterByDisplayedIndex(int displayedIndex, GameUser user) {
        Node<Notice> current = getHead();
        int currentDisplayIndex = 0;
        while (current != null){
            if ((current.data.receiver == null || (current.data.receiver != null && current.data.receiver.getId() == user.getId())) &&
                    Duration.between(current.data.sendTime, LocalDateTime.now()).getSeconds() >= current.data.durationSeconds) {
                if (currentDisplayIndex == displayedIndex) {
                    GameLog.info("\n--- جزئیات نامه ---");
                    GameLog.info("شماره نامه: " + current.data.letterNumber);
                    GameLog.info("عنوان: " + current.data.Title);
                    GameLog.info("فرستنده: " + ((current.data.sender == null) ? "سیستم" : current.data.sender.getUsername()));
                    GameLog.info("گیرنده: " + ((current.data.receiver == null) ? "عمومی" : current.data.receiver.getUsername()));
                    GameLog.info("زمان ارسال: " + current.data.sendTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    GameLog.info("متن:\n" + current.data.note);
                    GameLog.info("----------------------");
                    return;
                }
                currentDisplayIndex++;
            }
            current=current.next;
        }
        GameLog.info("❌ نامه‌ای با این شماره برای شما یافت نشد یا هنوز در دسترس نیست.");
    }
}
