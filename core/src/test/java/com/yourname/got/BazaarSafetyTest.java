package com.yourname.got;

import com.yourname.got.logic.enums.ResourceType;
import com.yourname.got.logic.enums.SoldierType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Army.Infantry;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Storage;
import com.yourname.got.logic.util.MyLinkedList;
import com.yourname.got.logic.util.ResourceItem;
import com.yourname.got.logic.util.Result;
import com.yourname.got.logic.util.Transaction;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

/** Market rules: no negative offers, no goods lost to a full storage, soldiers asked as payment are really paid. */
class BazaarSafetyTest {
    private static void storage(Kingdom k, String type) {
        Storage st = new Storage(type, k);
        st.markAsBuilt();
        k.addStorage(st);
        k.getBuildings().add(st);
    }

    private static MyLinkedList<ResourceItem> items(ResourceType type, int amount) {
        MyLinkedList<ResourceItem> l = new MyLinkedList<>();
        l.add(new ResourceItem(type, amount));
        return l;
    }

    @Test
    void offersAreValidatedAndPaymentsAreReal() throws Exception {
        TestWorld.reset(true);
        GameUser seller = new GameUser("alice", "1234", 1), buyer = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("bazaar", 1, seller);
        g.addPlayer(buyer);
        g.startGame();
        Kingdom sk = seller.getKingdom(), bk = buyer.getKingdom();
        for (String t : new String[]{"Wood", "Coin"}) { storage(sk, t); storage(bk, t); }
        sk.addResources("Wood", 1000);
        bk.addResources("Coin", 1000);

        // --- validation
        Transaction negative = new Transaction(seller, 0, items(ResourceType.Wood, -100), null, items(ResourceType.Coin, 10), null, null);
        int woodBefore = sk.getTotalResources("Wood");
        Result r = g.getBazar().listOffer(negative);
        Check.that(!r.isSuccess(), "a negative amount is refused");
        Check.that(sk.getTotalResources("Wood") == woodBefore, "...and no wood was printed (" + sk.getTotalResources("Wood") + ")");
        Transaction empty = new Transaction(seller, 0, new MyLinkedList<ResourceItem>(), null, items(ResourceType.Coin, 10), null, null);
        Check.that(!g.getBazar().listOffer(empty).isSuccess(), "an offer that gives nothing is refused");
        Transaction valid = new Transaction(seller, 0, items(ResourceType.Wood, 100), null, items(ResourceType.Coin, 50), null, null);
        Check.that(g.getBazar().listOffer(valid).isSuccess() && sk.getTotalResources("Wood") == woodBefore - 100, "a normal offer still works");

        // --- the buyer's wood storage is full: the purchase must be refused, not paid and thrown away
        bk.addResources("Wood", bk.getTotalCapacity("Wood"));
        int coinBefore = bk.getTotalResources("Coin"), sizeBefore = g.getBazar().size();
        Result full = g.getBazar().buy(valid, buyer);
        Check.that(!full.isSuccess() && full.getMessage().contains("cannot hold"), "full storage: " + full.getMessage());
        Check.that(bk.getTotalResources("Coin") == coinBefore && g.getBazar().size() == sizeBefore, "nothing was paid and the offer is still listed");

        // --- soldiers asked as payment
        for (int i = 0; i < 2; i++) bk.getArmyManager().recruit(new Infantry());
        bk.detuctResources(bk.getTotalResources("Wood"), 0, 0, 0); // empty the wood storage again
        SoldierUnit price = new SoldierUnit(SoldierType.INFANTRY);
        for (int i = 0; i < 3; i++) price.addSoldier(new Infantry());
        Transaction barter = new Transaction(seller, 0, items(ResourceType.Wood, 100), null, new MyLinkedList<ResourceItem>(), price, null);
        g.getBazar().add(barter);
        Result poor = g.getBazar().buy(barter, buyer);
        Check.that(!poor.isSuccess(), "2 infantry cannot pay for an offer asking 3: " + poor.getMessage());
        for (int i = 0; i < 3; i++) bk.getArmyManager().recruit(new Infantry()); // now 5
        int sellerArmy = sk.getArmyManager().getTotalSoldier();
        Result paid = g.getBazar().buy(barter, buyer);
        Check.that(paid.isSuccess(), "5 infantry can pay: " + paid.getMessage());
        Check.that(bk.getArmyManager().getTotalSoldier() == 2, "the buyer paid 3 soldiers (" + bk.getArmyManager().getTotalSoldier() + " left)");
        Check.that(sk.getArmyManager().getTotalSoldier() == sellerArmy + 3, "the seller received them");
        Check.that(bk.getTotalResources("Wood") == 100, "the buyer got the wood");
        Check.assertAll();
    }
}
