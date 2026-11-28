package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Army.Soldier;

import java.io.Serializable;

public class WhiteWalkerSoldier extends Soldier implements Serializable {
    private static final long serialVersionUID = 1L;

    /** What kind of creature of the Long Night this is (used for the reports and the battle text). */
    public enum Kind {
        WIGHT("Wight", "Wights"),
        WALKER("White Walker", "White Walkers"),
        GIANT("Ice Giant", "Ice Giants"),
        NIGHT_KING("Night King", "Night King");

        public final String singular, plural;
        Kind(String singular, String plural) { this.singular = singular; this.plural = plural; }
    }

    private int baseAttack;
    private int baseHealth;
    /** Null in old saves: those soldiers count as plain wights. */
    private Kind kind;

    public WhiteWalkerSoldier(int attack, int health) {
        this(attack, health, Kind.WIGHT);
    }

    public WhiteWalkerSoldier(int attack, int health, Kind kind) {
        super(SoldierType.WHITEWALKER);
        this.baseAttack = Math.max(1, attack);
        this.baseHealth = Math.max(1, health);
        this.kind = kind == null ? Kind.WIGHT : kind;
        try { setAttackPower(this.baseAttack); } catch (Throwable ignored) {}
        try { setHealth(this.baseHealth); } catch (Throwable ignored) {}
    }

    public Kind getKind() { return kind == null ? Kind.WIGHT : kind; }

    @Override
    public boolean isEquippedWithDragonGlass() {
        return false;
    }

    @Override
    public String toString() {
        return getKind().singular + "{atk=" + baseAttack + ", hp=" + baseHealth + '}';
    }
}
