package com.yourname.got.logic.WhiteWalker;


import com.yourname.got.logic.util.GameLog;
import com.yourname.got.logic.login.BattleLogger;
import com.yourname.got.logic.login.GameFileUtility;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.Army.SoldierUnit;
import com.yourname.got.logic.model.Building.Building;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.model.Kingdom;
import com.yourname.got.logic.model.Mission;
import com.yourname.got.logic.util.EventTextLog;

import java.io.Serializable;

public class WhiteWalkerMission extends Mission implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean finalWave;
    private int day;

    public WhiteWalkerMission(GameUser defender, SoldierUnit[] attackers) {
        this(defender, attackers, 0, false, 0);
    }

    /** The dead need {@code marchSeconds} to arrive, so the player sees them coming. */
    public WhiteWalkerMission(GameUser defender, SoldierUnit[] attackers, int marchSeconds, boolean finalWave, int day) {
        super(null, defender, attackers, marchSeconds, com.yourname.got.logic.enums.AttackType.WHITEWALKER);
        this.ambush = false; // WWs never ambush
        this.finalWave = finalWave;
        this.day = day;
    }

    private static int count(SoldierUnit[] units) {
        int n = 0;
        if (units != null) for (SoldierUnit u : units) if (u != null) n += u.getCount();
        return n;
    }

    private static int defenderCount(Kingdom k) {
        int n = 0;
        if (k == null) return 0;
        for (SoldierUnit u : k.getArmyManager().getDefenceManager().getDefenders()) if (u != null) n += u.getCount();
        return n;
    }

    @Override
    public Boolean startAttack() {
        GameUser defUser = this.getDefender();
        if (defUser == null) {
            this.markCompleted();
            return true;
        }
        GameOfThrones game = defUser.getGame();

        if (game != null) {
            GameOfThrones.PlayerState state = game.getPlayerState(defUser.getId());
            if (state == null || state.status == GameOfThrones.PlayerStatus.DEFEATED) {
                GameLog.info("❄️ [WWMission] Attack cancelled: " + defUser.getUsername() + " is already defeated.");
                this.markCompleted();
                return true;
            }
        }

        GameLog.info("\n❄️ [WWMission] White Walker battle starting against " + defUser.getKingdom().getType() + "...");

        getReplayLog().add(new EventTextLog((finalWave ? "THE LONG NIGHT HAS COME! " : "The dead march from beyond the Wall. ")
            + "The White Walkers attack " + defUser.getKingdom().getType() + "!"));
        getReplayLog().add(new EventTextLog("Against you: " + WhiteWalkerWave.describe(WhiteWalkerWave.census(getTroopsAttacker())) + "."));

        Kingdom kingdom = defUser.getKingdom();
        String enemies = WhiteWalkerWave.describe(WhiteWalkerWave.census(getTroopsAttacker()));
        int enemiesBefore = count(getTroopsAttacker());
        int defendersBefore = defenderCount(kingdom);
        int glassBefore = kingdom.getDragonGlass();

        boolean whiteWalkersWon = this.soldierVsSoldierCombatAdvanced();

        AssaultReport report = new AssaultReport(day, finalWave, defUser.getId(), kingdom.getType().name(), enemies,
            enemiesBefore, count(getTroopsAttacker()), defendersBefore, defenderCount(kingdom),
            Math.max(0, glassBefore - kingdom.getDragonGlass()), whiteWalkersWon);

        if (whiteWalkersWon) {
            GameLog.info("...💀 The armies of " + defUser.getKingdom().getType() + " have fallen. The kingdom is overrun.");
            getReplayLog().add(new EventTextLog("The defenders have fallen!"));
            if (game != null) {
                game.handlePlayerDefeat(defUser);
            }
        } else {
            GameLog.info("✅ The armies of " + defUser.getKingdom().getType() + " have successfully repelled the White Walkers!");
            getReplayLog().add(new EventTextLog("VICTORY! The White Walkers were repelled!"));
        }

        int defenderStars = whiteWalkersWon ? 0 : 3;
        String replayPath = BattleLogger.saveReplayLog(this.getReplayLog(), defUser.getGame(),this);

        BattleLogger.logBattle(
            this,
            3 - defenderStars,
            game.getBATTLE_LOG_FILE(),
            replayPath,
            0, 0, 0, 0
        );

        GameLog.info("\n> Reconciling defender's army...");
        defUser.getKingdom().getArmyManager().reconcileAllSoldiers();
        defUser.getKingdom().getArmyManager().healAllSoldiers();

        defUser.setInFight(false);
        this.markCompleted();

        GameFileUtility.saveFullGameState(game);
        try {
            if (com.yourname.got.mediator.GameMediator.getInstance() != null) {
                com.yourname.got.mediator.GameMediator.getInstance().triggerAssaultReport(report);
            }
        } catch (RuntimeException e) {
            GameLog.error("White Walker report could not be shown: " + e);
        }
        return true;
    }

    private void destroyAllKingdomBuildings(Kingdom kingdom) {
        if (kingdom == null || kingdom.getBuildings() == null) return;

        GameLog.info("🔥 A great winter falls... All buildings in " + kingdom.getType() + " have been destroyed.");
        for (Building building : kingdom.getBuildings()) {
            if (building != null && !building.isDestroyed()) {
                kingdom.processBuildingDestruction(building, null);
            }
        }
    }
}


