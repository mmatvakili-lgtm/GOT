package com.yourname.got.logic.login;

import com.yourname.got.logic.model.GameRecord;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import java.io.*;

public class UserSystem {
    private static final String INDEX_FILE = "users_index.txt";
    private static final String USERS_DIR = "users";
    public static final String USER_DATA_PATH = USERS_DIR + File.separator;

    private int lastId = 0;

    public UserSystem() {
        File dir = new File(USERS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        initializeLastId();
    }

    public Result  register(String username, String password) {
        if (findUserInIndex(username) != null) {
            return new Result(false, "Username is already registered");
        }

        if (password.length() != 4) {
            return new Result(false, "Password must be exactly 4 digits");
        }

        int newId = lastId + 1;
        String folder = "user_" + newId;
        String folderPath = USERS_DIR + File.separator + folder;
        File userFolder = new File(folderPath);

        if (!userFolder.mkdirs()) {
            return new Result(false, "Could not create user directory");
        }
        String userFileName = "user_" + newId + ".ser";
        String userFilePath = folderPath + File.separator + userFileName;
        GameUser newUser = new GameUser(username, password, newId);

        if (!saveGameUser(newUser, userFilePath)) {
            return new Result(false, "Error saving user data");
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(INDEX_FILE, true))) {
            String indexPath = folder + File.separator + userFileName;
            pw.println(newId + "," + username + "," + password + "," + indexPath);
            lastId = newId;
        } catch (IOException e) {

            return new Result(false, "Error updating user index");
        }

        return new Result(true, "Registration was successful", newUser);

    }

    public Result login(String username, String password) {
        String[] entry = findUserInIndex(username);
        if (entry == null) {
            return new Result(false, "Username not found");
        }

        if (!entry[2].equals(password)) {
            return new Result(false, "Incorrect password");
        }

        String filepath = USERS_DIR + File.separator + entry[3];
        GameUser user = loadGameUser(filepath);

        if (user == null) {
            return new Result(false, "Could not load user profile");
        }

        return new Result(true, "Login was successful", user);
    }

    private String[] findUserInIndex(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(INDEX_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length == 4 && parts[1].equalsIgnoreCase(username)) {
                    return parts;
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private void initializeLastId() {
        try (BufferedReader br = new BufferedReader(new FileReader(INDEX_FILE))) {
            String line;
            int maxId = 0;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length >= 1) {
                    try {
                        maxId = Math.max(maxId, Integer.parseInt(parts[0]));
                    } catch (NumberFormatException e) {}
                }
            }
            this.lastId = maxId;
        } catch (IOException e) {
            this.lastId = 0;
        }
    }

    public boolean saveGameUser(GameUser user, String filepath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filepath))) {
            out.writeObject(user);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public GameUser loadGameUser(String filepath) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filepath))) {
            return (GameUser) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Result saveGameRecord(GameUser user, GameRecord record) {
        if (user == null || record == null) {
            return new Result(false, "User or record cannot be null.");
        }
        String gameNameSanitized = record.getGameName().replaceAll("[^a-zA-Z0-9.-]", "_");
        String recordFileName = "Record_" + gameNameSanitized + ".ser";
        String filePath = USERS_DIR + File.separator + "user_" + user.getId() + File.separator + recordFileName;

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(record);
            System.out.println("INFO: Game record for user '" + user.getUsername() + "' saved successfully.");
            return new Result(true, "Game record saved.");
        } catch (IOException e) {
            System.err.println("ERROR: Could not save game record for user '" + user.getUsername() + "': " + e.getMessage());
            return new Result(false, "Failed to save game record.");
        }
    }

    public static Result loadAllUserRecords(GameUser user) {
        if (user == null) {
            return new Result(false, "User cannot be null.");
        }
        MyLinkedList<GameRecord> records = new MyLinkedList<>();
        File userDir = new File(USERS_DIR + File.separator + "user_" + user.getId());

        if (userDir.exists() && userDir.isDirectory()) {
            File[] files = userDir.listFiles((dir, name) -> name.startsWith("Record_") && name.endsWith(".ser"));
            if (files != null) {
                for (File file : files) {
                    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                        Object obj = in.readObject();
                        if (obj instanceof GameRecord) {
                            records.add((GameRecord) obj);
                        }
                    } catch (EOFException eof) {
                        System.err.println("WARN: Record file " + file.getName() + " is empty or corrupted. Skipping.");
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("WARN: Could not load record file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        return new Result(true, "Records loaded successfully.", records);
    }

    public static Result loadGameRecord(GameUser user, String gameName) {
        if (user == null || gameName == null || gameName.trim().isEmpty()) {
            return new Result(false, "User or game name cannot be null or empty.");
        }
        String gameNameSanitized = gameName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String recordFileName = "Record_" + gameNameSanitized + ".ser";
        String filePath = USERS_DIR + File.separator + "user_" + user.getId() + File.separator + recordFileName;
        File recordFile = new File(filePath);

        if (recordFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(recordFile))) {
                GameRecord record = (GameRecord) in.readObject();
                return new Result(true, "Game record found.", record);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("ERROR: Failed to load record " + recordFile.getName() + ": " + e.getMessage());
                return new Result(false, "Error loading game record file.");
            }
        }
        return new Result(false, "Game record not found.");
    }

    public static Result updateUser(GameUser user) {
        if (user == null) {
            return new Result(false, "User to update cannot be null.");
        }
        String userFileName = "user_" + user.getId() + ".ser";
        String userFolderPath = USERS_DIR + File.separator + "user_" + user.getId();
        String userFilePath = userFolderPath + File.separator + userFileName;

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(userFilePath))) {
            out.writeObject(user);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to save user file for " + user.getUsername() + ": " + e.getMessage());
            return new Result(false, "Failed to save user file.");
        }

        File inputFile = new File(INDEX_FILE);
        File tempFile = new File(inputFile.getAbsolutePath() + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            boolean updated = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length == 4 && Integer.parseInt(parts[0]) == user.getId()) {
                    String indexPath = "user_" + user.getId() + File.separator + userFileName;
                    writer.write(user.getId() + "," + user.getUsername() + "," + user.getPassword() + "," + indexPath);
                    updated = true;
                } else {
                    writer.write(line);
                }
                writer.newLine();
            }
            if (!updated) {
                String indexPath = "user_" + user.getId() + File.separator + userFileName;
                writer.write(user.getId() + "," + user.getUsername() + "," + user.getPassword() + "," + indexPath);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to update index file for " + user.getUsername() + ": " + e.getMessage());
            tempFile.delete();
            return new Result(false, "Failed to update user index.");
        }

        if (!inputFile.delete()) {
            System.err.println("ERROR: Could not delete old index file.");
            return new Result(false, "Could not finalize user update (delete failed).");
        }
        if (!tempFile.renameTo(inputFile)) {
            System.err.println("ERROR: Could not rename temp index file.");
            return new Result(false, "Could not finalize user update (rename failed).");
        }

        return new Result(true, "User updated successfully.");
    }
}
