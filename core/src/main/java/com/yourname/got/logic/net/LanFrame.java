package com.yourname.got.logic.net;

/** One message on the LAN wire: a type and a text body, sent as a single line (tabs/newlines/backslashes escaped). */
public final class LanFrame {
    public static final int MAX_LINE = 16 * 1024;

    public final String type;
    public final String body;

    public LanFrame(String type, String body) {
        this.type = type == null ? "" : type;
        this.body = body == null ? "" : body;
    }

    public String encode() {
        return escape(type) + "\t" + escape(body);
    }

    /** Returns null for a malformed line. */
    public static LanFrame decode(String line) {
        if (line == null) return null;
        int tab = line.indexOf('\t');
        if (tab <= 0) return null;
        return new LanFrame(unescape(line.substring(0, tab)), unescape(line.substring(tab + 1)));
    }

    static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '\t': b.append("\\t"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    static String unescape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't': b.append('\t'); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    default: b.append(n);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    @Override public String toString() { return type + ":" + body; }
}
