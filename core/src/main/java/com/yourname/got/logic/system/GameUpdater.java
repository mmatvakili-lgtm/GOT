// File: GameUpdater.java

package com.yourname.got.logic.system;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.mediator.GameMediator;
import com.yourname.got.logic.system.KingdomUpdater.UpdateResult; // <<-- ADD THIS IMPORT


import java.io.Serializable;

public class GameUpdater implements Serializable {

//    public static void updateUser(GameUser user, GameMediator mediator) {
//        if (user == null || user.getGame() == null || mediator == null) {
//            return;
//        }
//
//        GameOfThrones game = user.getGame();
//        try {
//            // آپدیت‌های روتین پادشاهی مثل قبل انجام می‌شود
//            MyLinkedList<String> kingdomMessages = KingdomUpdater.updateKingdom(user.getKingdom(), game.getSpeed());
//            for (String message : kingdomMessages) {
//                GameMediator.sendGlobalMessage(message);
//            }
//            game.getBazar().autoSeedSoldierOffersIfDue(user.getKingdom(), game.getGameTime());
//
//        } catch (Exception e) {
//            System.err.println("!!! An error occurred during GameUpdater.updateUser for user: " + user.getUsername());
//            e.printStackTrace();
//        }
//
//        // *** START OF MODIFICATION (راه حل جدید و مرکزی) ***
//        // پس از هر آپدیت، وضعیت قلعه کاربر را چک می‌کنیم
//        if (user.getKingdom() != null &&
//            user.getKingdom().getCastle() != null &&
//            user.getKingdom().getCastle().isDestroyed()) {
//
//            // متد هدفمند را با آیدی کاربر شکست‌خورده فراخوانی می‌کنیم تا پنل فقط برای او نمایش داده شود
//            mediator.triggerDefeatPanelDisplay(user.getId());
//        }
//        // *** END OF MODIFICATION ***

    // File: GameUpdater.java

    // >> CHANGE THIS METHOD'S SIGNATURE AND LOGIC <<
    public static UpdateResult updateUser(GameUser user, GameMediator mediator) {
        if (user == null || user.getGame() == null || mediator == null) {
            return new UpdateResult(new MyLinkedList<>(), false); // Return a default empty result
        }

        GameOfThrones game = user.getGame();
        UpdateResult kingdomResult = null;

        try {
            kingdomResult = KingdomUpdater.updateKingdom(user.getKingdom(), game.getSpeed());
            for (String message : kingdomResult.messages) {
                GameMediator.sendGlobalMessage(message);
            }
            game.getBazar().autoSeedSoldierOffersIfDue(user.getKingdom(), game.getGameTime());

        } catch (Exception e) {
            System.err.println("!!! An error occurred during GameUpdater.updateUser for user: " + user.getUsername());
            e.printStackTrace();
        }

        // *** START OF MODIFICATION (راه حل جدید و مرکزی) ***
        // ... (your existing defeat check logic remains unchanged)
        if (user.getKingdom() != null &&
            user.getKingdom().getCastle() != null &&
            user.getKingdom().getCastle().isDestroyed()) {
            mediator.triggerDefeatPanelDisplay(user.getId());
        }
        // *** END OF MODIFICATION ***

        // Pass the result up to the GameLoopManager
        return kingdomResult != null ? kingdomResult : new UpdateResult(new MyLinkedList<>(), false);
    }

    public static void updateGlobalState(GameOfThrones game) {
        // ... این متد بدون تغییر باقی می‌ماند
        if (game == null) return;
        try {
            game.getGameTime().updateTime();
            int currentDay = game.getGameTime().getDay();
            game.getWhiteWalkerManager().onDayAdvanced(currentDay);
            game.checkMissions();
            game.checkGameOver();
        } catch (Exception e) {
            System.err.println("!!! An error occurred during GLOBAL game update !!!");
            e.printStackTrace();
        }
    }
}
