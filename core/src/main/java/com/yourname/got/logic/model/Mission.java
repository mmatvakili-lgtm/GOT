//package com.yourname.got.logic.model;
//
//import com.yourname.got.logic.login.BattleLogger;
//import com.yourname.got.logic.login.GameUser;
//import com.yourname.got.logic.enums.*;
//import com.yourname.got.logic.model.Army.BattleShip;
//import com.yourname.got.logic.model.Army.DefenceManager;
//import com.yourname.got.logic.model.Army.SoldierUnit;
//import com.yourname.got.logic.model.Army.Hero;
//import com.yourname.got.logic.model.Building.*;
//import com.yourname.got.logic.enums.SoldierType;
//import com.yourname.got.logic.util.*;
//import com.yourname.got.mediator.GameMediator;
//
//import java.io.Serializable;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class Mission implements Serializable {
//
//    private GameUser attacker;
//    private GameUser defender;
//    private SoldierUnit[] troopsAttacker;
//    private DefenceManager defend;
//    private LocalDateTime startTime;
//    private int durationSeconds;
//    protected boolean ambush;
//    private boolean completed;
//    private UserSubMap defenderMap;
//    private final AttackType attackType;
//    private int dragonGlassUsed;
//    private  MyLinkedList<BattleEvent> replayLog;
//    private static final long serialVersionUID = 3491705858838316866L;
//    private BattleShip attackerBattleShip;
//    private static double SoldierEfficiency = 0.9;
//    private int totalWoodLoot = 0;
//    private int totalStoneLoot = 0;
//    private int totalFoodLoot = 0;
//    private int totalCoinLoot = 0;
//    private boolean soldiersDefeated = false;
//    private boolean complexBreached = false;
//    private boolean allBuildingsDestroyed = false;
//
//    private void setupAttackPath() {
//        if (defender == null || defender.getKingdom() == null) return;
//
//        Building entryPoint = defend.getDefenseComplex().getPrimaryTargetFor(this.attackType);
//        if (entryPoint != null) {
//            System.out.println("نقطه ورود حمله: " + entryPoint.getName() + ". بازسازی مسیر حمله...");
//        } else {
//            System.err.println("هشدار: هیچ نقطه ورودی معتبری (دروازه/بندر) برای این نوع حمله یافت نشد!");
//        }
//    }
//
//    public Mission(GameUser attacker, GameUser defender, SoldierUnit[] troops, int durationSeconds, AttackType attackType) {
//        this.attacker = attacker;
//        this.defender = defender;
//        this.troopsAttacker = troops;
//        this.startTime = LocalDateTime.now();
//        this.durationSeconds = durationSeconds;
//        this.completed = false;
//        this.ambush = true;
//        this.replayLog = new MyLinkedList<>();
//        this.attackType = attackType;
//
//        defender.setInFight(true);
//
//        if (defender != null && defender.getKingdom() != null) {
//            this.defenderMap = defender.getKingdom().getUserMap();
//            this.defend = defender.getKingdom().getArmyManager().getDefenceManager();
//        } else {
//            System.err.println("Warning: Defender or Kingdom is null.");
//        }
//        if (attackType == AttackType.SEA) {
//            this.attackerBattleShip = attacker.getKingdom().getBattleShip();
//        } else {
//            this.attackerBattleShip = null;
//        }
//        setAmbush();
//
//        if (this.attackType != AttackType.WHITEWALKER) {
//            setupAttackPath();
//        }
//
//    }
//
//    public MyLinkedList<BattleEvent> getReplayLog() {
//        if (this.replayLog == null) {
//            this.replayLog = new MyLinkedList<>();
//        }
//        return this.replayLog;
//    }
//
//
//    public void checkMissionTime() {
//        if (startTime == null) {
//            System.out.println("⚠️ [DEBUG] Mission has not started yet.");
//            return;
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//        Duration elapsed = Duration.between(startTime, now);
//        long elapsedSeconds = elapsed.getSeconds();
//
//        System.out.println("📊 [DEBUG] Now: " + now);
//        System.out.println("📊 [DEBUG] Elapsed seconds: " + elapsedSeconds);
//
//        if (elapsedSeconds >= durationSeconds) {
//            System.out.println("✅ Mission duration passed!");
//        } else {
//            System.out.println("⏳ Still waiting... (" + (durationSeconds - elapsedSeconds) + " seconds remaining)");
//        }
//    }
//
//    public Boolean startAttack() {
//        checkMissionTime();
//        if (!isReadyToAttack()) {
//            System.out.println("Attack not ready yet.");
//            return false;
//        }
//        if (!validateAndUpdateParticipants()) {
//            return true;
//        }
//        System.out.println("___Attack started!___");
//        getReplayLog().add(new EventTextLog("Battle Started!"));
//
//        if (this.attackType == AttackType.SEA && this.attackerBattleShip != null) {
//            System.out.println("--- 🌊 کشتی جنگی حمله اولیه خود را آغاز می‌کند! ---");
//            Building coastalTarget = defend.getDefenseComplex().getPrimaryTargetFor(AttackType.SEA);
//
//            if (coastalTarget != null && !coastalTarget.isDestroyed()) {
//                int shipPower = this.attackerBattleShip.attackPower;
//                System.out.println("کشتی به " + coastalTarget.getName() + " با قدرت " + shipPower + " حمله می‌کند.");
//                coastalTarget.takeDamage(shipPower);
//
//                if (coastalTarget.isDestroyed()) {
//                    System.out.println("هدف ساحلی (" + coastalTarget.getName() + ") توسط حمله اولیه کشتی نابود شد!");
//                } else {
//                    System.out.println("سلامتی فعلی " + coastalTarget.getName() + ": " + coastalTarget.getHealth());
//                }
//            } else {
//                System.out.println("کشتی هدف ساحلی مناسبی برای حمله پیدا نکرد.");
//            }
//            System.out.println("-------------------------------------------------");
//        }
//        applyInitialBattleEffects();
//
//        if (isAmbush()) {
//            System.out.println("\n--- Ambush attack initiated! (Defender soldiers are surprised) ---");
//            getReplayLog().add(new EventTextLog("Ambush Attack! Defenders are surprised."));
//            ambushAttackScenario();
//        } else {
//            System.out.println("\n--- Direct attack initiated! ---");
//            getReplayLog().add(new EventTextLog("Direct Attack! Defenders are ready."));
//            directAttackScenario();
//        }
//
//        markCompleted();
//        System.out.println("\n___نبرد کامل شد.___");
//        getReplayLog().add(new EventTextLog("Battle Finished."));
//
//
//        int stars = calculateStars();
//        System.out.println("نتیجه نهایی: " + stars + " ستاره کسب شد.");
//        String replayFilePath = BattleLogger.saveReplayLog(getReplayLog(), getAttacker().getGame(), this);
//        String mainLogFile = getAttacker().getGame().getBATTLE_LOG_FILE();
//        BattleLogger.logBattle(
//            this,
//            stars,
//            mainLogFile,
//            replayFilePath,
//            totalWoodLoot,
//            totalStoneLoot,
//            totalFoodLoot,
//            totalCoinLoot
//        );System.out.println("\n--- پردازش نتایج نهایی نبرد ---");
//
//        GameUser attacker = this.getAttacker();
//        attacker.getKingdom().getArmyManager().returnSurvivorsToDefence(this.getAttackerTroops());
//
//        GameUser defender = this.getDefender();
//
//        System.out.println("\n> ثبت تلفات برای مهاجم:");
//        attacker.getKingdom().getArmyManager().reconcileAllSoldiers();
//
//        System.out.println("\n> ثبت تلفات برای مدافع:");
//        defender.getKingdom().getArmyManager().reconcileAllSoldiers();
//        attacker.getKingdom().removeRentedShip();
//        BattleShip finalShip = attacker.getKingdom().getBattleShip();
//        if (finalShip != null) {
//            finalShip.setAvailable(true);
//            System.out.println("کشتی جنگی شما از مأموریت بازگشت و اکنون دوباره در دسترس است.");
//        }
//        displayLootSummary();
//
//        System.out.println("\n✅ پردازش نتایج جنگ با موفقیت به پایان رسید.");
//
//        return true;
//    }
//
//    private long getOverallHealth(SoldierUnit[] units) {
//        long totalHealth = 0;
//        if (units == null) return 0;
//        for (SoldierUnit unit : units) {
//            if (unit != null) totalHealth += unit.getTotalHealth();
//        }
//        return totalHealth;
//    }
//
//    private boolean validateAndUpdateParticipants() {
//        GameOfThrones game = attacker.getGame();
//        if (game == null) {
//            System.out.println("خطای سیستمی: دسترسی به آبجکت اصلی بازی امکان‌پذیر نیست. حمله لغو شد.");
//            return false;
//        }
//
//        GameUser currentAttacker = game.getUser(this.attacker.getId());
//        if (currentAttacker == null) {
//            System.out.println("حمله لغو شد زیرا مهاجم دیگر در بازی وجود ندارد.");
//            return false;
//        }
//        this.attacker = currentAttacker;
//
//        GameUser currentDefender = game.getUser(this.defender.getId());
//        if (currentDefender == null || game.getPlayerState(this.defender.getId()).status == GameOfThrones.PlayerStatus.DEFEATED) {
//            System.out.println("هدف حمله دیگر وجود ندارد یا قبلاً شکست خورده است. سربازان بدون درگیری به خانه بازمی‌گردند.");
//            this.attacker.getKingdom().getArmyManager().returnSurvivorsToDefence(this.troopsAttacker);
//            this.attacker.getKingdom().removeRentedShip();
//            BattleShip finalShip = this.attacker.getKingdom().getBattleShip();
//            if (finalShip != null) {
//                finalShip.setAvailable(true);
//            }
//
//            return false;
//        }
//        this.defender = currentDefender;
//
//        this.defenderMap = this.defender.getKingdom().getUserMap();
//        this.defend = this.defender.getKingdom().getArmyManager().getDefenceManager();
//
//        return true;
//    }
//

