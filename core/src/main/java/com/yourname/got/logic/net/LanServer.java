package com.yourname.got.logic.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small host-side LAN server. Players join with a name and a join code (HELLO handshake); after that every frame they
 * send goes to the {@link Handler}, whose reply (if any) goes straight back. The host can also broadcast to everyone.
 * Nothing here knows about the game: it only moves frames. Threads are daemons, so it never keeps the game open.
 */
public final class LanServer {

    public interface Handler {
        /** Called on the client's thread. Return a reply frame or null. Must not block for long. */
        LanFrame onFrame(String player, LanFrame frame);
        default void onJoin(String player) {}
        default void onLeave(String player) {}
    }

    private static final class Peer {
        final String name;
        final Socket socket;
        final BufferedWriter out;
        Peer(String name, Socket socket, BufferedWriter out) { this.name = name; this.socket = socket; this.out = out; }

        synchronized boolean send(LanFrame f) {
            try {
                out.write(f.encode());
                out.write('\n');
                out.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private final String joinCode;
    private final int maxPlayers;
    private final Handler handler;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private volatile boolean running;
    private ServerSocket server;

    public LanServer(String joinCode, int maxPlayers, Handler handler) {
        this.joinCode = joinCode == null ? "" : joinCode;
        this.maxPlayers = Math.max(1, maxPlayers);
        this.handler = handler;
    }

    /** Starts listening; port 0 picks a free one. Returns the real port. */
    public synchronized int start(int port) throws IOException {
        if (running) return server.getLocalPort();
        server = new ServerSocket(port);
        running = true;
        Thread t = new Thread(this::acceptLoop, "lan-accept");
        t.setDaemon(true);
        t.start();
        return server.getLocalPort();
    }

    public boolean isRunning() { return running; }

    public List<String> players() { return new ArrayList<>(peers.keySet()); }

    public void broadcast(LanFrame f) {
        for (Peer p : peers.values()) {
            if (!p.send(f)) drop(p);
        }
    }

    public boolean sendTo(String player, LanFrame f) {
        Peer p = peers.get(player);
        if (p == null) return false;
        if (p.send(f)) return true;
        drop(p);
        return false;
    }

    public synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        for (Peer p : new ArrayList<>(peers.values())) {
            try { p.socket.close(); } catch (IOException ignored) { }
        }
        peers.clear();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                Thread t = new Thread(() -> serve(s), "lan-client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) continue; // a failed accept must not kill the server
            }
        }
    }

    private void serve(Socket s) {
        Peer me = null;
        try {
            s.setSoTimeout(15000); // handshake must be quick
            s.setTcpNoDelay(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));

            LanFrame hello = readFrame(in);
            if (hello == null || !"HELLO".equals(hello.type)) { reject(out, "Expected HELLO"); return; }
            String[] parts = hello.body.split("\n", 2);
            String name = parts[0].trim();
            String code = parts.length > 1 ? parts[1] : "";
            if (name.isEmpty() || name.length() > 24) { reject(out, "Bad name"); return; }
            if (!joinCode.equals(code)) { reject(out, "Wrong join code"); return; }

            Peer candidate = new Peer(name, s, out);
            synchronized (this) {
                if (peers.size() >= maxPlayers) { reject(out, "Server is full"); return; }
                if (peers.putIfAbsent(name, candidate) != null) { reject(out, "Name already in use"); return; }
            }
            me = candidate;
            me.send(new LanFrame("WELCOME", name));
            s.setSoTimeout(0);
            safeJoin(name);

            while (running) {
                LanFrame f = readFrame(in);
                if (f == null) break;
                LanFrame reply;
                try {
                    reply = handler.onFrame(name, f);
                } catch (RuntimeException e) {
                    reply = new LanFrame("ERROR", "Server error");
                }
                if (reply != null && !me.send(reply)) break;
            }
        } catch (IOException ignored) {
            // client went away
        } finally {
            try { s.close(); } catch (IOException ignored) { }
            if (me != null) drop(me);
        }
    }

    private LanFrame readFrame(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return LanFrame.decode(sb.toString());
            if (sb.length() >= LanFrame.MAX_LINE) throw new IOException("line too long");
            sb.append((char) c);
        }
        return null;
    }

    private void reject(BufferedWriter out, String why) {
        try {
            out.write(new LanFrame("REJECTED", why).encode());
            out.write('\n');
            out.flush();
        } catch (IOException ignored) { }
    }

    private void drop(Peer p) {
        if (peers.remove(p.name, p)) {
            try { p.socket.close(); } catch (IOException ignored) { }
            try { handler.onLeave(p.name); } catch (RuntimeException ignored) { }
        }
    }

    private void safeJoin(String name) {
        try { handler.onJoin(name); } catch (RuntimeException ignored) { }
    }
}
