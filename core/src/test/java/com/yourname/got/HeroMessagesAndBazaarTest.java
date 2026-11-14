package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.system.*;
import com.yourname.got.logic.util.*;
import com.yourname.got.mediator.*;
import java.lang.reflect.Field;
import java.util.*;

class HeroMessagesAndBazaarTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    static void set(Object o, Class<?> c, String f, Object v) throws Exception { Field x = c.getDeclaredField(f); x.setAccessible(true); x.set(o, v); }

    static class Listener implements GameMessageListener {
        final boolean inGame; final List<String> got = new ArrayList<>();
        Listener(boolean inGame) { this.inGame = inGame; }
        public void onMessage(String m) { got.add(m); }
        public boolean acceptsGameEvents() { return inGame; }
    }

    @Test
    void heroUnlockDragonstoneMessagesAndBazaarExpiry() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2);
        GameOfThrones g = new GameOfThrones("t13", 1, u1); g.addPlayer(u2); g.startGame();

        System.out.println("=== hero unlock at castle level 5 (any code path) ===");
        GameUser nd = u1.getKingdom().getType() != KingdomType.DRAGONSTONE ? u1 : u2;
        Kingdom k = nd.getKingdom();
        check(!k.getHero().isUnlocked() && k.getHero().getStatusText().startsWith("Locked"), "hero starts locked: " + k.getHero().getStatusText());
        set(k.getCastle(), Building.class, "level", 5);       // castle reaches level 5 WITHOUT going through Castle.completeUpgrade
        KingdomUpdater.updateKingdom(k, 1);
        check(k.getHero().isUnlocked() && k.getHero().getStatusText().equals("Ready"), "tick unlocks the hero: " + k.getHero().getStatusText());

        System.out.println("=== Dragonstone: dragons are the spy ===");
        GameUser ds = u1.getKingdom().getType() == KingdomType.DRAGONSTONE ? u1 : (u2.getKingdom().getType() == KingdomType.DRAGONSTONE ? u2 : null);
        if (ds == null) { // kingdoms are random: force one
            Kingdom dk = new Kingdom(KingdomType.DRAGONSTONE, g.getMap(), 30, 17, 13, 13, 36, 17, 36, 29);
            dk.setOwner(u1); ds = u1; set(u1, GameUser.class, "kingdom", dk);
        }
        Kingdom dk = ds.getKingdom();
        check(dk.getHero().isUnlocked() && dk.hasSpy(), "Dragonstone: hero unlocked from the start and counts as spy");
        dk.getHero().setHealth(0);
        check(!dk.hasSpy(), "fallen dragons no longer count as spy");
        dk.getHero().restoreHealth();

        System.out.println("=== messages go only to the right player on the game screen ===");
        GameMediator med = new GameMediator(null);
        set(med, GameMediator.class, "currentUser", u1);
        Listener login = new Listener(false), play = new Listener(true);
        med.setMessageListener(login);
        GameMediator.sendMessageToUser(u1, "storage full");
        check(login.got.isEmpty(), "login/lobby screen gets no game events");
        med.setMessageListener(play);
        GameMediator.sendMessageToUser(u2, "bob's storage is full");
        check(play.got.isEmpty(), "another player's message is NOT shown to the current player");
        GameMediator.sendMessageToUser(u1, "my storage is full");
        check(play.got.size() == 1, "own message is shown on the game screen");
        med.setMessageListener(null);
        try { GameMediator.sendGlobalMessage("no listener"); check(true, "no NullPointerException without a listener"); }
        catch (Throwable t) { check(false, "NPE without listener: " + t); }

        System.out.println("=== bazaar: expiry is processed by the tick, private to the seller ===");
        Kingdom sk = u1.getKingdom();
        Storage st = new Storage("Wood", sk); st.markAsBuilt(); sk.addStorage(st); sk.getBuildings().add(st);
        MyLinkedList<ResourceItem> give = new MyLinkedList<>(); give.add(new ResourceItem(ResourceType.Wood, 500));
        MyLinkedList<ResourceItem> take = new MyLinkedList<>(); take.add(new ResourceItem(ResourceType.Coin, 100));
        Transaction t = new Transaction(u1, 0, give, null, take, null, null);
        t.sendTime = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;      // offered 7 days ago
        g.getBazar().add(t);
        Transaction sys = new Transaction(null, 0, new MyLinkedList<>(), null, new MyLinkedList<>(), null, null);
        sys.sendTime = t.sendTime; g.getBazar().add(sys);
        int before = g.getBazar().size(), wood0 = sk.getTotalResources("Wood");
        med.setMessageListener(play); play.got.clear();
        g.getBazar().checkBazar(1);
        check(g.getBazar().size() == before - 2, "both expired offers were removed (" + before + " -> " + g.getBazar().size() + ")");
        check(sk.getTotalResources("Wood") == wood0 + 500, "seller got the 500 wood back");
        check(play.got.size() == 1 && play.got.get(0).contains("expired"), "seller sees exactly one expiry message; system offer expired silently");
        Check.assertAll();
    }
}