//
//    private void ambushAttackScenario() {
//        System.out.println("\n--- فاز اول: حمله به مجموعه دفاعی ---");
//        if (defend.getDefenseComplex() != null && !defend.getDefenseComplex().isDestroyed(this.attackType)) {
//            if (!attackDefenseComplex()) {
//                System.out.println("مجموعه دفاعی مقاومت کرد. حمله ناموفق.");
//                return;
//            }
//        } else {
//            System.out.println("گیت نابود شده یا وجود ندارد :");
//        }
//        this.complexBreached = true;
//
//        System.out.println("\n--- فاز دوم: نبرد با سربازان غافلگیر شده ---");
//        if (getOverallSoldierCount(defend.getDefenders()) > 0 && !soldierVsSoldierCombat(true)) {
//            System.out.println("نبرد سربازان با شکست مهاجم همراه بود. حمله ناموفق.");
//            return;
//        }
//        this.soldiersDefeated = true;
//
//        System.out.println("\n--- فاز سوم: پیشروی در قلعه ---");
//        advanceThroughDefenses();
//    }
//
//    private void applyInitialBattleEffects() {
//        if (defender != null && defender.getKingdom().getType() == KingdomType.WINTERFELL) {
//            int healthReductionPercent = 10; // 10% کاهش جان
//            System.out.println("❄️ بادهای سرد وینترفل ارتش مهاجم را تضعیف می‌کند!");
//            getReplayLog().add(new EventTextLog("❄️ Winterfell's cold winds weaken the attacking army!")); // <<-- REPLAY LOGGING
//            System.out.println("   > اعمال " + healthReductionPercent + "% کاهش جان به تمام مهاجمان.");
//            for (SoldierUnit unit : troopsAttacker) {
//                if (unit != null) {
//                    unit.applyHealthReductionPercentage(healthReductionPercent);
//                }
//            }
//        }
//        if (defender != null && defender.getKingdom().getType() == KingdomType.EYRIE) {
//            int attackReductionPercent = 15;
//            System.out.println("کوه ها و دره های ایری قابلیت های حمله مهاجمان را کم کرده است!");
//            getReplayLog().add(new EventTextLog("⛰️ Eyrie's terrain reduces attacker power!")); // <<-- REPLAY LOGGING
//            System.out.println("   > اعمال " + attackReductionPercent + "% کاهش قدرت حمله به تمام مهاجمان.");
//            for (SoldierUnit unit : troopsAttacker) {
//                if (unit != null) {
//                    unit.applyAttackPowerReductionPercentage(attackReductionPercent);
//                }
//            }
//        }
//    }
//
//    private void directAttackScenario() {
//        System.out.println("\n--- فاز اول: نبرد با سربازان آماده ---");
//        if (getOverallSoldierCount(defend.getDefenders()) > 0 && !soldierVsSoldierCombat(false)) {
//            System.out.println("نبرد سربازان با شکست مهاجم همراه بود. حمله ناموفق.");
//            return;
//        }
//        this.soldiersDefeated = true;
//
//        System.out.println("\n--- فاز دوم: حمله به مجموعه دفاعی ---");
//        if (defend.getDefenseComplex() != null && !defend.getDefenseComplex().isDestroyed(this.attackType)) {
//            if (!attackDefenseComplex()) {
//                System.out.println("مجموعه دفاعی مقاومت کرد. حمله ناموفق.");
//                return;
//            }
//        }
//        this.complexBreached = true;
//
//        System.out.println("\n--- فاز سوم: پیشروی در قلعه ---");
//        advanceThroughDefenses();
//    }
//    private boolean attackDefenseComplex() {
//        DefenseComplex complex = defend.getDefenseComplex();
//        SoldierUnit wallArcherUnit = complex.getDefensiveArchers();
//
//        while (getOverallSoldierCount(troopsAttacker) > 0 &&
//            (complex.getTotalHealth(this.attackType) > 0 ||
//                (wallArcherUnit != null && wallArcherUnit.getCount() > 0))) {
//
//            System.out.println("\n--- دور جدید نبرد در دروازه ---");
//            getReplayLog().add(new EventTextLog("--- دور جدید نبرد در دروازه ---"));
//
//            int attackerArchersAP = 0;
//            int attackerMeleeAP = 0;
//            int currentRow = 0;
//            int wallArchersAP = 0;
//
//            for (SoldierUnit unit : troopsAttacker) {
//                if (unit == null || unit.getCount() <= 0) continue;
//                double powerInRow = unit.getTotalAttackPower() * Math.pow(SoldierEfficiency, currentRow);
//                if (unit.getType() == SoldierType.ARCHER) {
//                    attackerArchersAP += powerInRow;
//                } else {
//                    attackerMeleeAP += powerInRow;
//                }
//                currentRow++;
//            }
//
//            // به‌روزرسانی وضعیت سلامتی (برای ProgressBar)
//            getReplayLog().add(new EventRoundUpdate(
//                "Gate Assault: Attackers (Power: " + (attackerArchersAP + attackerMeleeAP) +
//                    ") vs Wall Archers (Power: " + wallArchersAP + ")",
//                getOverallHealth(troopsAttacker),
//                complex.getTotalHealth(this.attackType)
//            ));
//
//            if (wallArcherUnit != null && wallArcherUnit.getCount() > 0) {
//                wallArchersAP = wallArcherUnit.getTotalAttackPower();
//            }
//
//            System.out.println("  > قدرت‌های محاسبه شده: [مهاجمان: " +
//                (attackerArchersAP + attackerMeleeAP) +
//                "], [کمانداران دیوار: " + wallArchersAP + "]");
//            getReplayLog().add(new EventTextLog("قدرت محاسبه شده: مهاجمان=" +
//                (attackerArchersAP + attackerMeleeAP) + " | کمانداران دیوار=" + wallArchersAP));
//
//            if (wallArcherUnit != null && wallArcherUnit.getCount() > 0) { // سناریو دوئل
//                if (attackerArchersAP > 0) {
//                    wallArcherUnit.getTotalDamage(attackerArchersAP);
//                    getReplayLog().add(new EventTextLog("مهاجمان به کمانداران دیوار ضربه زدند (" + attackerArchersAP + " قدرت)."));
//                }
//                if (attackerMeleeAP > 0) {
//                    complex.applyDamage(this.attackType, attackerMeleeAP);
//                    getReplayLog().add(new EventTextLog("مهاجمان به دروازه ضربه زدند (" + attackerMeleeAP + " قدرت)."));
//                }
//            } else {
//                int totalDamageToGate = attackerMeleeAP + attackerArchersAP;
//                if (totalDamageToGate > 0) {
//                    complex.applyDamage(this.attackType, totalDamageToGate);
//                    getReplayLog().add(new EventTextLog("مهاجمان به دروازه ضربه مستقیم زدند (" + totalDamageToGate + " قدرت)."));
//                }
//            }
//
//            if (wallArchersAP > 0) {
//                System.out.println("  🎯 کمانداران روی دیوار (" + wallArchersAP + " قدرت) به کل ارتش مهاجم شلیک می‌کنند.");
//                getReplayLog().add(new EventTextLog("🎯 کمانداران روی دیوار (" + wallArchersAP + ") به مهاجمان شلیک کردند."));
//                applyDamageToUnits(troopsAttacker, wallArchersAP);
//            }
//
//            System.out.println("  > وضعیت پایان دور: [سلامت دروازه: " +
//                complex.getTotalHealth(this.attackType) + "], [کمانداران دیوار: " +
//                (wallArcherUnit != null ? wallArcherUnit.getCount() : 0) +
//                "], [مهاجمان: " + getOverallSoldierCount(troopsAttacker) + "]");
//            getReplayLog().add(new EventRoundUpdate(
//                "پایان دور: سلامت به‌روزرسانی شد",
//                getOverallHealth(troopsAttacker),
//                complex.getTotalHealth(this.attackType)
//            ));
//        }
//
//        if (getOverallSoldierCount(troopsAttacker) <= 0) {
//            System.out.println("\n⚔️ حمله با شکست مواجه شد. تمام نیروهای مهاجم نابود شدند.");
//            getReplayLog().add(new EventTextLog("⚔️ حمله با شکست مواجه شد. تمام نیروهای مهاجم نابود شدند."));
//            return false;
//        }
//        if (complex.getTotalHealth(this.attackType) <= 0 &&
//            (wallArcherUnit == null || wallArcherUnit.getCount() <= 0)) {
//            System.out.println("\n🎉 مجموعه دفاعی (دروازه و کماندارانش) به طور کامل نابود شد!");
//            getReplayLog().add(new EventTextLog("🎉 مجموعه دفاعی (دروازه و کماندارانش) نابود شد!"));
//            return true;
//        }
//        System.out.println("\n⚔️ حمله ناموفق بود. نیروهای مهاجم برای نابودی کامل مجموعه دفاعی کافی نبودند.");
//        getReplayLog().add(new EventTextLog("⚔️ حمله ناموفق بود. نیروهای مهاجم برای نابودی کامل مجموعه دفاعی کافی نبودند."));
//        return false;
//    }
//
//    private void advanceThroughDefenses() {
//        Building entryPointBuilding = defend.getDefenseComplex().getPrimaryTargetFor(this.attackType);
//        if (entryPointBuilding == null) {
//            System.out.println("خطا: نقطه ورود برای پیشروی یافت نشد.");
//            return;
//        }
//        Position attackerPosition = entryPointBuilding.getPosition();
//
//        while (getOverallSoldierCount(troopsAttacker) > 0) {
//            MyLinkedList<Building> allBuildings = defender.getKingdom().getBuildings();
//            Building targetBuilding = findNextTarget(attackerPosition, allBuildings);
//
//            if (targetBuilding == null) {
//                this.allBuildingsDestroyed = true;
//                break;
//            }
//
//            System.out.println("\n----------------------------------------------------");
//            System.out.println(">> هدف جدید: " + targetBuilding.getName() + " در " + targetBuilding.getPosition() + " <<");
//
//            if (targetBuilding instanceof Castle) {
//                System.out.println("  > ارتش به قلعه دشمن رسیده است! سناریوی پایانی فعال شد.");
//                handleCastleAssault((Castle) targetBuilding);
//                break;
//            }
//
//            attackNormalBuilding(targetBuilding, attackerPosition);
//
//            if (targetBuilding.isDestroyed()) {
//                attackerPosition = targetBuilding.getPosition();
//            }
//        }
//
//        if (this.allBuildingsDestroyed) {
//            System.out.println("\n--- پیروزی! تمام ساختمان‌های دشمن نابود شدند. ---");
//        } else if (getOverallSoldierCount(troopsAttacker) <= 0) {
//            System.out.println("\n--- شکست! تمام سربازان مهاجم از بین رفتند. ---");
//        }
//    }
//
//    private Building findNextTarget(Position currentPos, MyLinkedList<Building> allBuildings) {
//        Building closestOtherBuilding = null;
//        int minDistance = Integer.MAX_VALUE;
//        Castle castle = null;
//
//        for (Building building : allBuildings) {
//            if (building == null || building.isDestroyed()) {
//                continue;
//            }
//
//            if (building instanceof Castle) {
//                castle = (Castle) building;
//            } else if (!(building instanceof Gate) && !(building instanceof WallsOfGate) && !(building instanceof Port)) {
//                int distance = currentPos.distance(building.getPosition());
//                if (distance < minDistance) {
//                    minDistance = distance;
//                    closestOtherBuilding = building;
//                }
//            }
//        }
//
//        if (closestOtherBuilding != null) {
//            return closestOtherBuilding;
//        }
//
//        return castle;
//    }
//
//
//    private void handleCastleAssault(Castle castle) {
//        Hero defenderHero = (defender.getKingdom() != null) ? defender.getKingdom().getHero() : null;
//
//        if (defenderHero == null || !defenderHero.isAvailable()) {
//            System.out.println("🔥 مدافع قهرمان آماده‌ای برای دفاع ندارد! قلعه سقوط کرد.");
//            GameOfThrones game = attacker.getGame();
//            if (game != null) {
//                game.handlePlayerDefeat(defender);
//            }
//        } else {
//            System.out.println("🦸 مدافع قهرمان خود (" + defenderHero.getName() + ") را برای دفاع نهایی فرامی‌خواند!");
//            Hero attackerHero = (attacker.getKingdom() != null) ? attacker.getKingdom().getHero() : null;
//
//            if (attackerHero != null && attackerHero.isAvailable()) {
//                System.out.println("⚔️ قهرمان مهاجم (" + attackerHero.getName() + ") نیز قدم به میدان می‌گذارد! دوئل قهرمانان!");
//                boolean attackerWon = resolveHeroDuel(attackerHero, defenderHero);
//                if (attackerWon) {
//                    System.out.println("🏆 قهرمان مهاجم در دوئل پیروز شد! قلعه فتح شد!");
//                    if (attacker.getGame() != null) attacker.getGame().handlePlayerDefeat(defender);
//                } else {
//                    System.out.println("🛡️ قهرمان مدافع پیروز دوئل شد و حمله را دفع کرد! ارتش مهاجم شکست خورد.");
//                    applyDamageToUnits(troopsAttacker, Integer.MAX_VALUE);
//                }
//            } else {
//                System.out.println("🛡️ قهرمان مدافع به تنهایی در برابر ارتش باقی‌مانده مهاجم می‌ایستد!");
//                System.out.println("   > ارتش مهاجم از رویارویی با قهرمان تضعیف شده و با ۱۰٪ جان می‌جنگد!");
//                for (SoldierUnit unit : troopsAttacker) {
//                    if (unit != null && unit.getCount() > 0) {
//                        unit.applyHealthReductionPercentage(90);
//                    }
//                }
//                boolean attackerWon = resolveHeroVsArmy(defenderHero, troopsAttacker);
//                if (attackerWon) {
//                    System.out.println("🏆 ارتش مهاجم با وجود ضعف، بر قهرمان غلبه کرد! قلعه فتح شد!");
//                    if (attacker.getGame() != null) attacker.getGame().handlePlayerDefeat(defender);
//                } else {
//                    System.out.println("🛡️ قهرمان مدافع یک تنه کل ارتش باقی‌مانده را شکست داد! حمله دفع شد.");
//                    applyDamageToUnits(troopsAttacker, Integer.MAX_VALUE);
//                }
//            }
//        }
//    }
//
//    private void attackNormalBuilding(Building targetBuilding, Position attackerPosition) {
//        while (!targetBuilding.isDestroyed() && getOverallSoldierCount(troopsAttacker) > 0) {
//            TerrainEffect currentTurnEffect = getTerrainEffect(attackerPosition);
//            if (currentTurnEffect.getHealthReductionPercentage() > 0) {
//                int reductionPercent = currentTurnEffect.getHealthReductionPercentage();
//                System.out.println("   > شرایط سخت زمین " + reductionPercent + "% از جان سربازان می‌کاهد!");
//                for (SoldierUnit unit : troopsAttacker) {
//                    if (unit != null) unit.applyHealthReductionPercentage(reductionPercent);
//                }
//            }
//            if (getOverallSoldierCount(troopsAttacker) <= 0) break;
//
//            if (attackerPosition.equals(targetBuilding.getPosition())) {
//                int baseArmyAP = calculateRowBasedAttackPower(troopsAttacker);
//                int finalArmyAP = (int) (baseArmyAP * currentTurnEffect.getAttackModifier());
//                targetBuilding.takeDamage(finalArmyAP);
//            } else {
//                int baseArcherAP = calculateRowBasedAttackPower(getArcherUnits(troopsAttacker));
//                int finalArcherAP = (int) (baseArcherAP * currentTurnEffect.getAttackModifier());
//                if (finalArcherAP > 0) {
//                    targetBuilding.takeDamage(finalArcherAP);
//                }
//                attackerPosition = moveOneStepCloser(attackerPosition, targetBuilding.getPosition());
//            }
//
//            MyLinkedList<DefensiveTower> towersList = defend.getTowers();
//            if (towersList != null && !towersList.isEmpty()) {
//                int totalTowerAP = 0;
//                for (DefensiveTower tower : towersList) {
//                    if (calculateDistance(tower.getPosition(), attackerPosition) <= tower.getRange()) {
//                        totalTowerAP += tower.getAttackPower();
//                    }
//                }
//                if (totalTowerAP > 0) {
//                    applyDamageToUnits(troopsAttacker, totalTowerAP);
//                }
//            }
//        }
//
//        if (targetBuilding.isDestroyed()) {
//            System.out.println("🎉 ساختمان " + targetBuilding.getName() + " نابود شد!");
//            int newLoot = defender.getKingdom().processBuildingDestruction(targetBuilding, attacker.getKingdom());
//            if (newLoot > 0 && targetBuilding instanceof Storage) {
//                Storage storage = (Storage) targetBuilding;
//                switch (storage.getResourceType().toUpperCase()) {
//                    case "WOOD":
//                        totalWoodLoot += newLoot;
//                        break;
//                    case "STONE":
//                        totalStoneLoot += newLoot;
//                        break;
//                    case "FOOD":
//                        totalFoodLoot += newLoot;
//                        break;
//                    case "COIN":
//                        totalCoinLoot += newLoot;
//                        break;
//                }
//            }
//        }
//    }
//
//    private boolean resolveHeroDuel(Hero attackerHero, Hero defenderHero) {
//        System.out.println("   > دوئل آغاز شد: " + attackerHero.getName() + " (قدرت تهاجمی: " + attackerHero.getAttackBonus() +
//            ") در برابر " + defenderHero.getName() + " (قدرت تدافعی: " + defenderHero.getDefenseBonus() + ")");
//
//        attackerHero.markBusy(); //
//        defenderHero.markBusy(); //
//
//        int attackerFinalPower = attackerHero.getAttackBonus() + (int) (Math.random() * 150); //
//        int defenderFinalPower = defenderHero.getDefenseBonus() + (int) (Math.random() * 150); //
//
//        System.out.println("   > قدرت نهایی قهرمان مهاجم: " + attackerFinalPower);
//        System.out.println("   > قدرت نهایی قهرمان مدافع: " + defenderFinalPower);
//
//        return attackerFinalPower > defenderFinalPower;
//    }
//
//    private boolean resolveHeroVsArmy(Hero defenderHero, SoldierUnit[] attackingArmy) {
//        System.out.println("   > " + defenderHero.getName() + " با قدرت دفاعی پایه " + defenderHero.getDefenseBonus() + " در برابر ارتش مهاجم می‌ایستد."); //
//
//        defenderHero.markBusy(); //
//
//        int armyTotalAttackPower = calculateRowBasedAttackPower(attackingArmy);
//        System.out.println("   > قدرت تهاجمی کل ارتش تضعیف شده: " + armyTotalAttackPower);
//
//        final int BALANCE_FACTOR = 3;
//        int heroFinalDefensePower = defenderHero.getDefenseBonus() * BALANCE_FACTOR;
//        System.out.println("   > قدرت دفاعی نهایی قهرمان (با ضریب تعادل " + BALANCE_FACTOR + "): " + heroFinalDefensePower);
//
//        return armyTotalAttackPower > heroFinalDefensePower;
//    }
//
//    private Building findClosestUndestroyedBuilding(Position currentPos, MyLinkedList<Building> buildings) {
//        Building closest = null;
//        int minDistance = Integer.MAX_VALUE;
//
//        Building castle = null;
//        int otherBuildingsCount = 0;
//
//        for (Building building : buildings) {
//            if (building != null && !building.isDestroyed()) {
//                if (building instanceof Castle) {
//                    castle = building;
//                } else {
//                    otherBuildingsCount++;
//                }
//            }
//        }
//
//        if (otherBuildingsCount > 0) {
//            for (Building building : buildings) {
//                if (building != null && !building.isDestroyed() && !(building instanceof Castle)) {
//                    int distance = currentPos.distance(building.getPosition());
//                    if (distance < minDistance) {
//                        minDistance = distance;
//                        closest = building;
//                    }
//                }
//            }
//            return closest;
//        } else {
//            return castle;
//        }
//    }
//
//    private int calculateDistance(Position a, Position b) {
//        int dx = Math.abs(a.getX() - b.getX());
//        int dy = Math.abs(a.getY() - b.getY());
//        return Math.max(dx, dy);
//    }
//
//
//    public int calculateStars() {
//        int stars = 0;
//        if (soldiersDefeated) stars++;
//        if (complexBreached) stars++;
//        if (allBuildingsDestroyed) stars++;
//        return stars;
//    }
//
//
//    private boolean hasUndestroyedBuildings(MyLinkedList<Building> allBuildings) {
//        Node<Building> current = allBuildings.getHead();
//        while (current != null) {
//            Building building = current.data;
//            if (building != null && !building.isDestroyed() && !(building instanceof Gate) && !(building instanceof WallsOfGate) && !(building instanceof Port)) {
//                return true;
//            }
//            current = current.next;
//        }
//        return false;
//    }
//
//    private void setAmbush() {
//        if (defender != null && defender.getKingdom() != null && defender.getKingdom().hasSpy()) {
//            this.ambush = false;
//            System.out.println("جاسوس مدافع، حمله را شناسایی کرد! کمین لغو شد.");
//            defender.getKingdom().getArmyManager().getDefenceManager().getDefenders().remove(defender.getKingdom().getSpy());
//        } else {
//            this.ambush = true;
//        }
//    }
//
//    public boolean isReadyToAttack() {
//        return Duration.between(startTime, LocalDateTime.now()).getSeconds() >= durationSeconds;
//    }
//
//    public boolean isAmbush() {
//        return this.ambush;
//    }
//
//    private int getOverallSoldierCount(SoldierUnit[] units) {
//        if (units == null) return 0;
//        int count = 0;
//        for (SoldierUnit unit : units) {
//            if (unit != null) count += unit.getCount();
//        }
//        return count;
//    }
//
//    private int getOverallSoldierCount(MyLinkedList<SoldierUnit> units) {
//        if (units == null) return 0;
//        int count = 0;
//        Node<SoldierUnit> current = units.getHead();
//        while (current != null) {
//            if (current.data != null) count += current.data.getCount();
//            current = current.next;
//        }
//        return count;
//    }
//
//    protected boolean soldierVsSoldierCombat(boolean isSurpriseAttack) {
//        MyLinkedList<SoldierUnit> defenderUnits = defend.getDefenders();
//
//        // محافظت در برابر null (فقط ایمنی بیشتر، اگر لازم نیست می‌تونی حذفش کنی)
//        if (defenderUnits == null) {
//            System.out.println("ERROR: defenderUnits is null!");
//            getReplayLog().add(new EventTextLog("ERROR: defenderUnits is null!"));
//            return false;
//        }
//
//        while (getOverallSoldierCount(troopsAttacker) > 0 && getOverallSoldierCount(defenderUnits) > 0) {
//
//            // اعلام شروع راند (کنسول و ریپلی)
//            System.out.println("\n--- دور جدید نبرد سرباز با سرباز ---");
//            getReplayLog().add(new EventTextLog("--- دور جدید نبرد سرباز با سرباز ---"));
//
//            // قدرت مهاجم (با مکان‌های ردیفی مثل قبل)
//            int attackerTotalAP = calculateRowBasedAttackPower(troopsAttacker);
//
//            // ساخت آرایه از مدافعان برای محاسبات
//            int defenderSize = defenderUnits.size();
//            SoldierUnit[] defenderArray = new SoldierUnit[defenderSize];
//            defenderUnits.fillArray(defenderArray);
//
//            // محاسبه قدرت کل مدافعان
//            int defenderTotalAP = calculateTotalAttackPower(defenderArray);
//
//            if (isSurpriseAttack) {
//                // نیم‌کردن قدرت مدافعان (فرکتشن را گرد کن به پایین)
//                defenderTotalAP = (int) Math.floor(defenderTotalAP * 0.5);
//                System.out.println("  > مدافع غافلگیر شده و با نصف قدرت می‌جنگد!");
//                getReplayLog().add(new EventTextLog("مدافع غافلگیر شده و با نصف قدرت می‌جنگد!"));
//            }
//
//            // پیام‌های معمولی (کنسول)
//            System.out.println("  > مهاجم " + attackerTotalAP + " آسیب وارد می‌کند.");
//            System.out.println("  > مدافع " + defenderTotalAP + " آسیب وارد می‌کند.");
//
//            // همین پیام‌ها رو توی ریپلی هم ثبت کن
//            getReplayLog().add(new EventTextLog("مهاجم " + attackerTotalAP + " آسیب وارد می‌کند."));
//            getReplayLog().add(new EventTextLog("مدافع " + defenderTotalAP + " آسیب وارد می‌کند."));
//
//            // اعمال آسیب‌ها به یونیت‌ها (وظیفهٔ همین متد بود)
//            applyDamageToUnits(defenderUnits, attackerTotalAP);
//            applyDamageToUnits(troopsAttacker, defenderTotalAP);
//
//            // بعد از اعمال آسیب، وضعیت سلامت کل هر طرف را بگیر (برای نوار بالای صفحه)
//            long attackerTotalHealth = getOverallHealth(troopsAttacker);       // فرض: این متد موجود است و با آرایه کار می‌کند
//            long defenderTotalHealth = getOverallHealth(defenderArray);       // استفاده از آرایه‌ای که ساختیم
//
//            // اضافه کردن EventRoundUpdate برای آپدیت health bars و لاگ پایین
//            getReplayLog().add(new EventRoundUpdate(
//                "پایان دور: وضعیت نیروها به‌روز شد.",
//                attackerTotalHealth,
//                defenderTotalHealth
//            ));
//
//            // پرینت وضعیت پایانی راند (کنسول) — مثل قبل
//            System.out.println("  > وضعیت پایان دور: [مهاجمان: " + getOverallSoldierCount(troopsAttacker) +
//                "], [مدافعان: " + getOverallSoldierCount(defenderUnits) + "]");
//            // و همین پیام را نیز در ریپلی ثبت کن
//            getReplayLog().add(new EventTextLog("پایان دور: مهاجمان=" + getOverallSoldierCount(troopsAttacker) +
//                " | مدافعان=" + getOverallSoldierCount(defenderUnits)));
//        }
//
//        // پایان نبرد — پیام‌ها و Event نهایی
//        boolean attackerSurvived = getOverallSoldierCount(troopsAttacker) > 0;
//
//        if (!attackerSurvived) {
//            System.out.println("\n⚔️ حمله شکست خورد. تمام نیروهای مهاجم نابود شدند.");
//            getReplayLog().add(new EventTextLog("⚔️ حمله شکست خورد. تمام نیروهای مهاجم نابود شدند."));
//            // EventRoundUpdate نهایی برای نشان دادن صفر شدن سلامتی مهاجم
//            getReplayLog().add(new EventRoundUpdate("پایان نبرد: مهاجمان نابود شدند.",
//                getOverallHealth(troopsAttacker),
//                getOverallHealth(defenderUnits.size() > 0 ? defenderUnits.toArray(new SoldierUnit[defenderUnits.size()]) : new SoldierUnit[0])));
//            return false;
//        }
//
//        // اگر مدافعان تمام شده‌اند
//        if (getOverallSoldierCount(defenderUnits) <= 0) {
//            System.out.println("\n🎉 مدافعان شکست خوردند. مهاجمان پیروز شدند!");
//            getReplayLog().add(new EventTextLog("🎉 مدافعان شکست خوردند. مهاجمان پیروز شدند!"));
//            getReplayLog().add(new EventRoundUpdate("پایان نبرد: مدافعان شکست خوردند.",
//                getOverallHealth(troopsAttacker),
//                getOverallHealth(defenderUnits.size() > 0 ? defenderUnits.toArray(new SoldierUnit[defenderUnits.size()]) : new SoldierUnit[0])));
//            return true;
//        }
//
//        // حالت پیش‌بینی‌نشده — ولی مطابق نسخهٔ قبلی، مقدار بقای مهاجم را برگردان
//        System.out.println("\n⚔️ نبرد پایان یافت.");
//        getReplayLog().add(new EventTextLog("⚔️ نبرد پایان یافت."));
//        return attackerSurvived;
//    }
//
//
//    protected int calculateRowBasedAttackPower(SoldierUnit[] units) {
//        double totalPower = 0;
//        int currentRow = 0;
//        for (SoldierUnit unit : units) {
//            if (unit != null && unit.getCount() > 0) {
//                totalPower += unit.getTotalAttackPower() * Math.pow(SoldierEfficiency, currentRow);
//                currentRow++;
//            }
//        }
//        return (int) totalPower;
//    }
//
//    private int calculateTotalAttackPower(DefensiveTower[] towers) {
//        int totalPower = 0;
//        for (DefensiveTower tower : towers) {
//            if (tower != null && !tower.isDestroyed()) {
//                totalPower += tower.getAttackPower();
//            }
//        }
//        return totalPower;
//    }
//
//    private int calculateTotalAttackPower(SoldierUnit[] units) {
//        int totalPower = 0;
//        for (SoldierUnit unit : units) {
//            if (unit != null && unit.getCount() > 0) {
//                totalPower += unit.getTotalAttackPower();
//            }
//        }
//        return totalPower;
//    }
//
//    private void applyDamageToUnits(SoldierUnit[] units, int totalDamage) {
//        if (units == null || totalDamage <= 0) return;
//        for (int i = 0; i < units.length; i++) {
//            if (totalDamage <= 0) break;
//            SoldierUnit unit = units[i];
//            if (unit != null && unit.getCount() > 0) {
//                totalDamage = unit.getTotalDamage(totalDamage);
//                if (unit.getCount() == 0) units[i] = null;
//            }
//        }
//    }
//
//    private void applyDamageToUnits(MyLinkedList<SoldierUnit> units, int totalDamage) {
//        if (units == null || totalDamage <= 0) return;
//        Node<SoldierUnit> currentUnitNode = units.getHead();
//        while (totalDamage > 0 && currentUnitNode != null) {
//            SoldierUnit unit = currentUnitNode.data;
//            Node<SoldierUnit> nextNode = currentUnitNode.next;
//            if (unit != null) {
//                totalDamage = unit.getTotalDamage(totalDamage);
//                if (unit.getCount() == 0) units.remove(unit);
//            }
//            currentUnitNode = nextNode;
//        }
//    }
//
//    private SoldierUnit[] getArcherUnits(SoldierUnit[] allUnits) {
//        if (allUnits == null) return new SoldierUnit[0];
//        List<SoldierUnit> archers = new ArrayList<>();
//        for (SoldierUnit unit : allUnits) {
//            if (unit != null && unit.getType() == SoldierType.ARCHER) {
//                archers.add(unit);
//            }
//        }
//        return archers.toArray(new SoldierUnit[0]);
//    }
//
//    protected MyLinkedList<Building> collectAllBuildingsFromMap(UserSubMap map) {
//        MyLinkedList<Building> buildings = new MyLinkedList<>();
//        if (map == null || map.mainMap == null || map.mainMap.getMap() == null) return buildings;
//        for (int i = map.getStartX(); i < map.getStartX() + map.getWidth(); i++) {
//            for (int j = map.getStartY(); j < map.getStartY() + map.getLength(); j++) {
//                if (map.mainMap.getMap()[i][j] != null && map.mainMap.getMap()[i][j].getObjectOnTile() != null && map.mainMap.getMap()[i][j].getObjectOnTile().getBuilding() != null) {
//                    buildings.add(map.mainMap.getMap()[i][j].getObjectOnTile().getBuilding());
//                }
//            }
//        }
//        return buildings;
//    }
//
//    private Position moveOneStepCloser(Position current, Position target) {
//        int dx = Integer.signum(target.getX() - current.getX());
//        int dy = Integer.signum(target.getY() - current.getY());
//        if (dx != 0 && Math.abs(current.getX() - target.getX()) >= Math.abs(current.getY() - target.getY())) {
//            return new Position(current.getX() + dx, current.getY());
//        } else if (dy != 0) {
//            return new Position(current.getX(), current.getY() + dy);
//        } else if (dx != 0) {
//            return new Position(current.getX() + dx, current.getY());
//        }
//        return current;
//    }
//
//    public boolean isCompleted() {
//        return this.completed;
//    }
//
//    public void markCompleted() {
//        this.completed = true;
//        if (getDefender() != null) {
//            getDefender().setInFight(false);
//        }
//    }
//
//    public GameUser getAttacker() {
//        return attacker;
//    }
//
//    public GameUser getDefender() {
//        return defender;
//    }
//
//    public LocalDateTime getStartTime() {
//        return startTime;
//    }
//
//    public SoldierUnit[] getAttackerTroops() {
//        return troopsAttacker;
//    }
//
//    private void destroyAllBuildings(UserSubMap map) {
//        MyLinkedList<Building> buildings = collectAllBuildingsFromMap(map);
//        for (Building b : buildings) {
//            if (b != null && !b.isDestroyed()) {
//                b.setStatus(Status.destroyed);
//            }
//        }
//        System.out.println("🔥 تمام ساختمان‌های اقلیم نابود شدند.");
//    }
//

