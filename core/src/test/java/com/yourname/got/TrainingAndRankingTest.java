package com.yourname.got;

import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import com.yourname.got.logic.login.*;
import com.yourname.got.logic.model.*;
import com.yourname.got.logic.enums.*;
import com.yourname.got.logic.model.Army.*;
import com.yourname.got.logic.model.Building.*;
import java.io.*;

class TrainingAndRankingTest {
    static void check(boolean c, String m) { Check.that(c, m); }
    static GameRecord readRecord(int uid, String game) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("users/user_" + uid + "/Record_" + game + ".ser"))) {
            return (GameRecord) in.readObject();
        }
    }
    static String dump(GameRecord r) {
        StringBuilder sb = new StringBuilder();
        for (PlayerResult p : r.getPlayerResults()) sb.append(p.getFinalRank()).append(":").append(p.getUsername()).append("(").append(p.getStatus()).append(",").append(p.getScore()).append(") ");
        return sb.toString();
    }

    @Test
    void queueRefundAndFinalRanking() throws Exception {
        TestWorld.reset(true);
        GameUser u1 = new GameUser("alice", "1234", 1), u2 = new GameUser("bob", "1234", 2), u3 = new GameUser("carol", "1234", 3);
        GameOfThrones g = new GameOfThrones("last", 1, u1);
        g.addPlayer(u2); g.addPlayer(u3); g.startGame();

        System.out.println("=== BARRACK QUEUE + REFUND ===");
        Kingdom k = u1.getKingdom();
        Camp c = new Camp(k); c.markAsBuilt(); k.getBuildings().add(c);          // capacity +20
        Barrack b = new Barrack(k); b.markAsBuilt(); b.setStatus(com.yourname.got.logic.enums.Status.stable); k.getBuildings().add(b);
        for (int i = 0; i < 10; i++) k.getArmyManager().recruit(new Infantry());   // 10 / 20 used
        for (String res : new String[]{"Wood", "Coin", "Food"}) { Storage st = new Storage(res, k); st.markAsBuilt(); k.addStorage(st); k.getBuildings().add(st); }
        k.addResources("Wood", 100000); k.addResources("Coin", 100000); k.addResources("Food", 100000);
        System.out.println("wood=" + k.getTotalResources("Wood") + " coin=" + k.getTotalResources("Coin") + " food=" + k.getTotalResources("Food") + " cap=" + k.getTotalSoldierCapacity());
        int free = k.getFreeSoldierSlots();
        int queued = 0;
        while (b.trainSoldier(SoldierType.INFANTRY, 1)) { queued++; if (queued > 100) break; }
        check(queued == free, "barrack queues exactly the free slots (" + queued + " of " + free + ")");
        check(k.getTrainingQueue().getSize() == queued, "queue size matches");
        // someone else (e.g. buying mercenaries) fills the camp meanwhile -> queued soldiers cannot be deployed
        for (int i = 0; i < 3; i++) k.getArmyManager().recruit(new Infantry());
        int wood0 = k.getTotalResources("Wood"), coin0 = k.getTotalResources("Coin"), food0 = k.getTotalResources("Food");
        // drain the 'free' room first so the next training truly has no space
        while (k.getFreeSoldierSlots() > 0) k.getArmyManager().recruit(new Infantry());
        TrainingTask head = k.getTrainingQueue().getHead().data;
        boolean res = head.finishTraining().isSuccess();
        check(!res, "queued soldier is refused when camps got full in the meantime");
        check(k.getTotalResources("Wood") == wood0 + SoldierType.INFANTRY.woodCost
           && k.getTotalResources("Coin") == coin0 + SoldierType.INFANTRY.coinCost
           && k.getTotalResources("Food") == food0 + SoldierType.INFANTRY.foodCost,
           "resources of that soldier were refunded exactly");

        System.out.println("=== LAST PLAYER STANDING ranking ===");
        g.handlePlayerDefeat(u3);   // carol falls first
        g.handlePlayerDefeat(u2);   // bob falls second
        g.checkGameOver();
        check(g.isFinished(), "game ends when one player is left");
        GameRecord rec = readRecord(1, "last");
        System.out.println("   " + dump(rec));
        PlayerResult[] rs = new PlayerResult[3]; int i = 0; for (PlayerResult p : rec.getPlayerResults()) rs[i++] = p;
        check(rs[0].getUsername().equals("alice") && rs[0].getStatus() == PlayerResult.PlayerStatus.WINNER && rs[0].getFinalRank() == 1, "alice = WINNER rank 1");
        check(rs[1].getUsername().equals("bob") && rs[1].getFinalRank() == 2 && rs[1].getStatus() == PlayerResult.PlayerStatus.DEFEATED, "bob (fell last) = rank 2");
        check(rs[2].getUsername().equals("carol") && rs[2].getFinalRank() == 3, "carol (fell first) = rank 3");
        check(rs[0].getScore() > 0, "winner has a real score (" + rs[0].getScore() + ")");

        System.out.println("=== SURVIVED THE LONG NIGHT ranking by score ===");
        GameUser v1 = new GameUser("dave", "1234", 4), v2 = new GameUser("erin", "1234", 5), v3 = new GameUser("frank", "1234", 6);
        new File("users/user_4").mkdirs(); new File("users/user_5").mkdirs(); new File("users/user_6").mkdirs(); new File("GAMES/game_3").mkdirs();
        GameOfThrones g2 = new GameOfThrones("night", 1, v1);
        g2.addPlayer(v2); g2.addPlayer(v3); g2.startGame();
        for (int n = 0; n < 5; n++) v2.getKingdom().getArmyManager().recruit(new Infantry());     // erin: some soldiers
        for (int n = 0; n < 30; n++) v3.getKingdom().getArmyManager().recruit(new Infantry());    // frank: many soldiers
        g2.endGame(GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT);
        GameRecord rec2 = readRecord(4, "night");
        System.out.println("   " + dump(rec2));
        PlayerResult[] r2 = new PlayerResult[3]; i = 0; for (PlayerResult p : rec2.getPlayerResults()) r2[i++] = p;
        check(r2[0].getUsername().equals("frank") && r2[1].getUsername().equals("erin") && r2[2].getUsername().equals("dave"), "ranked by score: frank > erin > dave");
        check(r2[0].getScore() > r2[1].getScore() && r2[1].getScore() > r2[2].getScore(), "scores strictly descending");
        check(r2[0].getStatus() == PlayerResult.PlayerStatus.SURVIVED && r2[0].getFinalRank() == 1 && r2[2].getFinalRank() == 3, "all SURVIVED with ranks 1..3");
        Check.assertAll();
    }
}
