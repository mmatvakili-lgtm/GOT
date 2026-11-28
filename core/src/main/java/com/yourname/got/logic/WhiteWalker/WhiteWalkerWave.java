package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.Soldier;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.Node;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * A wave of the Long Night. Its size is measured against the strongest living kingdom, so it stays a real threat
 * however big the armies grow (before, a fixed small number of weak soldiers was beaten by everyone):
 *  - ordinary waves grow from about 12% to 40% of the strongest kingdom's attack power over the game;
 *  - the final wave is 55% (easy), 75% (medium) or 100% (hard) of it, and brings Ice Giants and the Night King;
 *  - the dead are many and tough: wights are numerous, walkers hit hard, giants and the Night King soak up damage.
 * Every kingdom meets the same army, so only well-prepared kingdoms (big army, Dragonglass, hero, research) survive.
 */
public class WhiteWalkerWave implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Attack, health and share of the wave's attack power for each kind. */
    private static final int[][] STATS = {
        // atk, hp
        {6, 24},      // wight
        {26, 104},    // white walker
        {70, 280},    // ice giant
        {160, 1600},  // night king
    };

    private final GameOfThrones gameOfThrones;
    private final int day;
    private final int attackInterval;
    private final boolean isFinal;

    public WhiteWalkerWave(GameOfThrones game, int day, int attackInterval, boolean isFinal) {
        this.day = day;
        this.attackInterval = Math.max(1, attackInterval);
        this.isFinal = isFinal;
        this.gameOfThrones = game;
    }

    /** Attack power of the strongest living kingdom's defenders (never below a small floor, so early waves exist). */
    public static long strongestKingdomPower(GameOfThrones game) {
        long best = 0;
        try {
            GameUser[] users = game.getActiveUsers();
            if (users != null) {
                for (GameUser u : users) {
                    if (u == null || u.getKingdom() == null) continue;
                    long p = 0;
                    MyLinkedList<SoldierUnit> defenders = u.getKingdom().getArmyManager().getDefenceManager().getDefenders();
                    if (defenders == null) continue;
                    Node<SoldierUnit> cur = defenders.getHead();
                    while (cur != null) {
                        if (cur.data != null) p += cur.data.getTotalAttackPower();
                        cur = cur.next;
                    }
                    best = Math.max(best, p);
                }
            }
        } catch (RuntimeException ignored) {
            // fall back to the floor below
        }
        return best;
    }

    /** Share of the strongest kingdom's power this wave brings. */
    public double strengthFraction(int startDay, int finalDay) {
        if (isFinal) {
            switch (difficulty()) {
                case "easy": return 0.55;
                case "hard": return 1.00;
                default: return 0.75;
            }
        }
        double span = Math.max(1, finalDay - startDay);
        double progress = Math.max(0, Math.min(1, (day - startDay) / span));
        return 0.12 + 0.28 * progress;
    }

    private String difficulty() {
        try {
            String d = gameOfThrones.getDifficulty();
            return d == null ? "medium" : d.toLowerCase();
        } catch (RuntimeException e) {
            return "medium";
        }
    }

    public SoldierUnit[] generateArmy() {
        int start = 10, fin = 100;
        try {
            start = gameOfThrones.getWhitewalkerStartDay();
            fin = gameOfThrones.getWhitewalkerFinalDay();
        } catch (RuntimeException ignored) {}
        long reference = Math.max(strongestKingdomPower(gameOfThrones), 3000L + 60L * Math.max(0, day));
        long target = Math.round(reference * strengthFraction(start, fin));
        return build(target, isFinal, progressOf(start, fin));
    }

    private double progressOf(int start, int fin) {
        return Math.max(0, Math.min(1, (day - start) / (double) Math.max(1, fin - start)));
    }

    /** Builds an army with roughly {@code power} total attack power. Pure and deterministic (used by the tests). */
    public static SoldierUnit[] build(long power, boolean finalWave, double progress) {
        double wightShare = 0.50, walkerShare = 0.32, giantShare = 0.18, kingShare = 0.0;
        if (progress < 0.5 && !finalWave) { wightShare = 0.62; walkerShare = 0.38; giantShare = 0.0; }
        if (finalWave) { wightShare = 0.40; walkerShare = 0.28; giantShare = 0.20; kingShare = 0.12; }

        SoldierUnit unit = new SoldierUnit(SoldierType.WHITEWALKER);
        add(unit, WhiteWalkerSoldier.Kind.WIGHT, STATS[0], power * wightShare, 600);
        add(unit, WhiteWalkerSoldier.Kind.WALKER, STATS[1], power * walkerShare, 250);
        if (giantShare > 0) add(unit, WhiteWalkerSoldier.Kind.GIANT, STATS[2], power * giantShare, 80);
        if (kingShare > 0) add(unit, WhiteWalkerSoldier.Kind.NIGHT_KING, STATS[3], power * kingShare, 1);
        return new SoldierUnit[]{unit};
    }

    private static void add(SoldierUnit unit, WhiteWalkerSoldier.Kind kind, int[] st, double powerShare, int cap) {
        int n = (int) Math.max(1, Math.min(cap, Math.round(powerShare / st[0])));
        for (int i = 0; i < n; i++) unit.addSoldier((Soldier) new WhiteWalkerSoldier(st[0], st[1], kind));
    }

    /** Counts of each kind in an army, in the order wight, walker, giant, king. */
    public static Map<WhiteWalkerSoldier.Kind, Integer> census(SoldierUnit[] army) {
        Map<WhiteWalkerSoldier.Kind, Integer> out = new EnumMap<>(WhiteWalkerSoldier.Kind.class);
        for (WhiteWalkerSoldier.Kind k : WhiteWalkerSoldier.Kind.values()) out.put(k, 0);
        if (army == null) return out;
        for (SoldierUnit u : army) {
            if (u == null) continue;
            Node<Soldier> cur = u.getSoldiers().getHead();
            while (cur != null) {
                if (cur.data instanceof WhiteWalkerSoldier w) out.merge(w.getKind(), 1, Integer::sum);
                cur = cur.next;
            }
        }
        return out;
    }

    /** "312 Wights, 40 White Walkers, 6 Ice Giants and the Night King". */
    public static String describe(Map<WhiteWalkerSoldier.Kind, Integer> census) {
        StringBuilder sb = new StringBuilder();
        for (WhiteWalkerSoldier.Kind k : WhiteWalkerSoldier.Kind.values()) {
            int n = census.getOrDefault(k, 0);
            if (n <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(k == WhiteWalkerSoldier.Kind.NIGHT_KING ? "the Night King" : n + " " + (n == 1 ? k.singular : k.plural));
        }
        return sb.length() == 0 ? "nothing" : sb.toString();
    }
}
