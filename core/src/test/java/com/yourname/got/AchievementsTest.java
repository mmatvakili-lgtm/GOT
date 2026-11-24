package com.yourname.got;

import com.yourname.got.logic.system.Achievements;
import com.yourname.got.logic.system.Leaderboard;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;

/** Achievements follow the leaderboard numbers. */
class AchievementsTest {
    @Test
    void unlockedFromLeaderboardNumbers() throws Exception {
        TestWorld.reset(true);
        new File("leaderboard.txt").delete();
        Check.that(Achievements.unlocked(null).isEmpty(), "nothing for an unknown player");
        Check.that(Achievements.find("zed") == null, "unknown player is not found");
        Leaderboard.record("Zed", 1200, true);
        Leaderboard.Entry e = Achievements.find("zed");
        Check.that(e != null, "found ignoring case");
        int n = Achievements.unlocked(e).size();
        Check.that(n == 3, "first steps + victor + rising power (" + n + ")");
        for (int i = 0; i < 9; i++) Leaderboard.record("Zed", 6000, true);
        n = Achievements.unlocked(Achievements.find("Zed")).size();
        Check.that(n == 6, "all six after 10 wins and a 6000 score (" + n + ")");
        Check.assertAll();
        new File("leaderboard.txt").delete();
    }
}
