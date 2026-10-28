package com.yourname.got.logic.util;

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
        System.out.println("\n📬 نامه‌های شما:");
        while (current!=null){
            if((current.data.receiver==null || (current.data.receiver != null && current.data.receiver.getId()==user.getId())) &&
                    Duration.between(current.data.sendTime, LocalDateTime.now()).getSeconds() >= current.data.durationSeconds){
                System.out.println(displayCounter + "- " + current.data);
                displayCounter++;
            }
            current=current.next;
        }
        if (displayCounter == 0) {
            System.out.println("❗ شما نامه جدیدی ندارید.");
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
                    System.out.println("\n--- جزئیات نامه ---");
                    System.out.println("شماره نامه: " + current.data.letterNumber);
                    System.out.println("عنوان: " + current.data.Title);
                    System.out.println("فرستنده: " + ((current.data.sender == null) ? "سیستم" : current.data.sender.getUsername()));
                    System.out.println("گیرنده: " + ((current.data.receiver == null) ? "عمومی" : current.data.receiver.getUsername()));
                    System.out.println("زمان ارسال: " + current.data.sendTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    System.out.println("متن:\n" + current.data.note);
                    System.out.println("----------------------");
                    return;
                }
                currentDisplayIndex++;
            }
            current=current.next;
        }
        System.out.println("❌ نامه‌ای با این شماره برای شما یافت نشد یا هنوز در دسترس نیست.");
    }
}
