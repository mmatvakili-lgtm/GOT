package com.yourname.got.logic.util;

import com.yourname.got.logic.enums.KingdomType;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.enums.ResourceType;
import com.yourname.got.logic.model.Army.SoldierUnit;

import java.io.Serializable;
import java.time.LocalDateTime;
public class Transaction implements Serializable {
    private static final long serialVersionUID = 3L; // Version updated

    GameUser exchanger;
    public long sendTime;
    private int durationSeconds;

    private MyLinkedList<ResourceItem> resourcesToGive;
    private SoldierUnit soldiersToGive;

    private MyLinkedList<ResourceItem> resourcesToTake;
    private SoldierUnit soldiersToTake;

    private KingdomType onlyForKingdom;
    public static boolean DEBUG_PRINT_OBJECT_ID = false;

    public Transaction(GameUser user, int durationSeconds, MyLinkedList<ResourceItem> resourcesToGive, SoldierUnit soldiersToGive, MyLinkedList<ResourceItem> resourcesToTake, SoldierUnit soldiersToTake , KingdomType onlyForKingdom) {
        this.exchanger = user;
        this.sendTime = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
        this.resourcesToGive = (resourcesToGive != null) ? resourcesToGive : new MyLinkedList<>();
        this.soldiersToGive = soldiersToGive;
        this.resourcesToTake = (resourcesToTake != null) ? resourcesToTake : new MyLinkedList<>();
        this.soldiersToTake = soldiersToTake;
        this.onlyForKingdom = onlyForKingdom;
    }

    @Override
    public String toString() {
        String exchangerName = (exchanger == null) ? "سیستم" : exchanger.getUsername();
        StringBuilder giveStr = new StringBuilder();
        StringBuilder takeStr = new StringBuilder();

        String tag = (onlyForKingdom != null) ? (" | 🎯 فقط برای: " + onlyForKingdom.name()) : "";
        if (resourcesToGive != null && !resourcesToGive.isEmpty())
            for (ResourceItem item : resourcesToGive)
                giveStr.append(item.amount).append(" ").append(item.type.name()).append(", ");
        if (soldiersToGive != null && soldiersToGive.getCount() > 0)
            giveStr.append(soldiersToGive.toDisplayString(onlyForKingdom)).append(", ");

        if (resourcesToTake != null && !resourcesToTake.isEmpty())
            for (ResourceItem item : resourcesToTake)
                takeStr.append(item.amount).append(" ").append(item.type.name()).append(", ");
        if (soldiersToTake != null && soldiersToTake.getCount() > 0)
            takeStr.append(soldiersToTake.toDisplayString(onlyForKingdom)).append(", ");

        if (giveStr.length() > 2) giveStr.setLength(giveStr.length() - 2);
        if (takeStr.length() > 2) takeStr.setLength(takeStr.length() - 2);

        String durationInfo = " |  Arrival time: " + this.durationSeconds + " s";

        String out = "Seller: " + exchangerName
            + " | Supply: [" + giveStr + "]"
            + " | Demand: [" + takeStr + "]"
            + durationInfo
            + tag;

        if (DEBUG_PRINT_OBJECT_ID) {
            out += "  (dbg:" + super.toString() + ")";
        }
        return out;
    }

    public GameUser getExchanger() { return exchanger; }
    public KingdomType getOnlyForKingdom() {
        return onlyForKingdom;
    }
    public int getDurationSeconds() { return durationSeconds; }
    public MyLinkedList<ResourceItem> getResourcesToGive() { return resourcesToGive; }
    public SoldierUnit getSoldiersToGive() { return soldiersToGive; }
    public MyLinkedList<ResourceItem> getResourcesToTake() { return resourcesToTake; }
    public SoldierUnit getSoldiersToTake() { return soldiersToTake; }
}
