package com.yourname.got.logic.login;

import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Result;
import java.io.*;

public final class Admin {

    private static final String informationPath = "info.txt";

    public static void checkInfoFile() {
        File file = new File(informationPath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("ERROR: Could not create info.txt: " + e.getMessage());
            }
        }
    }

    public static Result writeLetter(int day, String title, String body) {
        if (title == null || title.trim().isEmpty() || body == null || body.trim().isEmpty() || day < 1 || day > 100) {
            return new Result(false, "Invalid input. Day must be 1-100 and fields cannot be empty.");
        }

        MyLinkedList<String> blocks = readAllLetters();
        String newLetter = formatLetter(day, title, body);
        int insertIndex = 0;

        for (int i = 0; i < blocks.size(); i++) {
            int d = extractDay(blocks.get(i));
            if (d > 0 && d <= day) { // Only consider valid days
                insertIndex = i + 1;
            } else if (d > day) {
                break;
            }
        }

        MyLinkedList<String> updated = new MyLinkedList<>();
        for (int i = 0; i < insertIndex; i++) updated.add(blocks.get(i));
        updated.add(newLetter);
        for (int i = insertIndex; i < blocks.size(); i++) updated.add(blocks.get(i));

        return writeAllToFile(updated)
            ? new Result(true, "Notice added successfully.")
            : new Result(false, "Failed to write notices to file.");
    }

    public static MyLinkedList<String> listAllNoticeTitles() {
        MyLinkedList<String> blocks = readAllLetters();
        MyLinkedList<String> result = new MyLinkedList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i);
            int day = extractDay(block);
            String title = extractTitle(block);

            if (day > 0 && !"[No Title]".equals(title)) {
                result.add("Day " + day + ": " + title);
            }
        }
        return result;
    }

    public static Result deleteLetterByIndex(int index) {
        MyLinkedList<String> allBlocks = readAllLetters();
        MyLinkedList<String> displayBlocks = new MyLinkedList<>();

        // Re-create the same filtered list the UI sees
        for (int i = 0; i < allBlocks.size(); i++) {
            String block = allBlocks.get(i);
            int day = extractDay(block);
            String title = extractTitle(block);
            if (day > 0 && !"[No Title]".equals(title)) {
                displayBlocks.add(block);
            }
        }

        if (index < 0 || index >= displayBlocks.size()) {
            return new Result(false, "Invalid index provided.");
        }

        String blockToRemove = displayBlocks.get(index);
        allBlocks.remove(blockToRemove);

        return writeAllToFile(allBlocks)
            ? new Result(true, "Notice deleted successfully.")
            : new Result(false, "Failed to update notice file after deletion.");
    }

    public static MyLinkedList<String> readAllLetters() {
        MyLinkedList<String> blocks = new MyLinkedList<>();
        File file = new File(informationPath);
        if (!file.exists()) return blocks;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder currentBlock = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--- Day:")) {
                    if (currentBlock.length() > 0) {
                        blocks.add(currentBlock.toString());
                    }
                    currentBlock = new StringBuilder(line + "\n");
                } else {
                    currentBlock.append(line).append("\n");
                }
            }
            if (currentBlock.length() > 0) {
                blocks.add(currentBlock.toString());
            }
        } catch (IOException e) {
            System.err.println("ERROR reading info.txt: " + e.getMessage());
        }
        return blocks;
    }

    private static boolean writeAllToFile(MyLinkedList<String> list) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(informationPath, false))) { // false = Overwrite
            for (int i = 0; i < list.size(); i++) {
                writer.write(list.get(i));
            }
            return true;
        } catch (IOException e) {
            System.err.println("ERROR writing to info.txt: " + e.getMessage());
            return false;
        }
    }

    private static String formatLetter(int day, String title, String body) {
        return "--- Day: " + day + " ---\n" +
            "Title: " + title + "\n" +
            "Body:\n" + body + "\n---\n";
    }

    public static int extractDay(String block) {
        try {
            String firstLine = block.split("\r?\n")[0];
            if (firstLine.contains("Day:")) {
                return Integer.parseInt(firstLine.replaceAll("[^0-9]", ""));
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String extractTitle(String block) {
        try {
            String[] lines = block.split("\r?\n");
            for (String line : lines) {
                if (line.startsWith("Title:")) {
                    return line.substring("Title:".length()).trim();
                }
            }
        } catch (Exception e) {
            return "[No Title]";
        }
        return "[No Title]";
    }

    public static String extractBody(String block) {
        try {
            StringBuilder bodyContent = new StringBuilder();
            String[] lines = block.split("\r?\n");
            boolean inBody = false;
            for (String line : lines) {
                if (inBody) {
                    if (line.trim().equals("---")) {
                        break;
                    }
                    bodyContent.append(line).append("\n");
                }
                if (line.trim().equalsIgnoreCase("Body:")) {
                    inBody = true;
                }
            }
            return bodyContent.toString().trim();
        } catch (Exception e) {
            return "[No Body Content]";
        }
    }
}
