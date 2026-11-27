package com.yourname.got.logic.system;

import com.yourname.got.logic.Balance.BalanceConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Peace treaties and alliances between two kingdoms (by player id). Off unless diplomacy.enabled=1.
 * While a pact is active neither side can attack the other. A peace treaty ends after diplomacy.peace.days game days;
 * an alliance lasts until one side cancels it. Either side may cancel at any time (armies already marching continue).
 * A proposal must be answered within {@link #PROPOSAL_DAYS} game days. Stored inside the game file.
 */
public class Diplomacy implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int PROPOSAL_DAYS = 3;

    public enum Type { PEACE, ALLIANCE }

    public static final class Pact implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int a, b;
        public final Type type;
        public final int startDay;
        /** Last day the pact is active; 0 = no end. */
        public final int endDay;
        Pact(int a, int b, Type type, int startDay, int endDay) {
            this.a = a; this.b = b; this.type = type; this.startDay = startDay; this.endDay = endDay;
        }
        boolean involves(int x, int y) { return (a == x && b == y) || (a == y && b == x); }
        boolean activeOn(int day) { return endDay == 0 || day <= endDay; }
    }

    public static final class Proposal implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int from, to;
        public final Type type;
        public final int day;
        Proposal(int from, int to, Type type, int day) { this.from = from; this.to = to; this.type = type; this.day = day; }
    }

    private final List<Pact> pacts = new ArrayList<>();
    private final List<Proposal> proposals = new ArrayList<>();

    public static boolean enabled() { return BalanceConfig.integer("diplomacy.enabled", 0, 0, 1) == 1; }
    public static int peaceDays() { return BalanceConfig.integer("diplomacy.peace.days", 10, 1, 60); }

    /** Drops expired pacts and unanswered proposals. */
    public void prune(int day) {
        pacts.removeIf(p -> !p.activeOn(day));
        proposals.removeIf(p -> day - p.day > PROPOSAL_DAYS);
    }

    public Pact pactBetween(int x, int y, int day) {
        for (Pact p : pacts) if (p.involves(x, y) && p.activeOn(day)) return p;
        return null;
    }

    public Proposal proposalBetween(int x, int y) {
        for (Proposal p : proposals) if ((p.from == x && p.to == y) || (p.from == y && p.to == x)) return p;
        return null;
    }

    public List<Pact> pactsOf(int id, int day) {
        List<Pact> out = new ArrayList<>();
        for (Pact p : pacts) if ((p.a == id || p.b == id) && p.activeOn(day)) out.add(p);
        return out;
    }

    public String propose(int from, int to, Type type, int day) {
        prune(day);
        if (from == to) return "You cannot make a pact with yourself.";
        Pact existing = pactBetween(from, to, day);
        if (existing != null && (existing.type == type || existing.type == Type.ALLIANCE))
            return "You already have this or a stronger pact with this player.";
        if (proposalBetween(from, to) != null) return "There is already a pending proposal between you two.";
        proposals.add(new Proposal(from, to, type, day));
        return null;
    }

    /** Accepts or refuses the proposal that {@code from} sent to {@code to}. Returns an error text or null. */
    public String respond(int from, int to, boolean accept, int day) {
        prune(day);
        Proposal found = null;
        for (Proposal p : proposals) if (p.from == from && p.to == to) found = p;
        if (found == null) return "This proposal is no longer available.";
        proposals.remove(found);
        if (accept) {
            pacts.removeIf(p -> p.involves(from, to)); // an alliance replaces a peace treaty
            int end = found.type == Type.PEACE ? day + peaceDays() : 0;
            pacts.add(new Pact(from, to, found.type, day, end));
        }
        return null;
    }

    public boolean cancel(int x, int y, int day) {
        boolean had = pactBetween(x, y, day) != null;
        pacts.removeIf(p -> p.involves(x, y));
        proposals.removeIf(p -> (p.from == x && p.to == y) || (p.from == y && p.to == x));
        return had;
    }

    public List<Proposal> incoming(int id, int day) {
        List<Proposal> out = new ArrayList<>();
        for (Proposal p : proposals) if (p.to == id && day - p.day <= PROPOSAL_DAYS) out.add(p);
        return out;
    }
}
