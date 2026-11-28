package com.yourname.got;

import com.yourname.got.logic.net.LanClient;
import com.yourname.got.logic.net.LanFrame;
import com.yourname.got.logic.net.LanServer;
import com.yourname.got.support.Check;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;

/** The LAN link on localhost: handshake rules, request/reply, broadcast, leave notices. */
class LanLinkTest {
    @Test
    void frameRoundTrip() {
        LanFrame f = new LanFrame("SAY", "a\tb\nc\\d");
        LanFrame back = LanFrame.decode(f.encode());
        Check.that(back != null && back.type.equals("SAY") && back.body.equals("a\tb\nc\\d"), "tabs, newlines and backslashes survive");
        Check.that(LanFrame.decode("no tab here") == null, "malformed line is refused");
        Check.assertAll();
    }

    @Test
    void joinTalkAndLeave() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        LanServer server = new LanServer("secret", 2, new LanServer.Handler() {
            @Override public LanFrame onFrame(String player, LanFrame f) {
                return "PING".equals(f.type) ? new LanFrame("PONG", player + ":" + f.body) : null;
            }
            @Override public void onJoin(String player) { events.add("join " + player); }
            @Override public void onLeave(String player) { events.add("leave " + player); }
        });
        int port = server.start(0);
        try {
            LanClient bad = new LanClient();
            Check.that("Wrong join code".equals(bad.connect("127.0.0.1", port, "x", "nope", null, null)), "wrong code is rejected");

            BlockingQueue<LanFrame> got = new LinkedBlockingQueue<>();
            LanClient a = new LanClient();
            Check.that(a.connect("127.0.0.1", port, "Ann", "secret", got::add, null) == null, "first player joins");
            LanClient dup = new LanClient();
            Check.that("Name already in use".equals(dup.connect("127.0.0.1", port, "Ann", "secret", null, null)), "same name refused");

            Check.that(a.send(new LanFrame("PING", "hi")), "send works");
            LanFrame pong = got.poll(3, TimeUnit.SECONDS);
            Check.that(pong != null && pong.type.equals("PONG") && pong.body.equals("Ann:hi"), "host reply comes back");

            BlockingQueue<LanFrame> got2 = new LinkedBlockingQueue<>();
            LanClient b = new LanClient();
            Check.that(b.connect("127.0.0.1", port, "Bob", "secret", got2::add, null) == null, "second player joins");
            LanClient full = new LanClient();
            Check.that("Server is full".equals(full.connect("127.0.0.1", port, "Cy", "secret", null, null)), "full server refuses");

            server.broadcast(new LanFrame("DAY", "7"));
            LanFrame d1 = got.poll(3, TimeUnit.SECONDS), d2 = got2.poll(3, TimeUnit.SECONDS);
            Check.that(d1 != null && d1.body.equals("7") && d2 != null && d2.body.equals("7"), "broadcast reaches everyone");

            b.close();
            long end = System.currentTimeMillis() + 3000;
            while (!events.contains("leave Bob") && System.currentTimeMillis() < end) Thread.sleep(20);
            Check.that(events.contains("join Ann") && events.contains("join Bob") && events.contains("leave Bob"), "join/leave are reported");
            Check.that(server.players().size() == 1, "leaver is removed");
            a.close();
        } finally {
            server.stop();
        }
        Check.assertAll();
    }
}
