
package com.yourname.got.logic.model;

import com.yourname.got.logic.login.BattleLogger;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.BattleShip;
import com.yourname.got.logic.model.Army.DefenceManager;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Army.Hero;
import com.yourname.got.logic.model.Building.*;
import com.yourname.got.logic.util.*;
import com.yourname.got.mediator.GameMediator;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Mission implements Serializable {

    private static final long serialVersionUID = 3491705858838316866L;

    private GameUser attacker;
    private GameUser defender;
    private SoldierUnit[] troopsAttacker;
    private DefenceManager defend;
    private LocalDateTime startTime;
    private int durationSeconds;
    protected boolean ambush;
    private boolean completed;
    private UserSubMap defenderMap;
    private final AttackType attackType;
    private MyLinkedList<BattleEvent> replayLog;
    private BattleShip attackerBattleShip;
    private static final double SOLDIER_EFFICIENCY = 0.9;

    // --- افزودنی‌های جدید برای قوانین بازی ---
    private boolean isConquest; // قانون 2و3: آیا حمله با هیرو است؟
    private boolean heroParticipated; // آیا هیرو در نبرد حضور دارد؟
    private boolean castleDestroyed = false; // برای بررسی شرط حذف
    // ---------------------------------------

    // Loot tracking
    private int totalWoodLoot = 0;
    private int totalStoneLoot = 0;
    private int totalFoodLoot = 0;
    private int totalCoinLoot = 0;

    // Battle outcomes
    // Attack power of every attacker soldier before any battle buff/debuff (hero buff, Eyrie malus).
    // Used to undo those temporary effects once the battle is over. Battles run atomically, so it is not saved.
    private transient java.util.IdentityHashMap<com.yourname.got.logic.model.Army.Soldier, Integer> originalAttackPower;
    private boolean troopsReturned = false;
    private boolean soldiersDefeated = false;
    private boolean complexBreached = false;
    private boolean allBuildingsDestroyed = false;

    public Mission(GameUser attacker, GameUser defender, SoldierUnit[] troops, int durationSeconds, AttackType attackType) {
        this.attacker = attacker;
        this.defender = defender;
        this.troopsAttacker = troops;
        this.startTime = LocalDateTime.now();
        this.durationSeconds = durationSeconds;
        this.completed = false;
        this.ambush = true;
        this.replayLog = new MyLinkedList<>();
        this.attackType = attackType;

        if (defender != null) {
            defender.setInFight(true);
            if (defender.getKingdom() != null) {
                this.defenderMap = defender.getKingdom().getUserMap();
                this.defend = defender.getKingdom().getArmyManager().getDefenceManager();
            }
        }

        if (attackType == AttackType.SEA) {
            this.attackerBattleShip = attacker.getKingdom().getBattleShip();
        } else {
            this.attackerBattleShip = null;
        }

        checkAmbushStatus();

        if (this.attackType != AttackType.WHITEWALKER) {
            setupAttackPath();
        }

        // ... (کدهای قبلی سازنده بدون تغییر)

        // --- تغییر: تعیین نوع حمله بر اساس حضور هیرو (قانون ۳) ---
        Hero h = attacker.getKingdom().getHero();
        if (h != null && h.isAvailable()) {
            this.isConquest = true;
            this.heroParticipated = true;
            System.out.println("Mission Type: CONQUEST (Hero Present)");
        } else {
            this.isConquest = false;
            this.heroParticipated = false;
            System.out.println("Mission Type: RAID (No Hero)");
        }
        // -------------------------------------------------------
    }

    private void setupAttackPath() {
        if (defender == null || defender.getKingdom() == null) return;
        Building entryPoint = defend.getDefenseComplex().getPrimaryTargetFor(this.attackType);
        if (entryPoint != null) {
            System.out.println("DEBUG: Entry point found: " + entryPoint.getName());
        } else {
            System.err.println("WARNING: No valid entry point found!");
        }
    }

    private void checkAmbushStatus() {
        if (defender != null && defender.getKingdom() != null && defender.getKingdom().hasSpy()) {
            this.ambush = false;
            System.out.println("Defender's spy detected the attack! Ambush cancelled.");

            // پیدا کردن یگان جاسوس‌ها
            SoldierUnit spyUnit = defender.getKingdom().getArmyManager().getDefenceManager().findUnitInDefenders(com.yourname.got.logic.enums.SoldierType.SPY);
            if (spyUnit != null && spyUnit.getCount() > 0) {
                // حذف فقط "یک" جاسوس از یگان به جای حذف کل یگان
                com.yourname.got.logic.model.Army.Soldier spyToRemove = spyUnit.getSoldiers().getHead().data;
                spyUnit.removeSoldier(spyToRemove);

                // اگر تعداد صفر شد، کل یگان را از لیست مدافعان پاک کن
                if (spyUnit.getCount() == 0) {
                    defender.getKingdom().getArmyManager().getDefenceManager().getDefenders().remove(spyUnit);
                }
            }
        } else {
            this.ambush = true;
        }
    }

    public MyLinkedList<BattleEvent> getReplayLog() {
        if (this.replayLog == null) {
            this.replayLog = new MyLinkedList<>();
        }
        return this.replayLog;
    }

    public boolean isReadyToAttack() {
        if (startTime == null) return false;
        return Duration.between(startTime, LocalDateTime.now()).getSeconds() >= durationSeconds;
    }

    public void checkMissionTime() {
        if (startTime == null) return;
        long elapsedSeconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        if (elapsedSeconds >= durationSeconds) {
            System.out.println("DEBUG: Mission duration passed.");
        }
    }

    public Boolean startAttack() {
        checkMissionTime();
        if (!isReadyToAttack()) {
            System.out.println("Attack not ready yet.");
            return false;
        }
        try {

            if (!validateAndUpdateParticipants()) {
                markCompleted();
                return true;
            }

            System.out.println("___Attack started!___");
            getReplayLog().add(new EventTextLog("Battle Started!"));
            snapshotAttackPower();

            // 1. Sea Attack Initial Bombardment
            if (this.attackType == AttackType.SEA && this.attackerBattleShip != null) {
                getReplayLog().add(new EventTextLog("Warship initiates bombardment!"));
                Building coastalTarget = defend.getDefenseComplex().getPrimaryTargetFor(AttackType.SEA);

                if (coastalTarget != null && !coastalTarget.isDestroyed()) {
                    int shipPower = this.attackerBattleShip.attackPower;
                    coastalTarget.takeDamage(shipPower);
                    getReplayLog().add(new EventTextLog("Ship hits " + coastalTarget.getName() + " for " + shipPower + " dmg."));

                    if (coastalTarget.isDestroyed()) {
                        getReplayLog().add(new EventTextLog(coastalTarget.getName() + " destroyed by ship bombardment!"));
                    }
                }
            }

            // --- تغییر: اعمال باف هیرو (اصلاح شده) ---
            if (isConquest && heroParticipated) {
                Hero h = attacker.getKingdom().getHero();
                getReplayLog().add(new EventTextLog("Hero " + h.getName() + " leads the army!"));

                // استفاده از متد جدیدی که در SoldierUnit ساختیم
                for (SoldierUnit unit : troopsAttacker) {
                    if (unit != null) {
                        unit.applyAttackBuff(1.1); // افزایش ۱۰ درصدی قدرت
                    }
                }
            }
            // ---------------------------------------
            // 2. Apply Environment Effects
            applyInitialBattleEffects();

            // 3. Execute Battle Scenario
            if (this.ambush) {
                System.out.println("--- Ambush Attack ---");
                getReplayLog().add(new EventTextLog("Ambush Attack! Defenders are surprised."));
                ambushAttackScenario();
            } else {
                System.out.println("--- Direct Attack ---");
                getReplayLog().add(new EventTextLog("Direct Attack! Defenders are ready."));
                directAttackScenario();
            }

            // --- تغییر: بررسی شرط حذف در پایان نبرد (قانون ۲) ---
            // حذف فقط زمانی رخ می‌دهد که: حمله Conquest باشد + قلعه نابود شده باشد + هیروی مدافع نباشد
            boolean defenderHeroDefeated = (defender.getKingdom().getHero() == null || !defender.getKingdom().getHero().isAvailable() || defender.getKingdom().getHero().getHealth() <= 0);

            if (this.isConquest && this.castleDestroyed && defenderHeroDefeated) {
                System.out.println("⚠️ PLAYER ELIMINATED: " + defender.getUsername());
                getReplayLog().add(new EventTextLog("⚖️ ELIMINATION: Criteria met. Kingdom Fallen."));
                attacker.getGame().handlePlayerDefeat(defender);
            }
            // ----------------------------------------------------

            // 4. Finalize
            markCompleted();
            getReplayLog().add(new EventTextLog("Battle Finished."));

            int stars = calculateStars();
            System.out.println("Final Result: " + stars + " Stars.");

            // 5. Logging and Replay Saving
            String replayFilePath = BattleLogger.saveReplayLog(getReplayLog(), getAttacker().getGame(), this);
            String mainLogFile = getAttacker().getGame().getBATTLE_LOG_FILE();

            BattleLogger.logBattle(
                this, stars, mainLogFile, replayFilePath,
                totalWoodLoot, totalStoneLoot, totalFoodLoot, totalCoinLoot
            );

            // 6. Return Troops and Clean up
            System.out.println("\n--- Processing Battle Results ---");

            restoreBuffedAttackPower();
            returnTroopsAndCleanup();
        }catch (Exception e) {
            e.printStackTrace();
            getReplayLog().add(new EventTextLog("CRITICAL ERROR: Battle aborted."));
            // Never lose the army or leave the mission / hero stuck because of an error.
            try {
                markCompleted();
                restoreBuffedAttackPower();
                returnTroopsAndCleanup();
                if (defender != null && defender.getKingdom() != null && defender.getKingdom().getHero() != null) {
                    defender.getKingdom().getHero().release();
                }
            } catch (Exception cleanupError) {
                cleanupError.printStackTrace();
            }
        }

        displayLootSummary();
        return true;
    }

    /** Remembers the attack power of every attacking soldier before buffs/debuffs are applied. */
    private void snapshotAttackPower() {
        originalAttackPower = new java.util.IdentityHashMap<>();
        if (troopsAttacker == null) return;
        for (SoldierUnit unit : troopsAttacker) {
            if (unit == null || unit.getSoldiers() == null) continue;
            for (com.yourname.got.logic.model.Army.Soldier s : unit.getSoldiers()) {
                if (s != null) originalAttackPower.put(s, s.getAttackPower());
            }
        }
    }

    /** Hero buff and kingdom maluses only last for the battle: give survivors their normal power back. */
    private void restoreBuffedAttackPower() {
        if (originalAttackPower == null || troopsAttacker == null) return;
        for (SoldierUnit unit : troopsAttacker) {
            if (unit == null || unit.getSoldiers() == null) continue;
            for (com.yourname.got.logic.model.Army.Soldier s : unit.getSoldiers()) {
                Integer original = (s == null) ? null : originalAttackPower.get(s);
                if (original != null) s.setAttackPower(original);
            }
        }
        originalAttackPower = null;
    }

    /** Sends the survivors home, rebuilds both armies and frees the rented ship. Runs only once. */
    private void returnTroopsAndCleanup() {
        if (troopsReturned) return;
        troopsReturned = true;

        System.out.println("\n--- Processing Battle Results ---");

        // Return surviving attackers
        attacker.getKingdom().getArmyManager().returnSurvivorsToDefence(this.troopsAttacker);

        // Reconcile armies (clean up dead units)
        attacker.getKingdom().getArmyManager().reconcileAllSoldiers();
        defender.getKingdom().getArmyManager().reconcileAllSoldiers();

        // Ship cleanup
        attacker.getKingdom().removeRentedShip();
        BattleShip finalShip = attacker.getKingdom().getBattleShip();
        if (finalShip != null) {
            finalShip.setAvailable(true);
            System.out.println("BattleShip returned and is available.");
        }
    }

    private void ambushAttackScenario() {
        // Phase 1: Breach Defense Complex
        getReplayLog().add(new EventTextLog("Phase 1: Breaching Defenses..."));
        if (defend.getDefenseComplex() != null && !defend.getDefenseComplex().isDestroyed(this.attackType)) {
            if (!attackDefenseComplex()) {
                getReplayLog().add(new EventTextLog("Defense Complex held. Attack repelled."));
                return;
            }
        }
        this.complexBreached = true;

        // Phase 2: Fight Soldiers (Surprise)
        getReplayLog().add(new EventTextLog("Phase 2: Engaging Garrison (Ambush)..."));
        if (getOverallSoldierCount(defend.getDefenders()) > 0) {
            // رفع باگ: خروج امن اگر نیروها نتوانند پیروز شوند
            if (!soldierVsSoldierCombat(true)) {
                getReplayLog().add(new EventTextLog("Attacking army defeated or stalemated by garrison."));
                return;
            }
        }
        this.soldiersDefeated = true;

        // Phase 3: Pillage
        getReplayLog().add(new EventTextLog("Phase 3: Pillaging Kingdom..."));
        advanceThroughDefenses();
    }

    private void directAttackScenario() {
        // Phase 1: Fight Soldiers (Prepared)
        getReplayLog().add(new EventTextLog("Phase 1: Engaging Garrison..."));
        if (getOverallSoldierCount(defend.getDefenders()) > 0) {
            // رفع باگ: خروج امن
            if (!soldierVsSoldierCombat(false)) {
                getReplayLog().add(new EventTextLog("Attacking army defeated or stalemated by garrison."));
                return;
            }
        }
        this.soldiersDefeated = true;

        // Phase 2: Breach Defense Complex
        getReplayLog().add(new EventTextLog("Phase 2: Breaching Defenses..."));
        if (defend.getDefenseComplex() != null && !defend.getDefenseComplex().isDestroyed(this.attackType)) {
            if (!attackDefenseComplex()) {
                getReplayLog().add(new EventTextLog("Defense Complex held. Attack repelled."));
                return;
            }
        }
        this.complexBreached = true;

        // Phase 3: Pillage
        getReplayLog().add(new EventTextLog("Phase 3: Pillaging Kingdom..."));
        advanceThroughDefenses();
    }

    private boolean attackDefenseComplex() {
        DefenseComplex complex = defend.getDefenseComplex();
        SoldierUnit wallArcherUnit = complex.getDefensiveArchers();

        int round = 0;
        final int MAX_ROUNDS = 50; // ** اصلاح: محدودیت راند برای محاصره **

        while (getOverallSoldierCount(troopsAttacker) > 0 &&
            (complex.getTotalHealth(this.attackType) > 0 || (wallArcherUnit != null && wallArcherUnit.getCount() > 0))) {

            // ** اصلاح: خروج اضطراری **
            if (round++ > MAX_ROUNDS) {
                getReplayLog().add(new EventTextLog("Siege stalled. Attackers pull back."));
                break;
            }

            int attackerArchersAP = 0;
            int attackerMeleeAP = 0;
            int currentRow = 0;

            for (SoldierUnit unit : troopsAttacker) {
                if (unit == null || unit.getCount() <= 0) continue;
                double power = unit.getTotalAttackPower() * Math.pow(SOLDIER_EFFICIENCY, currentRow++);
                if (unit.getType() == SoldierType.ARCHER) attackerArchersAP += power;
                else attackerMeleeAP += power;
            }

            // ** اصلاح: تضمین حداقل آسیب **
            if (attackerArchersAP == 0 && getArcherUnits(troopsAttacker).length > 0) attackerArchersAP = 1;
            if (attackerMeleeAP == 0 && getMeleeUnits(troopsAttacker).length > 0) attackerMeleeAP = 1;

            int wallArchersAP = (wallArcherUnit != null) ? wallArcherUnit.getTotalAttackPower() : 0;

            getReplayLog().add(new EventRoundUpdate("Gate Assault Round " + round, getOverallHealth(troopsAttacker), complex.getTotalHealth(this.attackType)));

            // اعمال آسیب به دیوار/دروازه
            if (wallArcherUnit != null && wallArcherUnit.getCount() > 0) {
                if (attackerArchersAP > 0) wallArcherUnit.getTotalDamage(attackerArchersAP);
                if (attackerMeleeAP > 0) complex.applyDamage(this.attackType, attackerMeleeAP);
            } else {
                complex.applyDamage(this.attackType, attackerMeleeAP + attackerArchersAP);
            }

            // آسیب مدافعان دیوار به مهاجم
            if (wallArchersAP > 0) {
                applyDamageToUnits(troopsAttacker, wallArchersAP);
            }
        }

        return complex.getTotalHealth(this.attackType) <= 0 && (wallArcherUnit == null || wallArcherUnit.getCount() <= 0);
    }
    protected boolean soldierVsSoldierCombat(boolean isSurprise) {
        MyLinkedList<SoldierUnit> defenderUnits = defend.getDefenders();
        // اگر مدافعی نیست، پیروزی سریع
        if (defenderUnits == null || defenderUnits.isEmpty()) return true;

        int round = 0;
        // ** اصلاح ۱: محدودیت تعداد راند برای جلوگیری از گیر کردن **
        final int MAX_ROUNDS = 100;

        while (getOverallSoldierCount(troopsAttacker) > 0 && getOverallSoldierCount(defenderUnits) > 0) {

            // ** اصلاح ۲: خروج اضطراری اگر جنگ طولانی شد **
            if (round++ > MAX_ROUNDS) {
                getReplayLog().add(new EventTextLog("⚠️ Battle stalemate! Forces disengage due to exhaustion."));
                System.out.println("Battle Loop Broken: Max rounds reached.");
                break;
            }

            int attackerAP = Math.max(1, calculateRowBasedAttackPower(troopsAttacker));            // ** اصلاح ۳: تضمین حداقل ۱ واحد آسیب برای پیشبرد جنگ **
            int defenderAP = calculateTotalAttackPower(defenderUnits);
            if (isSurprise) defenderAP = (int)(defenderAP * 0.5); // تضعیف در کمین            if (defenderAP <= 0) defenderAP = 1;
            defenderAP = Math.max(1, defenderAP);

            // قانون ۵: هیرو آسیب می‌بیند
            if (isConquest && heroParticipated) {
                // شانس ۱۰ درصدی برخورد تیر به هیرو
                if (new Random().nextInt(100) < 10) {
                    attacker.getKingdom().getHero().takeDamage(defenderAP / 10);
                    if (attacker.getKingdom().getHero().getHealth() <= 0) {
                        heroParticipated = false;
                        getReplayLog().add(new EventTextLog("Attacker Hero Fallen! Army morale broken."));
                        attackerAP = (int)(attackerAP * 0.7); // افت روحیه
                    }
                }
            }
            // ثبت لاگ
            getReplayLog().add(new EventTextLog("Round " + round + ": ATK Power " + attackerAP + " vs DEF Power " + defenderAP));

            // اعمال آسیب
            applyDamageToUnits(defenderUnits, attackerAP);
            applyDamageToUnits(troopsAttacker, defenderAP);

            // آپدیت نوار سلامتی در ریپلای
            getReplayLog().add(new EventRoundUpdate("Troop Clash", getOverallHealth(troopsAttacker), getOverallHealth(defenderUnits)));
        }

        // A stalemate (round limit reached with defenders still alive) is NOT a victory:
        // the attackers only win if they are alive AND the garrison is gone.
        return getOverallSoldierCount(troopsAttacker) > 0 && getOverallSoldierCount(defenderUnits) == 0;
    }

    private void advanceThroughDefenses() {
        Building entry = defend.getDefenseComplex().getPrimaryTargetFor(this.attackType);
        if (entry == null) {
            System.out.println("No entry point found for advance.");
            return;
        }

        Position currentPos = entry.getPosition();
        int maxGlobalAdvancements = 100; // محدودکننده سطح بالا برای جلوگیری از حلقه بی‌نهایت

        while (getOverallSoldierCount(troopsAttacker) > 0 && maxGlobalAdvancements-- > 0) {
            // رفع باگ گیر کردن: اگر قدرت مهاجم صفر شده باشد و مدافعی نباشد که بکشدش، خارج شو
            if (calculateRowBasedAttackPower(troopsAttacker) <= 0) {
                getReplayLog().add(new EventTextLog("Attacking forces are exhausted and cannot deal damage. Retreating."));
                break;
            }

            Building target = findNextTarget(currentPos);
            if (target == null) {
                this.allBuildingsDestroyed = true;
                getReplayLog().add(new EventTextLog("All buildings destroyed!"));
                break;
            }

            getReplayLog().add(new EventTextLog("Advancing to " + target.getName()));

            if (target instanceof Castle) {
                if (!isConquest) {
                    getReplayLog().add(new EventTextLog("Raid Limit: Cannot attack Castle without Hero. Looting area..."));
                    break; // پایان حمله چون هدف اصلی (قلعه) در دسترس نیست
                }
                handleCastleAssault((Castle) target);
                break;
            }

            attackNormalBuilding(target, currentPos);
            if (target.isDestroyed()) {
                currentPos = target.getPosition();
            }
        }
    }

    private void attackNormalBuilding(Building targetBuilding, Position attackerPosition) {
        int round = 0;
        final int MAX_ROUNDS = 20; // ** اصلاح: محدودیت برای هر ساختمان **

        while (!targetBuilding.isDestroyed() && getOverallSoldierCount(troopsAttacker) > 0) {

            // ** اصلاح: خروج اضطراری **
            if (round++ > MAX_ROUNDS) {
                getReplayLog().add(new EventTextLog("Could not destroy " + targetBuilding.getName() + " in time. Moving on."));
                break;
            }

            int damage = calculateRowBasedAttackPower(troopsAttacker);
            // ** اصلاح: حداقل آسیب ۵ برای تخریب **
            if (damage <= 0) damage = 5;

            TerrainEffect effect = getTerrainEffect(attackerPosition);
            damage = (int) (damage * effect.getAttackModifier());

            if(effect.getHealthReductionPercentage() > 0) {
                for(SoldierUnit u : troopsAttacker) if(u!=null) u.applyHealthReductionPercentage(effect.getHealthReductionPercentage());
            }

            targetBuilding.takeDamage(damage);

            // آسیب برج‌های دفاعی
            int towerDamage = calculateTowerDamage(attackerPosition);
            if (towerDamage > 0) {
                applyDamageToUnits(troopsAttacker, towerDamage);
            }
        }

        if (targetBuilding.isDestroyed()) {
            getReplayLog().add(new EventTextLog(targetBuilding.getName() + " destroyed!"));
            processLoot(targetBuilding);
        }
    }

    private void handleCastleAssault(Castle castle) {
        Hero defHero = defender.getKingdom().getHero();
        Hero atkHero = attacker.getKingdom().getHero();

        // --- تغییر: منطق دوئل (قانون ۱۵) ---
        if (defHero != null && defHero.isAvailable() && isConquest && heroParticipated) {
            getReplayLog().add(new EventTextLog(">>> HERO DUEL INITIATED at Castle Gates <<<"));

            // محاسبه برنده دوئل
            int atkPower = atkHero.getAttackBonus() + new Random().nextInt(50);
            int defPower = defHero.getDefenseBonus() + new Random().nextInt(50);
            boolean atkWon = atkPower >= defPower;

            getReplayLog().add(new EventHeroDuel(atkHero.getName(), defHero.getName(), atkWon));

            if (atkWon) {
                getReplayLog().add(new EventTextLog("Attacker Hero Won! Castle Defenses collapsed."));
                defHero.setHealth(0); // کشتن هیروی مدافع
                castle.destroy();
                this.castleDestroyed = true;
            } else {
                getReplayLog().add(new EventTextLog("Defender Hero Won! Attack Repelled."));
                atkHero.setHealth(0); // کشتن هیروی مهاجم
                this.heroParticipated = false;
                applyDamageToUnits(troopsAttacker, 9999999); // نابودی ارتش مهاجم (بدون هیرو فرار نمی‌کنند)
            }
        }
        // حالت عادی (بدون دوئل)
        else if (defHero == null || !defHero.isAvailable()) {
            getReplayLog().add(new EventTextLog("Castle Undefended by Hero. Breaching..."));
            castle.destroy();
            this.castleDestroyed = true;
        } else {
            // حالت خاص: مدافع هیرو دارد ولی مهاجم ندارد (که طبق لاجیک Raid نباید به اینجا برسد، اما محض اطمینان)
            getReplayLog().add(new EventTextLog("Defender Hero repelled the army easily."));
            applyDamageToUnits(troopsAttacker, 9999999);
        }
    }

    public boolean soldierVsSoldierCombatAdvanced() {
        if (troopsAttacker == null || troopsAttacker.length == 0) return false;

        Kingdom k = (defender != null) ? defender.getKingdom() : null;

        MyLinkedList<SoldierUnit> defenders = null;
        if (k != null) {
            try {
                defenders = k.getArmyManager().getDefenceManager().getDefenders();
            } catch (Throwable ignored) {
            }
        }

        if (defenders == null || defenders.getSize() == 0) return true;

        final boolean isWhiteWalkerFight = (this.attackType == AttackType.WHITEWALKER);

        if (isWhiteWalkerFight && k != null) {
            try {
                int dg = k.getDragonGlass();
                if (dg > 0) k.equipSoldiersWithDragonGlass(dg);
            } catch (Throwable ignored) {
            }
        }

        int heroAttackBonus = 0;
        int heroDefenseBonus = 0;
        if (k != null && k.getHero() != null && k.getHero().isAvailable() && k.getHero().getHealth() > 0) {
            heroAttackBonus = k.getHero().getAttackBonus();
            heroDefenseBonus = k.getHero().getDefenseBonus();
            k.getHero().markBusy();
            System.out.println("🦸 قهرمان [" + k.getHero().getName() + "] در نبرد فعال شد! (+" +
                heroAttackBonus + " ATK, +" + heroDefenseBonus + " DEF)");
        }

        Random rng = new Random();
        int round = 0;
        final int MAX_ROUNDS = 10000;

        while (round++ < MAX_ROUNDS) {
            long atkPower = 0L;
            for (int i = 0; i < troopsAttacker.length; i++) {
                SoldierUnit su = troopsAttacker[i];
                if (su == null) continue;
                int cnt = su.getCount();
                if (cnt <= 0) continue;
                atkPower += su.getTotalAttackPower();
            }

            long defPower = 0L;
            Node<SoldierUnit> cur = defenders.getHead();
            while (cur != null) {
                SoldierUnit du = cur.data;
                if (du != null && du.getCount() > 0) {
                    if (isWhiteWalkerFight) defPower += du.getAdjustedAttackPowerAgainstWhiteWalkers();
                    else defPower += du.getTotalAttackPower();
                }
                cur = cur.next;
            }
            // The defending hero helps the defenders (both bonuses), never the attackers.
            defPower += heroAttackBonus + heroDefenseBonus;

            if (atkPower <= 0L) {
                if (k != null && k.getHero() != null) k.getHero().release();
                return false;
            }
            if (defPower <= 0L) {
                if (k != null && k.getHero() != null) k.getHero().release();
                return true;
            }

            long damageToDef = (long) (atkPower * (0.6 + 0.2 * rng.nextDouble()));
            long damageToAtk = (long) (defPower * (0.6 + 0.2 * rng.nextDouble()));

            long pending = damageToDef;
            Node<SoldierUnit> dcur = defenders.getHead();
            while (dcur != null && pending > 0) {
                SoldierUnit du = dcur.data;
                if (du != null && du.getCount() > 0) {
                    pending = du.getTotalDamage((int) Math.min(Integer.MAX_VALUE, pending));
                }
                dcur = dcur.next;
            }

            long pendA = damageToAtk;
            for (int i = 0; i < troopsAttacker.length && pendA > 0; i++) {
                SoldierUnit au = troopsAttacker[i];
                if (au == null || au.getCount() <= 0) continue;
                pendA = au.getTotalDamage((int) Math.min(Integer.MAX_VALUE, pendA));
            }

            boolean anyDefLeft = false;
            dcur = defenders.getHead();
            while (dcur != null) {
                SoldierUnit du2 = dcur.data;
                if (du2 != null && du2.getCount() > 0) {
                    anyDefLeft = true;
                    break;
                }
                dcur = dcur.next;
            }
            boolean anyAtkLeft = false;
            for (int i = 0; i < troopsAttacker.length; i++) {
                SoldierUnit au2 = troopsAttacker[i];
                if (au2 != null && au2.getCount() > 0) {
                    anyAtkLeft = true;
                    break;
                }
            }

            if (!anyAtkLeft) {
                if (k != null && k.getHero() != null) k.getHero().release();
                return false;
            }
            if (!anyDefLeft) {
                if (k != null && k.getHero() != null) k.getHero().release();
                return true;
            }
        }

        int leftAtk = 0;
        for (int i = 0; i < troopsAttacker.length; i++) {
            SoldierUnit su = troopsAttacker[i];
            if (su != null) leftAtk += su.getCount();
        }
        int leftDef = 0;
        Node<SoldierUnit> cur2 = defenders.getHead();
        while (cur2 != null) {
            SoldierUnit du = cur2.data;
            if (du != null) leftDef += du.getCount();
            cur2 = cur2.next;
        }

        if (k != null && k.getHero() != null) k.getHero().release();
        return leftAtk > leftDef;
    }

    private Building findNextTarget(Position currentPos) {
        Building closest = null;
        int minDist = Integer.MAX_VALUE;

        for (Building b : defender.getKingdom().getBuildings()) {
            if (b != null && !b.isDestroyed() && !(b instanceof Castle) && !isDefenseStructure(b)) {
                int dist = calculateDistance(currentPos, b.getPosition());
                if (dist < minDist) {
                    minDist = dist;
                    closest = b;
                }
            }
        }

        if (closest == null) {
            Building castle = defender.getKingdom().getCastle();
            if (castle != null && !castle.isDestroyed()) return castle;
        }
        return closest;
    }

    private boolean isDefenseStructure(Building b) {
        return b instanceof Gate || b instanceof WallsOfGate || b instanceof Port;
    }

    private void processLoot(Building b) {
        int loot = defender.getKingdom().processBuildingDestruction(b, attacker.getKingdom());
        if (loot > 0 && b instanceof Storage) {
            String type = ((Storage) b).getResourceType().toUpperCase();
            switch (type) {
                case "WOOD": totalWoodLoot += loot; break;
                case "STONE": totalStoneLoot += loot; break;
                case "FOOD": totalFoodLoot += loot; break;
                case "COIN": totalCoinLoot += loot; break;
            }
            getReplayLog().add(new EventTextLog("Looted " + loot + " " + type));
        }
    }

    private void applyInitialBattleEffects() {
        if (defender.getKingdom().getType() == KingdomType.WINTERFELL) {
            getReplayLog().add(new EventTextLog("Winterfell Bonus: Attacker HP reduced by 10%"));
            for (SoldierUnit unit : troopsAttacker) if(unit!=null) unit.applyHealthReductionPercentage(10);
        }
        if (defender.getKingdom().getType() == KingdomType.EYRIE) {
            getReplayLog().add(new EventTextLog("Eyrie Bonus: Attacker Power reduced by 15%"));
            for (SoldierUnit unit : troopsAttacker) if(unit!=null) unit.applyAttackPowerReductionPercentage(15);
        }
    }

    private boolean validateAndUpdateParticipants() {
        if (attacker.getGame() == null) return false;
        this.attacker = attacker.getGame().getUser(attacker.getId());
        this.defender = attacker.getGame().getUser(defender.getId());

        if (attacker == null || defender == null) return false;
        if (defender.getKingdom() == null) return false;

        this.defenderMap = defender.getKingdom().getUserMap();
        this.defend = defender.getKingdom().getArmyManager().getDefenceManager();
        return true;
    }

    public void markCompleted() {
        this.completed = true;
        if (defender != null) defender.setInFight(false);
    }

    private int calculateRowBasedAttackPower(SoldierUnit[] units) {
        double power = 0;
        int row = 0;
        for (SoldierUnit u : units) {
            if (u != null && u.getCount() > 0) {
                power += u.getTotalAttackPower() * Math.pow(SOLDIER_EFFICIENCY, row++);
            }
        }
        return (int) power;
    }

    private int calculateTotalAttackPower(MyLinkedList<SoldierUnit> units) {
        SoldierUnit[] arr = new SoldierUnit[units.size()];
        units.fillArray(arr);
        int power = 0;
        for (SoldierUnit u : arr) {
            if (u != null && u.getCount() > 0) power += u.getTotalAttackPower();
        }
        return power;
    }

    private int calculateTowerDamage(Position pos) {
        int damage = 0;
        MyLinkedList<DefensiveTower> towers = defend.getTowers();
        if (towers != null) {
            for (DefensiveTower t : towers) {
                if (calculateDistance(t.getPosition(), pos) <= t.getRange()) {
                    damage += t.getAttackPower();
                }
            }
        }
        return damage;
    }

    private void applyDamageToUnits(SoldierUnit[] units, int damage) {
        if (units == null || damage <= 0) return;
        for (SoldierUnit u : units) {
            if (damage <= 0) break;
            if (u != null && u.getCount() > 0) {
                damage = u.getTotalDamage(damage);
            }
        }
    }

    private void applyDamageToUnits(MyLinkedList<SoldierUnit> units, int damage) {
        if (units == null || damage <= 0) return;
        Node<SoldierUnit> node = units.getHead();
        while (node != null && damage > 0) {
            SoldierUnit u = node.data;
            Node<SoldierUnit> nextNode = node.next; // Save next ref before potential remove
            if (u != null && u.getCount() > 0) {
                damage = u.getTotalDamage(damage);
                if (u.getCount() == 0) units.remove(u);
            }
            node = nextNode;
        }
    }

    private long getOverallHealth(SoldierUnit[] units) {
        long h = 0;
        if (units != null) for (SoldierUnit u : units) if (u != null) h += u.getTotalHealth();
        return h;
    }

    private long getOverallHealth(MyLinkedList<SoldierUnit> units) {
        // Safe conversion for UI
        SoldierUnit[] arr = new SoldierUnit[units.size()];
        units.fillArray(arr);
        return getOverallHealth(arr);
    }

    private int getOverallSoldierCount(SoldierUnit[] units) {
        int c = 0;
        if (units != null) for (SoldierUnit u : units) if (u != null) c += u.getCount();
        return c;
    }

    private int getOverallSoldierCount(MyLinkedList<SoldierUnit> units) {
        int c = 0;
        Node<SoldierUnit> n = units.getHead();
        while (n != null) {
            if(n.data != null) c += n.data.getCount();
            n = n.next;
        }
        return c;
    }

    private int calculateDistance(Position a, Position b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private SoldierUnit[] getArcherUnits(SoldierUnit[] units) {
        List<SoldierUnit> list = new ArrayList<>();
        for (SoldierUnit u : units) if (u != null && u.getType() == SoldierType.ARCHER) list.add(u);
        return list.toArray(new SoldierUnit[0]);
    }

    private SoldierUnit[] getMeleeUnits(SoldierUnit[] units) {
        List<SoldierUnit> list = new ArrayList<>();
        for (SoldierUnit u : units) if (u != null && u.getType() != SoldierType.ARCHER) list.add(u);
        return list.toArray(new SoldierUnit[0]);
    }

    private TerrainEffect getTerrainEffect(Position position) {
        TerrainType terrainType = defender.getKingdom().getUserMap().mainMap.mainMap[position.getX()][position.getY()].terrainType;
        switch (terrainType) {
            case MOUNTAIN:
                return new TerrainEffect(0.9, 0); // 10% less attack
            case SNOW:
                return new TerrainEffect(0.8, 5); // 20% less attack, 5% hp loss
            default:
                return new TerrainEffect(1.0, 0);
        }
    }

    private void displayLootSummary() {
        System.out.println("Loot Summary: W:" + totalWoodLoot + " S:" + totalStoneLoot + " F:" + totalFoodLoot + " C:" + totalCoinLoot);
    }

    // Getters
    public GameUser getAttacker() { return attacker; }
    public GameUser getDefender() { return defender; }
    public SoldierUnit[] getAttackerTroops() { return troopsAttacker; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getInitialDuration() { return durationSeconds; }
    public int getElapsedTime() {
        if (startTime == null) return 0;
        return (int) Duration.between(startTime, LocalDateTime.now()).getSeconds();
    }
    public int getRemainingTime() {
        return Math.max(0, durationSeconds - getElapsedTime());
    }

    public int calculateStars() {
        int s = 0;
        if (soldiersDefeated) s++;
        if (complexBreached) s++;
        if (allBuildingsDestroyed) s++;
        return s;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isCompleted() { return completed; }
}
