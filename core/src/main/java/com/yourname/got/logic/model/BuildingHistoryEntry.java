package com.yourname.got.logic.model;

import java.io.Serializable;

public class BuildingHistoryEntry implements Serializable {
    private final String action;
    private final String buildingName;
    private final int x, y;
    private final long timestamp;

    public BuildingHistoryEntry(String action, String buildingName, int x, int y) {
        this.action = action;
        this.buildingName = buildingName;
        this.x = x;
        this.y = y;
        this.timestamp = System.currentTimeMillis();
    }

    public String getFormattedEntry() {
        return String.format("📅 [%s] %s ساختمان '%s' در (%d, %d)",
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date(timestamp)),
                action, buildingName, x, y);
    }
}
