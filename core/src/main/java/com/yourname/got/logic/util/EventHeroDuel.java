package com.yourname.got.logic.util;

public class EventHeroDuel implements BattleEvent {
    private static final long serialVersionUID = 1L;
    public String attackerHero;
    public String defenderHero;
    public boolean attackerWon;
    public long timestamp;

    public EventHeroDuel(String atkHero, String defHero, boolean atkWon) {
        this.attackerHero = atkHero;
        this.defenderHero = defHero;
        this.attackerWon = atkWon;
        this.timestamp = System.currentTimeMillis();
    }
    @Override public long getTimestamp() { return timestamp; }
}
