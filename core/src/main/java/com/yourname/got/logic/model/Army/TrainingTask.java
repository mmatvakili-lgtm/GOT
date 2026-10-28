package com.yourname.got.logic.model.Army;

import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.Building.Camp;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.util.Result;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TrainingTask implements Serializable {
    private static final long serialVersionUID = 221921894215379934L;
    private SoldierType type;
    private long startMillis = 0;
    private long requiredMillis;
    private Kingdom kingdom;

    public TrainingTask(SoldierType type, Kingdom kingdom , long startMillis ,int gameSpeed) {
        this.type = type;
        this.startMillis = startMillis;
        double realMinutes = type.buildTimeMinutes / gameSpeed;
        this.requiredMillis = (long)(realMinutes * 60 * 1000);
        this.kingdom = kingdom;
        System.out.println(this);
    }


    public void start(){
        this.startMillis = System.currentTimeMillis();
    }
    public boolean isStarted(){
        return startMillis != 0;
    }

    public boolean isCompleted() {
        if (!isStarted())
            return false;
        return (System.currentTimeMillis() - startMillis) >= requiredMillis;
    }

    public long getRemainingMillis() {
        if (!isStarted()) return requiredMillis;
        long elapsed = System.currentTimeMillis() - startMillis;
        return Math.max(0, requiredMillis - elapsed);
    }


    public Result finishTraining() {
        Soldier s = switch (type) {
            case ARCHER -> new Archer();
            case INFANTRY -> new Infantry();
            case CAVALRY -> new Cavalry();
            case SPY -> new Spy();
            default -> null;
        };

        if (s != null) {
            KingdomType kType = kingdom.getType();
            KingdomSoldierProfile profile = KingdomSoldierRegistry.getProfile(kType, type);
            if (profile != null) {
                s.setHealth(profile.getCustomHealth());
                s.setAttackPower(profile.getCustomAttack());
            }
        }

        boolean success = false;
        for (Building b : kingdom.getBuildings()) {
            if (b instanceof Camp camp && camp.hasCapacity()) {
                success = camp.addTrainedSoldier(s);
                if (success) break;
            }
        }

        long endMillis = System.currentTimeMillis();
        TrainingLogEntry log = new TrainingLogEntry(type, startMillis, endMillis, success);
        kingdom.getTrainingLogs().add(log);

        String message;
        if (success) {
            message = "SUCCESS: " + type + " training completed and deployed.";
        } else {
            message = "ERROR: " + type + " training completed but all camps are full.";
        }

        GameFileUtility.saveFullGameState(kingdom.getOwner().getGame());

        return new Result(success, message);
    }

    public SoldierType getType() {
        return type;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getRequireMillies() {
        return requiredMillis;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    @Override
    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "TrainingTask{" +
            "type=" + type +
            ", startMillis=" + fmt.format(new Date(startMillis)) +
            //", requiredMillis=" + fmt.format(new Date(requiredMillis)) +
            ", endMillis=" + fmt.format(new Date(startMillis + requiredMillis)) +
            ", kingdom=" + kingdom.getType() +
            '}';
    }
}
