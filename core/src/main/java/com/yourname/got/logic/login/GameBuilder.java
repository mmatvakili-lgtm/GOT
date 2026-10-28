package com.yourname.got.logic.login;

import com.yourname.got.logic.model.GameOfThrones;
import java.util.LinkedList;
import java.util.List;

public class GameBuilder {

    public static List<String> getGameList() {
        LinkedList<String> games = GameFileUtility.searchGame("");
        GameFileUtility.checkGameActivity(games);
        return games;
    }

    public static CreateGameResult createNewGame(String gameName, String passInput, GameUser user) {
        if (gameName == null || gameName.trim().isEmpty())
            return new CreateGameResult(false, "Game name cannot be empty");

        int newPassword = -1;
        if (passInput != null && !passInput.trim().isEmpty()) {
            try {
                newPassword = Integer.parseInt(passInput.trim());
            } catch (NumberFormatException e) {
                return new CreateGameResult(false, "Password must be numeric or left blank");
            }
        }

        GameOfThrones newGame = new GameOfThrones(gameName.trim(), newPassword, user);
        try {
            GameFileUtility.saveGame(newGame);
            UserSystem.updateUser(user);
            return new CreateGameResult(true, "Game saved successfully");
        } catch (Exception e) {
            return new CreateGameResult(false, "Error saving game file: " + e.getMessage());
        }
    }

    public static JoinGameResult joinGame(int selectedIndex, List<String> games, String inputPassword, GameUser user) {
        if (selectedIndex < 1 || selectedIndex > games.size()) {
            return new JoinGameResult(false, "The selected number is invalid");
        }

        String selectedGameLine = games.get(selectedIndex - 1);
        String[] parts = selectedGameLine.split(",");
        if (parts.length < 3) {
            return new JoinGameResult(false, "Game information is incomplete");
        }

        String name = parts[0];
        String password = parts[1];
        int id = Integer.parseInt(parts[2]);

        if (!password.equals("-1") && !password.equals(inputPassword)) {
            return new JoinGameResult(false, "The password is incorrect");
        }

        try {
            GameOfThrones loadedGame = GameFileUtility.loadGame(name, id);
            if (loadedGame.addPlayer(user).isSuccess()) {
                UserSystem.updateUser(user);
                return new JoinGameResult(true, "Successful login to the game", loadedGame);
            } else {
                return new JoinGameResult(false, "Login failed. Game is complete");
            }
        } catch (Exception e) {
            return new JoinGameResult(false, "Error loading game: " + e.getMessage());
        }
    }

    public static class CreateGameResult {
        private final boolean success;
        private final String message;

        public CreateGameResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class JoinGameResult {
        private final boolean success;
        private final String message;
        private final GameOfThrones game;

        public JoinGameResult(boolean success, String message) {
            this(success, message, null);
        }

        public JoinGameResult(boolean success, String message, GameOfThrones game) {
            this.success = success;
            this.message = message;
            this.game = game;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public GameOfThrones getGame() { return game; }
    }
}
