package com.yourname.got.logic.system;

import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.mediator.GameMediator; // << ۱. این import را اضافه کنید
// File: GameLoopManager.java
import com.yourname.got.logic.system.KingdomUpdater.UpdateResult;
import java.io.Serializable;

public class GameLoopManager extends Thread implements Serializable {
    private final GameUserList userList;
    private final GameMediator mediator;
    private final int tickMillis = 5000;

    public GameLoopManager(GameUserList users, GameMediator mediator) {
        this.userList = users;
        this.mediator = mediator;
    }

    @Override
    // In file: GameLoopManager.java


    public void run() {
        // یک بار در ابتدا بازی را بگیرید
        GameOfThrones game = null;
        if (!userList.isEmpty()) {
            game = userList.get(0).getGame();
        }

        if (game == null) {
            System.err.println("GameLoopManager: Game instance is null. Loop will not start.");
            return;
        }

        // حلقه را تا زمانی ادامه بده که بازی تمام نشده باشد

        while (!game.isFinished()) {
            GameUpdater.updateGlobalState(game);

            userList.forEach(user -> {
                synchronized (user) {
                    // >> START OF MODIFICATION <<
                    // Capture the result from the update process
                    UpdateResult result = GameUpdater.updateUser(user, this.mediator);

                    // If the result indicates a visual change, trigger a UI refresh via the mediator
                    if (result != null && result.visualsChanged) {
                        mediator.triggerUIRefresh(user.getId());
                    }
                    // >> END OF MODIFICATION <<
                }
            });

            game.getBazar().autoSeedGenericOffersIfDue(game.getGameTime());

            try {
                Thread.sleep(tickMillis);
            } catch (InterruptedException ignored) {
                break;
            }
        }
//        while (!game.isFinished()) { // << تغییر کلیدی
//            GameUpdater.updateGlobalState(game);
//
//            userList.forEach(user -> {
//                synchronized (user) {
//                    GameUpdater.updateUser(user, this.mediator);
//                }
//            });
//
//            game.getBazar().autoSeedGenericOffersIfDue(game.getGameTime());
//
//            try {
//                Thread.sleep(tickMillis);
//            } catch (InterruptedException ignored) {
//                // اگر ترد متوقف شد، حلقه را بشکن
//                break;
//            }
//        }

        System.out.println("Game loop has gracefully ended. Game is over.");
    }
}
