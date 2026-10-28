package com.yourname.got.logic.login;

import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.util.BattleEvent;
import com.yourname.got.logic.util.MyLinkedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// ✅ کلاس نهایی برای نگه‌داری کامل اطلاعات ریپلای
public class ReplayData implements Serializable {
    private static final long serialVersionUID = 1L;

    public MyLinkedList<BattleEvent> events;   // همه رویدادهای نبرد
    public String attackerName;        // نام مهاجم
    public String defenderName;        // نام مدافع
    public SoldierUnit[] initialTroops;

    public ReplayData() {
         this.events = new MyLinkedList<>();
    }

    // --- getters/setters ---
    public MyLinkedList<BattleEvent> getEvents() {
        return events;
    }
    public void setEvents(MyLinkedList<BattleEvent> events) {
        this.events = events;
    }

    public String getAttackerName() {
        return attackerName;
    }
    public void setAttackerName(String attackerName) {
        this.attackerName = attackerName;
    }

    public String getDefenderName() {
        return defenderName;
    }
    public void setDefenderName(String defenderName) {
        this.defenderName = defenderName;
    }

    public SoldierUnit[] getInitialTroops() {
        return initialTroops;
    }
    public void setInitialTroops(SoldierUnit[] initialTroops) {
        this.initialTroops = initialTroops;
    }
}
