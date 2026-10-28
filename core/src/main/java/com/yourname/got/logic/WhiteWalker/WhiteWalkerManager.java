package com.yourname.got.logic.WhiteWalker;

import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Map;
import com.yourname.got.logic.model.Position;
import com.yourname.got.logic.model.UserSubMap;
import com.yourname.got.logic.util.Notice;



import java.io.Serializable;
import java.util.Random;

public class WhiteWalkerManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GameOfThrones game;
    private final Map map;

    private final Random rng = new Random();

    private final int startDay;
    private final int finalDay;
    private final int attackInterval;
    private final int baseWallDamage;

    private WhiteWalkerAttackStrategy strategy = WhiteWalkerAttackStrategy.ALL_AT_ONCE;

    private int lastProcessedDay = 0;
    private boolean finalWaveTriggered = false;
    private int lastAttackDay = Integer.MIN_VALUE;

    public WhiteWalkerManager(GameOfThrones game) {
        this.game = game;
        Map gm = null;
        try { gm = game.getMap(); } catch (Throwable t) { /* ignore */ }
        this.map = gm;

        int iv = 10;
        int st = 50;
        int fd = 100;
        int base = 300;
        try { iv = Math.max(1, game.getWhitewalkerAttackInterval()); } catch (Throwable ignored) {}
        try { st = Math.max(0, game.getWhitewalkerStartDay()); } catch (Throwable ignored) {}
        try { fd = Math.max(1, game.getWhitewalkerFinalDay()); } catch (Throwable ignored) {}

        this.attackInterval = iv;
        this.startDay = Math.max(1, st);
        this.finalDay = Math.max(this.startDay + 1, fd);
        this.baseWallDamage = Math.max(1, base);


        try {  game.getWall(); } catch (Throwable ignored) {
        }

        System.out.println("✅ WhiteWalkerManager initialized: startDay=" + this.startDay +
            " finalDay=" + this.finalDay + " interval=" + this.attackInterval +
            " baseWallDamage=" + this.baseWallDamage);
    }

    public boolean isFinalWaveTriggered() {
        return finalWaveTriggered;
    }

    public GameOfThrones getGame() {
        return game;
    }

    public int getAttackInterval() {
        return attackInterval;
    }
    public int getStartDay() {
        return startDay;
    }
    public int getFinalDay() {
        return finalDay;
    }
    public int getBaseWallDamage() {
        return baseWallDamage;
    }

    public WhiteWalkerAttackStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(WhiteWalkerAttackStrategy s) { if (s != null) strategy = s; }


    public synchronized void onDayAdvanced(int currentDay) {
        if (currentDay <= 0) return;
        if (currentDay <= lastProcessedDay) {
            return;
        }
        for (int day = lastProcessedDay + 1; day <= currentDay; day++) {
            processDay(day);
        }
        lastProcessedDay = currentDay;
    }

    private void processDay(int day) {
        try {
            if (!game.getWall().isDestroyed() && day % 2 == 0) {
                game.broadcastWallHelpNotice();
            }
        } catch (Throwable ignored) {}

        if (day < startDay && !finalWaveTriggered) return;

        if (!finalWaveTriggered && day >= finalDay) {
            finalWaveTriggered = true;
            System.out.println("💥 [WWManager] Day " + finalDay + " -> FINAL WAVE. Forcing wall collapse.");
            try { game.getWall().forceCollapseOnFinalDay(finalDay); } catch (Throwable ignored) {}
            attackAllKingdoms(finalDay, true);
            lastAttackDay = finalDay;
            return;
        }

        if (finalWaveTriggered && day > finalDay) {
            return;
        }

        if (day >= startDay && ((day - startDay) % attackInterval == 0) && day != lastAttackDay) {
            System.out.println("\n❄️ [WWManager] Day " + day + " -> Periodic wave.");
            int waveIndex = Math.max(0, (day - startDay) / Math.max(1, attackInterval));
            String difficulty = game.getDifficulty();
            int difficultyMultiplier;

            switch (difficulty.toLowerCase()) {
                case "easy" -> difficultyMultiplier = 1;
                case "medium" -> difficultyMultiplier = 2;
                case "hard" -> difficultyMultiplier = 3;
                default -> difficultyMultiplier = 1; // fallback
            }
            int damage = baseWallDamage * difficultyMultiplier + waveIndex * 100 ;

            try {
                if (!game.getWall().isDestroyed()) {
                    game.getWall().takeDamage(damage, day);
                    System.out.println("🧱 Wall damaged by " + damage + " -> health=" + game.getWall().getHealth());
                    if (game.getWall().isDestroyed()) {
                        System.out.println("⚠️ Wall collapsed on day " + day + ". Now attacking kingdoms.");
                        attackAfterWall(day);
                    }
                } else {
                    attackAfterWall(day);
                }
            } catch (Throwable t) {
                System.out.println("⚠️ Error during periodic WW attack: " + t.getMessage());
            }
            lastAttackDay = day;
        }
    }


    private void attackAfterWall(int day) {
        if (day < (startDay + (finalDay - startDay) / 2.0)) {
            this.strategy = WhiteWalkerAttackStrategy.NEAREST;
            System.out.println("...استراتژی وایت‌واکرها: حمله به نزدیک‌ترین‌ها (NEAREST)...");
        }
        else {
            this.strategy = WhiteWalkerAttackStrategy.RANDOM;
            System.out.println("...استراتژی وایت‌واکرها: حمله تصادفی و پراکنده (RANDOM)...");
        }

        switch (strategy) {
            case RANDOM:
                attackRandomKingdoms(day);
                break;
            case NEAREST:
                attackNearestKingdoms(day);
                break;
            case ALL_AT_ONCE:
                attackAllKingdoms(day, false);
            default:
                attackAllKingdoms(day, false);
                break;
        }
    }

    private void attackAllKingdoms(int day, boolean isFinalWave) {
        String message = isFinalWave ? "The Final Wave is upon you! Prepare for the Long Night!" : "The White Walkers have breached the Wall and attack all fronts!";
        System.out.println("...Strategy: ATTACK ALL KINGDOMS...");

        SoldierUnit[] template = new WhiteWalkerWave(game, day, attackInterval, isFinalWave).generateArmy();

        GameUser[] targets = getActivePlayerPool();
        if (targets.length == 0) {
            System.out.println("...No kingdoms left to attack.");
            return;
        }

        System.out.println("-> Launching a full-scale invasion against " + targets.length + " kingdoms.");

        for (GameUser target : targets) {
            launchWhiteWalkerMission(target, template, message);
        }
    }

    private void attackRandomKingdoms(int day) {
        SoldierUnit[] template = new WhiteWalkerWave(game, day, attackInterval, false).generateArmy();

        GameUser[] pool = getActivePlayerPool();
        if (pool.length == 0) {
            System.out.println("...No valid targets found for a random attack.");
            return;
        }

        int totalTargets = Math.max(1, pool.length / 2);
        System.out.println("-> Random targets selected. Count: " + totalTargets);

        for (int i = 0; i < totalTargets; i++) {
            int swapIndex = i + rng.nextInt(pool.length - i);
            GameUser temp = pool[i];
            pool[i] = pool[swapIndex];
            pool[swapIndex] = temp;
            GameUser target = pool[i];
            launchWhiteWalkerMission(target, template, "You have been targeted at random!");
        }
    }

    private void attackNearestKingdoms(int day) {
        SoldierUnit[] template = new WhiteWalkerWave(game, day, attackInterval, false).generateArmy();
        GameUser[] pool = getActivePlayerPool();
        if (pool.length == 0) return;

        Position wallPos = estimateWallPosition();

        for (int i = 0; i < pool.length - 1; i++) {
            int best = i;
            int bestDist = estimateDistanceToWall(pool[i], wallPos);
            for (int j = i + 1; j < pool.length; j++) {
                int d = estimateDistanceToWall(pool[j], wallPos);
                if (d < bestDist) { bestDist = d; best = j; }
            }
            if (best != i) {
                GameUser t = pool[i]; pool[i] = pool[best]; pool[best] = t;
            }
        }

        int toAttack = Math.max(1, pool.length / 2);
        System.out.println("-> اهداف نزدیک مشخص شدند. تعداد: " + toAttack);
        for (int i = 0; i < toAttack; i++) {
            launchWhiteWalkerMission(pool[i], template, "خطر! از لحاظ فاصله هدف گرفته شده‌اید.");
        }
    }

    private Position estimateWallPosition() {
        try {
            if (map != null && map.mainMap != null && map.mainMap[0] != null) {
                int cols = map.mainMap[0].length;
                return new Position(0, cols / 2);
            }
        } catch (Throwable ignored) {}
        return new Position(0, 0);
    }


    private Position estimateKingdomCenter(GameUser u) {
        if (u == null || u.getKingdom() == null || u.getKingdom().getUserMap() == null) {
            return new Position(0, 0);
        }
        UserSubMap subMap = u.getKingdom().getUserMap();
        int centerX = subMap.getStartX() + (subMap.getWidth() / 2);
        int centerY = subMap.getStartY() + (subMap.getLength() / 2);
        return new Position(centerX, centerY);
    }


    private int estimateDistanceToWall(GameUser u, Position wallPos) {
        try {
            if (u == null || u.getKingdom() == null) return Integer.MAX_VALUE / 2;
            Position p = estimateKingdomCenter(u);
            if (p == null) return Integer.MAX_VALUE / 2;
            return p.distance(wallPos);
        } catch (Throwable t) {
            return rng.nextInt(1000);
        }
    }
    private GameUser[] safeGetUsers() {
        try { GameUser[] a = game.getActiveUsers(); if (a != null) return a; } catch (Throwable ignored) {}
        try { GameUser[] b = game.getUsers(); if (b != null) return b; } catch (Throwable ignored) {}
        return new GameUser[0];
    }



    private GameUser[] getActivePlayerPool() {
        GameUser[] users = safeGetUsers();
        if (users == null) return new GameUser[0];
        int total = 0;
        for (GameUser u : users) {
            if (u != null && u.isInGame && u.getKingdom() != null) {
                GameOfThrones.PlayerState state = game.getPlayerState(u.getId());
                if (state != null && state.status == GameOfThrones.PlayerStatus.ACTIVE) {
                    total++;
                }
            }
        }
        if (total == 0) return new GameUser[0];
        GameUser[] pool = new GameUser[total];
        int idx = 0;
        for (GameUser u : users) {
            if (u != null && u.isInGame && u.getKingdom() != null) {
                GameOfThrones.PlayerState state = game.getPlayerState(u.getId());
                if (state != null && state.status == GameOfThrones.PlayerStatus.ACTIVE) {
                    pool[idx++] = u;
                }
            }
        }
        return pool;
    }

    private void launchWhiteWalkerMission(GameUser target, SoldierUnit[] armyTemplate, String message) {
        if (target == null) return;
        try {

            SoldierUnit[] armyCopy = cloneUnits(armyTemplate);
            target.setInFight(true);

            game.getPostOffice().add(new Notice(0, null, target, "حملهٔ وایت‌واکرها", message));
            game.getMissions().add(new WhiteWalkerMission(target, armyCopy));
        } catch (Throwable ignored) {}
    }


    private SoldierUnit[] cloneUnits(SoldierUnit[] src) {
        if (src == null) return new SoldierUnit[0];
        SoldierUnit[] out = new SoldierUnit[src.length];
        for (int i = 0; i < src.length; i++) {
            SoldierUnit s = src[i];
            if (s == null) { out[i] = null; continue; }
            try {
                SoldierUnit copy = new SoldierUnit(s.getType());
                try { copy.addSoldiers(s.getSoldiers()); } catch (Throwable ignored) { out[i] = s; continue; }
                out[i] = copy;
            } catch (Throwable t) {
                out[i] = s;
            }
        }
        return out;
    }
}
