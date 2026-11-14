package com.yourname.got;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class HeroReviveTest {
    private static Hero newHero() {
        Hero h = new Hero("Test", KingdomType.WINTERFELL, "d", 1, 5, 5, false, 100, 100);
        h.unlock();
        return h;
    }

    @Test
    void fallenHeroIsUnavailableAndComesBack() throws Exception {
        Check.clear();
        Hero h = newHero();
        Check.that(h.isAvailable(), "alive hero is available");
        h.setHealth(0);
        Check.that(!h.isAvailable(), "dead hero is NOT available");
        Check.that(!h.tryRevive(1), "no instant revival");
        Field f = Hero.class.getDeclaredField("diedAtMillis");
        f.setAccessible(true);
        f.setLong(h, System.currentTimeMillis() - 11 * 60_000L);
        Check.that(h.tryRevive(1) && h.isAvailable() && h.getHealth() == 100, "revived with full health after 11 minutes");
        Hero h2 = newHero();
        h2.setHealth(0);
        f.setLong(h2, System.currentTimeMillis() - 4 * 60_000L);
        Check.that(h2.tryRevive(3), "faster at higher game speed");
        Check.assertAll();
    }
}
