package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import java.util.List;

class LobbyPlayerCountTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    static String countOf(List<String> list, String name) {
        for (String l : list) { String[] p = l.split(","); if (p[0].equals(name)) return p[4]; }
        return "missing";
    }
    @Test
    void lobbyListShowsRealPlayerCount() throws Exception {
        TestWorld.reset(false);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2), u3 = new GameUser("carol", "1234", 3);
        System.out.println("create: " + GameBuilder.createNewGame("lobby1", "", u1).getMessage());
        List<String> list = GameBuilder.getGameList();
        check(countOf(list, "lobby1").equals("1"), "new lobby lists 1 player (" + countOf(list, "lobby1") + ")");
        GameBuilder.JoinGameResult r2 = GameBuilder.joinGame(1, list, "-1", u2);
        check(r2.isSuccess(), "bob joins");
        check(countOf(GameBuilder.getGameList(), "lobby1").equals("2"), "list now shows 2 players (" + countOf(GameBuilder.getGameList(), "lobby1") + ")");
        GameBuilder.JoinGameResult r3 = GameBuilder.joinGame(1, GameBuilder.getGameList(), "-1", u3);
        check(r3.isSuccess(), "carol joins");
        check(countOf(GameBuilder.getGameList(), "lobby1").equals("3"), "list now shows 3 players (" + countOf(GameBuilder.getGameList(), "lobby1") + ")");
        check(!GameBuilder.createNewGame("bad,name", "", u1).isSuccess(), "comma in game name rejected");
        Check.assertAll();
    }
}
