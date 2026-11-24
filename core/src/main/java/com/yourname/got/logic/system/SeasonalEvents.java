package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.enums.Seasons;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.mediator.GameMediator;

import java.util.Random;

/**
 * Random events that follow the seasons (balance.properties, OFF by default: events.enabled=0).
 *
 *   summer / fall : Bountiful Harvest - every kingdom gets food (a percent of its food storage capacity)
 *   winter        : Famine            - every kingdom loses a percent of the food it holds
 *   spring        : Trade Windfall    - every kingdom gets coin (a percent of its coin storage capacity)
 *
 * Fairness and safety rules:
 *  - one roll per game day, and never twice for the same day (the day is stored in the game, so saving, loading
 *    or several ticks within a day cannot repeat it); days that passed while nobody played are not rolled later;
 *  - the roll is deterministic for (game, day), so the outcome does not depend on who happens to be online;
 *  - every kingdom (players and bots) is affected by the same percentage, so nobody is favoured;
 *  - resources are only added into storage space that exists and only taken from what is stored.
 */
public final class SeasonalEvents {
    private SeasonalEvents() {}

    public static boolean enabled() {
        return BalanceConfig.integer("events.enabled", 0, 0, 1) == 1;
    }

    /** Chance (percent) that a given day has an event. */
    public static int chancePercent() {
        return BalanceConfig.integer("events.chance.percent", 15, 0, 100);
    }

    /** Harvest and windfall size / famine loss, in percent. */
    public static int effectPercent() {
        return BalanceConfig.integer("events.effect.percent", 10, 1, 50);
    }

    /** Called by the game loop whenever the day may have advanced. Returns the event title, or null if none. */
    public static String onDayAdvanced(GameOfThrones game, int day) {
        if (game == null || !enabled() || !game.isGameReady() || game.isFinished()) return null;
        if (day <= game.getLastEventDay()) return null; // already rolled for this day (or a later one)
        game.setLastEventDay(day);

        Random dice = new Random(game.getId() * 1_000_003L + day * 7919L);
        if (dice.nextInt(100) >= chancePercent()) return null;

        Seasons season = game.getGameTime().getSeason();
        if (season == null) return null;
        int pct = effectPercent();
        String title;
        String body;
        switch (season) {
            case SUMMER, FALL -> {
                title = "Bountiful Harvest";
                body = "The fields were generous. Every kingdom gains food (" + pct + "% of its food storage).";
                for (Kingdom k : kingdoms(game)) gain(k, "FOOD", pct);
            }
            case WINTER -> {
                title = "Famine";
                body = "A hard winter spoils the stores. Every kingdom loses " + pct + "% of its food.";
                for (Kingdom k : kingdoms(game)) lose(k, "FOOD", pct);
            }
            default -> { // SPRING
                title = "Trade Windfall";
                body = "Merchants arrive with good deals. Every kingdom gains coin (" + pct + "% of its coin storage).";
                for (Kingdom k : kingdoms(game)) gain(k, "COIN", pct);
            }
        }

        GameLog.info("EVENT (day " + day + "): " + title);
        try {
            GameUser[] users = game.getUsers();
            if (game.getPostOffice() != null && users != null) {
                for (GameUser u : users) {
                    if (u != null && !com.yourname.got.logic.login.BotUser.isBot(u)) {
                        game.sendSystemNotificationToUser(u, title, body);
                    }
                }
            }
            GameMediator.sendGlobalMessage(title + ": " + body);
        } catch (RuntimeException e) {
            GameLog.error("Could not announce the event: " + e);
        }
        return title;
    }

    private static Iterable<Kingdom> kingdoms(GameOfThrones game) {
        java.util.List<Kingdom> list = new java.util.ArrayList<>();
        GameUser[] users = game.getUsers();
        if (users != null) {
            for (GameUser u : users) if (u != null && u.getKingdom() != null) list.add(u.getKingdom());
        }
        return list;
    }

    private static void gain(Kingdom k, String type, int pct) {
        int amount = (int) ((long) k.getTotalCapacity(type) * pct / 100);
        if (amount > 0) k.addResources(type, amount);
    }

    private static void lose(Kingdom k, String type, int pct) {
        int amount = (int) ((long) k.getTotalResources(type) * pct / 100);
        if (amount > 0) k.spendResources(type, amount);
    }
}
