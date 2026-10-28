package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.util.MyLinkedList;

public class KingdomSoldierRegistry {
    private static MyLinkedList<KingdomSoldierProfile> profiles = new MyLinkedList<>();

    static {
        profiles.add(new KingdomSoldierProfile(KingdomType.WINTERFELL, SoldierType.INFANTRY,
            "Northern Warrior", 200, 30));
        profiles.add(new KingdomSoldierProfile(KingdomType.WINTERFELL, SoldierType.ARCHER,
            "Northern Archer", 150, 40));
        profiles.add(new KingdomSoldierProfile(KingdomType.WINTERFELL, SoldierType.CAVALRY,
            "Snow Rider", 280, 50));
        profiles.add(new KingdomSoldierProfile(KingdomType.WINTERFELL, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.CASTERLY_ROCK, SoldierType.INFANTRY,
            "Golden Warrior", 220, 45));
        profiles.add(new KingdomSoldierProfile(KingdomType.CASTERLY_ROCK, SoldierType.ARCHER,
            "Golden Archer", 120, 55));
        profiles.add(new KingdomSoldierProfile(KingdomType.CASTERLY_ROCK, SoldierType.CAVALRY,
            "Lion Rider", 250, 60));
        profiles.add(new KingdomSoldierProfile(KingdomType.CASTERLY_ROCK, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.DRAGONSTONE, SoldierType.INFANTRY,
            "Unsullied", 280, 60));
        profiles.add(new KingdomSoldierProfile(KingdomType.DRAGONSTONE, SoldierType.ARCHER,
            "Unsullied Archer", 180, 70));
        profiles.add(new KingdomSoldierProfile(KingdomType.DRAGONSTONE, SoldierType.CAVALRY,
            "Dothraki Rider", 300, 75));
        profiles.add(new KingdomSoldierProfile(KingdomType.DRAGONSTONE, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.IRON_ISLANDS, SoldierType.INFANTRY,
            "Ironborn Warrior", 180, 35));
        profiles.add(new KingdomSoldierProfile(KingdomType.IRON_ISLANDS, SoldierType.ARCHER,
            "Ironborn Archer", 80, 45));
        profiles.add(new KingdomSoldierProfile(KingdomType.IRON_ISLANDS, SoldierType.CAVALRY,
            "Iron Fleet Raider", 220, 40));
        profiles.add(new KingdomSoldierProfile(KingdomType.IRON_ISLANDS, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.HIGHGARDEN, SoldierType.INFANTRY,
            "Forest Warrior", 160, 25));
        profiles.add(new KingdomSoldierProfile(KingdomType.HIGHGARDEN, SoldierType.ARCHER,
            "Forest Archer", 120, 30));
        profiles.add(new KingdomSoldierProfile(KingdomType.HIGHGARDEN, SoldierType.CAVALRY,
            "Forest Rider", 190, 35));
        profiles.add(new KingdomSoldierProfile(KingdomType.HIGHGARDEN, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.STORMS_END, SoldierType.INFANTRY,
            "Storm Soldier", 240, 35));
        profiles.add(new KingdomSoldierProfile(KingdomType.STORMS_END, SoldierType.ARCHER,
            "Storm Archer", 140, 45));
        profiles.add(new KingdomSoldierProfile(KingdomType.STORMS_END, SoldierType.CAVALRY,
            "Shadow Killer", 200, 80));
        profiles.add(new KingdomSoldierProfile(KingdomType.STORMS_END, SoldierType.SPY,
            "Spy", 10, 80));

        profiles.add(new KingdomSoldierProfile(KingdomType.EYRIE, SoldierType.INFANTRY,
            "Brave Warrior", 170, 30));
        profiles.add(new KingdomSoldierProfile(KingdomType.EYRIE, SoldierType.ARCHER,
            "Brave Archer", 120, 40));
        profiles.add(new KingdomSoldierProfile(KingdomType.EYRIE, SoldierType.CAVALRY,
            "Knight", 250, 55));
        profiles.add(new KingdomSoldierProfile(KingdomType.EYRIE, SoldierType.SPY,
            "Spy", 10, 80));

    }

    public static KingdomSoldierProfile getProfile(KingdomType kingdom, SoldierType base) {
        for (KingdomSoldierProfile profile : profiles) {
            if (profile.getKingdom() == kingdom && profile.getBaseType() == base) {
                return profile;
            }
        }
        return null;
    }

    public static String resolveDisplayName(KingdomType kingdom, SoldierType base) {
        KingdomSoldierProfile p = getProfile(kingdom, base);
        return (p != null) ? p.getDisplayName() : base.name();
    }

    public static String resolveDisplayNameOrBase(SoldierType base) {
        return base.name();
    }


    public static MyLinkedList<KingdomSoldierProfile> getAllProfiles() {
        return profiles;
    }
}
