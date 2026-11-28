package com.yourname.got.logic.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Joining side of the LAN link. Connects, does the HELLO handshake, then delivers incoming frames to a listener. */
public final class LanClient {
    private Socket socket;
    private BufferedWriter out;
    private volatile boolean open;

    /** Returns null on success, otherwise a short reason. */
    public synchronized String connect(String host, int port, String name, String joinCode,
                                       Consumer<LanFrame> onFrame, Runnable onClosed) {
        close();
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(10000);
            s.setTcpNoDelay(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
            w.write(new LanFrame("HELLO", name + "\n" + (joinCode == null ? "" : joinCode)).encode());
            w.write('\n');
            w.flush();

            String line = in.readLine();
            LanFrame answer = LanFrame.decode(line);
            if (answer == null) { s.close(); return "No answer from host"; }
            if ("REJECTED".equals(answer.type)) { s.close(); return answer.body; }
            if (!"WELCOME".equals(answer.type)) { s.close(); return "Unexpected answer"; }

            s.setSoTimeout(0);
            socket = s;
            out = w;
            open = true;
            Thread t = new Thread(() -> readLoop(in, onFrame, onClosed), "lan-reader");
            t.setDaemon(true);
            t.start();
            return null;
        } catch (IOException e) {
            return "Could not connect: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    public boolean isOpen() { return open; }

    public synchronized boolean send(LanFrame f) {
        if (!open) return false;
        try {
            out.write(f.encode());
            out.write('\n');
            out.flush();
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    public synchronized void close() {
        open = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        socket = null;
        out = null;
    }

    private void readLoop(BufferedReader in, Consumer<LanFrame> onFrame, Runnable onClosed) {
        try {
            String line;
            while (open && (line = in.readLine()) != null) {
                LanFrame f = LanFrame.decode(line);
                if (f != null && onFrame != null) {
                    try { onFrame.accept(f); } catch (RuntimeException ignored) { }
                }
            }
        } catch (IOException ignored) {
        } finally {
            boolean wasOpen = open;
            close();
            if (wasOpen && onClosed != null) {
                try { onClosed.run(); } catch (RuntimeException ignored) { }
            }
        }
    }
}
