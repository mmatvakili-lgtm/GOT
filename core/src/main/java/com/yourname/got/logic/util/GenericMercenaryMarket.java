package com.yourname.got.logic.util;

import com.yourname.got.logic.enums.ResourceType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.mediator.GameMediator;

import java.io.Serializable;

public final class GenericMercenaryMarket implements Serializable {
    private GenericMercenaryMarket() {}

    private static final double GENERAL_MARKUP = 2.2;
    private static final int MAX_BATCH_INFANTRY = 30;
    private static final int MAX_BATCH_ARCHER = 25;
    private static final int MAX_BATCH_CAVALRY = 20;


    public static void ensureOffers(Bazar bazar) {
        addMercenaryOffer(bazar, SoldierType.INFANTRY, randomUpTo(MAX_BATCH_INFANTRY));
        addMercenaryOffer(bazar, SoldierType.ARCHER,   randomUpTo(MAX_BATCH_ARCHER));
        addMercenaryOffer(bazar, SoldierType.CAVALRY,  randomUpTo(MAX_BATCH_CAVALRY));
    }

    private static int randomUpTo(int max) {
        if (max <= 10) return max;
        return 10 + (int)(Math.random() * (max - 9));
    }

    private static void addMercenaryOffer(Bazar bazar, SoldierType baseType, int count) {
        if (count <= 0) return;

        SoldierUnit unitForSale = new SoldierUnit(baseType);
        for (int i = 0; i < count; i++) {
            Soldier s = switch (baseType) {
                case ARCHER -> new Archer();
                case INFANTRY -> new Infantry();
                case CAVALRY -> new Cavalry();
                default -> null;
            };
            if (s != null) {
                unitForSale.addSoldier(s);
            }
        }

        int food = (int)Math.ceil(baseType.foodCost * count * GENERAL_MARKUP);
        int wood = (int)Math.ceil(baseType.woodCost * count * GENERAL_MARKUP);
        int coin = (int)Math.ceil(baseType.coinCost * count * GENERAL_MARKUP);

        MyLinkedList<ResourceItem> toTake = new MyLinkedList<>();
        if (food > 0) toTake.add(new ResourceItem(ResourceType.Food, food));
        if (wood > 0) toTake.add(new ResourceItem(ResourceType.Wood, wood));
        if (coin > 0) toTake.add(new ResourceItem(ResourceType.Coin, coin));

        Transaction t = new Transaction(
                null, 0,
                null, unitForSale,
                toTake, null,
                null
        );
        bazar.add(t);
        GameMediator.sendGlobalMessage("A new general mercenary shipment has been added to the market");
        System.out.println("✅ A new general mercenary shipment has been added to the market");
    }
}
