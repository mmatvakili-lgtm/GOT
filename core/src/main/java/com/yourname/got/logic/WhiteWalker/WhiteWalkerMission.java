package com.yourname.got.logic.WhiteWalker;

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

    public WhiteWalkerMission(GameUser defender, SoldierUnit[] attackers) {
        super(null, defender, attackers, 0, com.yourname.got.logic.enums.AttackType.WHITEWALKER);
        this.ambush = false; // WWs never ambush
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
                System.out.println("❄️ [WWMission] Attack cancelled: " + defUser.getUsername() + " is already defeated.");
                this.markCompleted();
                return true;
            }
        }

        System.out.println("\n❄️ [WWMission] White Walker battle starting against " + defUser.getKingdom().getType() + "...");

        getReplayLog().add(new EventTextLog("The White Walkers attack " + defUser.getKingdom().getType() + "!"));

        boolean whiteWalkersWon = this.soldierVsSoldierCombatAdvanced();

        if (whiteWalkersWon) {
            System.out.println("...💀 The armies of " + defUser.getKingdom().getType() + " have fallen. The kingdom is overrun.");
            getReplayLog().add(new EventTextLog("The defenders have fallen!"));
            if (game != null) {
                game.handlePlayerDefeat(defUser);
            }
        } else {
            System.out.println("✅ The armies of " + defUser.getKingdom().getType() + " have successfully repelled the White Walkers!");
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

        System.out.println("\n> Reconciling defender's army...");
        defUser.getKingdom().getArmyManager().reconcileAllSoldiers();

        defUser.setInFight(false);
        this.markCompleted();

        GameFileUtility.saveFullGameState(game);
        return true;
    }

    private void destroyAllKingdomBuildings(Kingdom kingdom) {
        if (kingdom == null || kingdom.getBuildings() == null) return;

        System.out.println("🔥 A great winter falls... All buildings in " + kingdom.getType() + " have been destroyed.");
        for (Building building : kingdom.getBuildings()) {
            if (building != null && !building.isDestroyed()) {
                kingdom.processBuildingDestruction(building, null);
            }
        }
    }
}


