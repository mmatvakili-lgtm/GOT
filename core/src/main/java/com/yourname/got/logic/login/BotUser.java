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

    /** Last game day this bot reinforced its guard (never twice for the same day). */
    private int lastGrowthDay = 0;

    /** Soldiers added per game day (balance.properties: bots.growth.perDay, default 0 = bots never grow). */
    public static int growthPerDay() {
        return com.yourname.got.logic.Balance.BalanceConfig.integer("bots.growth.perDay", 0, 0, 50);
    }

    /** Largest guard a bot builds up to (bots.garrison.max, default 200). */
    public static int garrisonMax() {
        return com.yourname.got.logic.Balance.BalanceConfig.integer("bots.garrison.max", 200, 0, 2000);
    }

    /**
     * Once per game day the bot recruits growthPerDay soldiers, never beyond garrisonMax().
     * Returns how many were added. Days that were skipped are not made up for.
     */
    private int lastBuildDay = 0;

    /** Once per game day: one storage/mine if bots.build.enabled=1 (see BotBuilder). Returns what was started or null. */
    public String develop(int day) {
        if (day <= lastBuildDay) return null;
        lastBuildDay = day;
        if (!com.yourname.got.logic.system.BotBuilder.enabled()) return null;
        return com.yourname.got.logic.system.BotBuilder.buildOne(getGame(), this);
    }

    /** Last game day this bot sent an attack (0 = never). See BotAttacker. */
    private int lastAttackDay = 0;

    public int getLastAttackDay() { return lastAttackDay; }
    public void setLastAttackDay(int day) { this.lastAttackDay = day; }

    private int lastTrainDay = 0;
    private int trainRotation = 0;

    /** Soldiers a bot tries to train per game day through its Barrack (bots.train.perDay, default 0 = off). */
    public static int trainPerDay() {
        return com.yourname.got.logic.Balance.BalanceConfig.integer("bots.train.perDay", 0, 0, 50);
    }

    /**
     * Once per game day: queue trainPerDay soldiers (infantry, archer, cavalry in turn) in the Barrack, paying the
     * normal resource cost. Does nothing without a built Barrack, resources or camp space. Returns how many were queued.
     */
    public int train(int day) {
        if (day <= lastTrainDay) return 0;
        lastTrainDay = day;
        int wanted = trainPerDay();
        Kingdom k = getKingdom();
        if (wanted <= 0 || k == null || k.getFirstAvailableBarrack() == null || getGame() == null) return 0;
        com.yourname.got.logic.enums.SoldierType[] order = {
            com.yourname.got.logic.enums.SoldierType.INFANTRY,
            com.yourname.got.logic.enums.SoldierType.ARCHER,
            com.yourname.got.logic.enums.SoldierType.CAVALRY};
        com.yourname.got.logic.enums.SoldierType type = order[trainRotation++ % order.length];
        int before = k.getTrainingQueue().getSize();
        k.getArmyManager().trainSoldiers(type, wanted, k, getGame().getSpeed());
        return Math.max(0, k.getTrainingQueue().getSize() - before);
    }

    public int reinforce(int day) {
        Kingdom k = getKingdom();
        if (k == null || day <= lastGrowthDay) return 0;
        lastGrowthDay = day;
        if (k.getCastle() == null || k.getCastle().isDestroyed()) return 0;
        int room = garrisonMax() - k.getOwnedSoldierCount();
        int add = Math.min(growthPerDay(), room);
        if (add <= 0) return 0;
        equipGarrison(add);
        return add;
    }

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
