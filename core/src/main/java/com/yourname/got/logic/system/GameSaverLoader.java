package com.yourname.got.logic.system;

import com.yourname.got.logic.login.GameUser;

import java.io.*;

public class GameSaverLoader implements Serializable {
    public static void save(GameUser user) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("saves/" + user.getUsername() + ".sav"))) {
            oos.writeObject(user);
        } catch (IOException e) {
            System.out.println("❌ Save failed for " + user.getUsername() + ": " + e.getMessage());
        }
    }

    public static GameUser load(String username) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("saves/" + username + ".sav"))) {
            return (GameUser) ois.readObject();
        } catch (Exception e) {
            System.out.println("❌ Load failed for " + username + ": " + e.getMessage());
            return null;
        }
    }
}
