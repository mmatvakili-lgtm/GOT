package com.yourname.got.logic.util;

public class EventTargetChange implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public String newTargetName;
    public long timestamp;

    public EventTargetChange(String newTargetName) {
        this.newTargetName = newTargetName;
        this.timestamp = System.currentTimeMillis();
    }
    @Override public long getTimestamp() { return timestamp; }
}
