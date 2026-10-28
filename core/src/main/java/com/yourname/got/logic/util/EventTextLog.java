package com.yourname.got.logic.util;

public class EventTextLog implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public String message;
    public long timestamp;

    public EventTextLog(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    @Override public long getTimestamp() { return timestamp; }
}
