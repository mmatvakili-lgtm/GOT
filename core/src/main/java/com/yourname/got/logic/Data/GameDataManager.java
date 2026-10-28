package com.yourname.got.logic.Data;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;

public class GameDataManager {
    private static MyLinkedList<GameUser> users = new MyLinkedList<>();
    private static MyLinkedList<Kingdom> kingdoms = new MyLinkedList<>();

    // افزودن کاربر جدید
    public static void addUser(GameUser user) {
        users.add(user);
    }

    // افزودن پادشاهی جدید
    public static void addKingdom(Kingdom kingdom) {
        kingdoms.add(kingdom);
    }

    // دریافت کاربر بر اساس نام کاربری
    public static GameUser getUserByUsername(String username) {
        Node<GameUser> current = users.getHead();
        while (current != null) {
            if (current.data.getUsername().equals(username)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    public static Kingdom getKingdomByUsername(String username) {
        Node<Kingdom> current = kingdoms.getHead();
        while (current != null) {
            if (current.data.getOwner().getUsername().equals(username)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    public static MyLinkedList<GameUser> getAllUsers() {
        return users;
    }

    public static MyLinkedList<Kingdom> getAllKingdoms() {
        return kingdoms;
    }
}
