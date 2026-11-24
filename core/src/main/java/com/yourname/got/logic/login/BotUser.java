package com.yourname.got.logic.login;

import com.yourname.got.logic.model.Army.Archer;
import com.yourname.got.logic.model.Army.Cavalry;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Kingdom;

/**
 * A computer-controlled lord that fills an empty kingdom (see "bots.count" in balance.properties).
 *
 * A bot lives ONLY inside the game file. It is never written to users/ or to the user index, it cannot log in
 * (the name prefix is reserved, see UserSystem.register), it has no game history, and its id is always negative
 * so it can never collide with a registered user. Everything that saves or lists "real" users must skip bots:
 * use {@link #isBot(GameUser)}.
 */
public class BotUser extends GameUser {
    private static final long serialVersionUID = 4721905561820001L;

    /** Registered users may not start their name with this (checked case-insensitively). */
    public static final String NAME_PREFIX = "[BOT]";

    public BotUser(int negativeId) {
        super(NAME_PREFIX + " #" + Math.abs(negativeId), "", negativeId);
        if (negativeId >= 0) {
            throw new IllegalArgumentException("A bot id must be negative: " + negativeId);
        }
    }

    public static boolean isBot(GameUser user) {
        return user instanceof BotUser;
    }

    /** "[BOT] STORMS_END": readable in the map legend and battle history, and unique per kingdom. */
    @Override
    public String getUsername() {
        Kingdom k = getKingdom();
        return (k != null) ? NAME_PREFIX + " " + k.getType().name() : super.getUsername();
    }

    /** Gives the kingdom a home guard (50% infantry, 30% archers, 20% cavalry) so it is a real opponent. */
    public void equipGarrison(int count) {
        Kingdom k = getKingdom();
        if (k == null || count <= 0) return;
        int archers = count * 3 / 10;
        int cavalry = count * 2 / 10;
        int infantry = count - archers - cavalry;
        for (int i = 0; i < infantry; i++) k.getArmyManager().recruit(new Infantry());
        for (int i = 0; i < archers; i++) k.getArmyManager().recruit(new Archer());
        for (int i = 0; i < cavalry; i++) k.getArmyManager().recruit(new Cavalry());
    }
}
