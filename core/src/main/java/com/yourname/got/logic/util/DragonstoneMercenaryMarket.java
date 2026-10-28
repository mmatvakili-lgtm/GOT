// DragonstoneMercenaryMarket.java  (کلاس جدیدِ Utility)
package com.yourname.got.logic.util;

import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Kingdom;

import java.io.Serializable;

public final class DragonstoneMercenaryMarket implements Serializable {
    private DragonstoneMercenaryMarket() {}

    private static final double INFANTRY_MARKUP = 2.6;
    private static final double ARCHER_MARKUP   = 2.6;
    private static final double CAVALRY_MARKUP  = 2.75;

    private static final int MAX_BATCH_INFANTRY = 25;
    private static final int MAX_BATCH_ARCHER   = 20;
    private static final int MAX_BATCH_CAVALRY  = 15;

    private static final int MAX_ACTIVE_OFFERS  = 12;

    public static void ensureOffersForDragonstone(Kingdom dragonstone) {
        if (dragonstone.getType() != KingdomType.DRAGONSTONE) return;

        Bazar bazar = dragonstone.getOwner().getGame().getBazar();
        if (bazar.getNumberOfTransactions() >= MAX_ACTIVE_OFFERS) return;

        addMercenaryOffer(bazar, dragonstone, SoldierType.INFANTRY, randomUpTo(MAX_BATCH_INFANTRY), INFANTRY_MARKUP); // Unsullied
        addMercenaryOffer(bazar, dragonstone, SoldierType.ARCHER,   randomUpTo(MAX_BATCH_ARCHER),   ARCHER_MARKUP);   // Unsullied Archer
        addMercenaryOffer(bazar, dragonstone, SoldierType.CAVALRY,  randomUpTo(MAX_BATCH_CAVALRY),  CAVALRY_MARKUP);  // Dothraki Rider
    }

    private static int randomUpTo(int max) {
        int n = 5 + (int)(Math.random() * (max - 4));
        return Math.max(5, n);
    }

    private static void addMercenaryOffer(Bazar bazar, Kingdom buyerKingdom,
                                          SoldierType baseType, int count, double markup) {
        if (count <= 0) return;
        if (hasActiveOffer(bazar, KingdomType.DRAGONSTONE, baseType)) return;

        SoldierUnit unitForSale = new SoldierUnit(baseType);
        for (int i = 0; i < count; i++) {
            Soldier s = switch (baseType) {
                case ARCHER -> new Archer();
                case INFANTRY -> new Infantry();
                case CAVALRY -> new Cavalry();
                case SPY -> new Spy();
                default -> null;
            };
            if (s != null) {
                KingdomSoldierProfile p = KingdomSoldierRegistry.getProfile(KingdomType.DRAGONSTONE, baseType);
                if (p != null) {
                    s.setHealth(p.getCustomHealth());
                    s.setAttackPower(p.getCustomAttack());
                }
                unitForSale.addSoldier(s);
            }
        }

        int food = (int)Math.ceil(baseType.foodCost * count * markup);
        int wood = (int)Math.ceil(baseType.woodCost * count * markup);
        int coin = (int)Math.ceil(baseType.coinCost * count * markup);

        MyLinkedList<ResourceItem> toTake = new MyLinkedList<>();
        if (food > 0) toTake.add(new ResourceItem(ResourceType.Food, food));
        if (wood > 0) toTake.add(new ResourceItem(ResourceType.Wood, wood));
        if (coin > 0) toTake.add(new ResourceItem(ResourceType.Coin, coin));

        Transaction t = new Transaction(
                null, 0,
                null, unitForSale,
                toTake, null,
                KingdomType.DRAGONSTONE // محدودیت خریدار
        );
        bazar.add(t);
        System.out.println("✅ یک محموله سیستمی جدید به بازار اضافه شد.");
    }

    private static boolean hasActiveOffer(Bazar bazar, KingdomType onlyFor, SoldierType type) {
        for (Transaction t : bazar) {
            if (t.getOnlyForKingdom() == onlyFor &&
                    t.getSoldiersToGive() != null &&
                    t.getSoldiersToGive().getType() == type) {
                return true;
            }
        }
        return false;
    }
}