//
//    private void displayLootSummary() {
//        System.out.println("\n--- 💰 گزارش نهایی غنایم جنگی 💰 ---");
//        System.out.println("  - چوب غارت شده: " + totalWoodLoot);
//        System.out.println("  - سنگ غارت شده: " + totalStoneLoot);
//        System.out.println("  - غذا غارت شده: " + totalFoodLoot);
//        System.out.println("  - سکه غارت شده: " + totalCoinLoot);
//        System.out.println("------------------------------------");
//    }
//
//    public int getInitialDuration() {
//        return this.durationSeconds;
//    }
//    public int getElapsedTime() {
//        if (this.startTime == null) {
//            return 0;
//        }
//        long elapsed = Duration.between(this.startTime, LocalDateTime.now()).getSeconds();
//        return (int) elapsed;
//    }
//
//    public int getRemainingTime() {
//        int elapsed = getElapsedTime();
//        int remaining = getInitialDuration() - elapsed;
//        return Math.max(0, remaining);
//    }
//    private TerrainEffect getTerrainEffect(Position position) {
//        TerrainType terrainType = defender.getKingdom().getUserMap().mainMap.mainMap[position.getX()][position.getY()].terrainType;
//
//        switch (terrainType) {
//            case MOUNTAIN:
//                System.out.println("   > مبارزه در زمین کوهستانی! قدرت حمله ۱۰٪ کاهش یافت.");
//                return new TerrainEffect(0.9, 0);
//
//            case SNOW:
//                System.out.println("   > مبارزه در زمین برفی! قدرت حمله ۲۰٪ و جان ۵٪ در هر نوبت کاهش می‌یابد.");
//                return new TerrainEffect(0.8, 5);
//
//
//            default:
//                return new TerrainEffect(1.0, 0);
//        }
//    }
//
//}

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
            boolean defenderHeroDefeated = (defender.getKingdom().getHero() == null || !defender.getKingdom().getHero().isAvailable());

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
        }catch (Exception e) {
            e.printStackTrace();
            getReplayLog().add(new EventTextLog("CRITICAL ERROR: Battle aborted."));
        }

        displayLootSummary();
        return true;
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

        return getOverallSoldierCount(troopsAttacker) > 0;
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
//    private void handleCastleAssault(Castle castle) {
//        Hero defHero = defender.getKingdom().getHero();
//        Hero atkHero = attacker.getKingdom().getHero();
//
//        if (defHero != null && defHero.isAvailable()) {
//            System.out.println("Hero defending castle!");
//            if (atkHero != null && atkHero.isAvailable()) {
//                getReplayLog().add(new EventHeroDuel(atkHero.getName(), defHero.getName(), false));
//                boolean atkWon = (atkHero.getAttackBonus() + new Random().nextInt(100)) > (defHero.getDefenseBonus() + new Random().nextInt(100));
//
//                getReplayLog().add(new EventHeroDuel(atkHero.getName(), defHero.getName(), atkWon));
//
//                if (atkWon) {
//                    getReplayLog().add(new EventTextLog("Attacker Hero Won! Castle Fallen."));
//                    attacker.getGame().handlePlayerDefeat(defender);
//                } else {
//                    getReplayLog().add(new EventTextLog("Defender Hero Won! Attack Repelled."));
//                    applyDamageToUnits(troopsAttacker, 999999); // Wipeout
//                }
//            } else {
//                getReplayLog().add(new EventTextLog("Defender Hero stands alone against the army!"));
//                int armyPower = calculateRowBasedAttackPower(troopsAttacker);
//                int heroPower = defHero.getDefenseBonus() * 3; // Hero buff
//
//                if (armyPower > heroPower) {
//                    getReplayLog().add(new EventTextLog("Army overwhelmed the Hero! Castle Fallen."));
//                    attacker.getGame().handlePlayerDefeat(defender);
//                } else {
//                    getReplayLog().add(new EventTextLog("Hero repelled the army!"));
//                    applyDamageToUnits(troopsAttacker, 999999);
//                }
//            }
//        } else {
//            getReplayLog().add(new EventTextLog("Castle undefended! Captured."));
//            attacker.getGame().handlePlayerDefeat(defender);
//        }
//    }

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
        if (k != null && k.getHero() != null && k.getHero().isAvailable()) {
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
            atkPower += heroAttackBonus;

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
            defPower += heroDefenseBonus;

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
