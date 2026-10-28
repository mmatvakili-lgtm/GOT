package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.SoldierType;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TrainingLogEntry implements Serializable {
    private SoldierType type;
    private long startMillis;
    private long endMillis;
    private boolean success;

    public TrainingLogEntry(SoldierType type, long startMillis, long endMillis, boolean success) {
        this.type = type;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.success = success;
    }

    @Override
    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "🪖 " + type +
                " | Start: " + fmt.format(new Date(startMillis)) +
                " | End: " + fmt.format(new Date(endMillis)) +
                " | Status: " + (success ? "✅ Deployed to Camp" : "❌ Failed (Camp Full)");
    }
}
