package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;
import com.yourname.got.logic.enums.AttackType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.BotUser;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.util.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional bot offence (bots.attack.enabled=1, default off). Safety rules:
 *  - nothing happens before game day bots.attack.startDay (a grace period for new players);
 *  - a bot attacks at most once every bots.attack.days game days and never while its last march is still on;
 *  - it sends only bots.attack.strength percent of its infantry, archers and cavalry (the rest stays home);
 *  - it uses the normal rules (GameRulesManager.canAttack): seasons, defeated or busy players and peace treaties or
 *    alliances all apply, and only ground attacks on real players are made (never bot against bot);
 *  - the target is told at once; any error leaves the soldiers at home and the game goes on.
 */
public final class BotAttacker {
    private BotAttacker() {}

    public static boolean enabled() { return BalanceConfig.integer("bots.attack.enabled", 0, 0, 1) == 1; }
    public static int intervalDays() { return BalanceConfig.integer("bots.attack.days", 7, 1, 60); }
    public static int startDay() { return BalanceConfig.integer("bots.attack.startDay", 10, 0, 200); }
    public static int strengthPercent() { return BalanceConfig.integer("bots.attack.strength", 50, 10, 100); }

    private static final int MIN_SENT = 5;

    /** Called once per game day for every bot. Returns the target's name when an attack was launched, else null. */
    public static String tryAttack(GameOfThrones game, BotUser bot, int day) {
        if (!enabled() || game == null || bot == null || game.isFinished()) return null;
        if (day < startDay()) return null;
        if (bot.getLastAttackDay() != 0 && day - bot.getLastAttackDay() < intervalDays()) return null;
        Kingdom k = bot.getKingdom();
        if (k == null || k.getCastle() == null || k.getCastle().isDestroyed()) return null;
        for (Mission m : game.getMissions()) if (m != null && m.getAttacker() == bot) return null; // still marching

        List<GameUser> targets = new ArrayList<>();
        for (GameUser u : game.getUsers()) {
            if (u == null || BotUser.isBot(u)) continue;
            Result r = game.getRulesManager().canAttack(bot, u, AttackType.GROUND);
            if (r.isSuccess()) targets.add(u);
        }
        if (targets.isEmpty()) return null; // try again tomorrow (the day is not used up)
        GameUser target = targets.get((int) (Math.abs((long) day * 31 + bot.getId()) % targets.size()));

        SoldierType[] order = {SoldierType.INFANTRY, SoldierType.CAVALRY, SoldierType.ARCHER};
        List<SoldierUnit> packed = new ArrayList<>();
        int total = 0;
        try {
            for (SoldierType t : order) {
                SoldierUnit home = k.getArmyManager().getDefenceManager().findUnitInDefenders(t);
                int have = home == null ? 0 : home.getCount();
                int send = have * strengthPercent() / 100;
                if (send <= 0) continue;
                SoldierUnit unit = k.getArmyManager().removeAndPackageSoldiers(t, send);
                if (unit == null) continue;
                packed.add(unit);
                total += send;
            }
            if (total < MIN_SENT) {
                giveBack(k, packed);
                return null;
            }
            int distance = game.GroundDistance(bot, target);
            int seconds = game.getGameTime().getTimeForDistance(distance, k.getMarchSpeed());
            Mission mission = new Mission(bot, target, packed.toArray(new SoldierUnit[0]), seconds, AttackType.GROUND);
            game.getMissions().add(mission);
            bot.setLastAttackDay(day);
            game.sendSystemNotificationToUser(target, "Warning",
                bot.getUsername() + " is marching on you with " + total + " soldiers!");
            return target.getUsername();
        } catch (RuntimeException e) {
            giveBack(k, packed);
            target.setInFight(false);
            GameLog.error("Bot attack failed, soldiers stayed home: " + e);
            return null;
        }
    }

    private static void giveBack(Kingdom k, List<SoldierUnit> packed) {
        if (packed.isEmpty()) return;
        k.getArmyManager().returnSurvivorsToDefence(packed.toArray(new SoldierUnit[0]));
        k.getArmyManager().reconcileAllSoldiers();
    }
}
