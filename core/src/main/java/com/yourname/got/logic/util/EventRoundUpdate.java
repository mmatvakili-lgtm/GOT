package com.yourname.got.logic.util;

public class EventRoundUpdate implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public long attackerTotalHealth;
    public long defenderTotalHealth;
    public String message;
    public long timestamp;

    public EventRoundUpdate(String msg, long atkHealth, long defHealth) {
        this.message = msg;
        this.attackerTotalHealth = atkHealth;
        this.defenderTotalHealth = defHealth;
        this.timestamp = System.currentTimeMillis();
    }
    @Override public long getTimestamp() { return timestamp; }
}
