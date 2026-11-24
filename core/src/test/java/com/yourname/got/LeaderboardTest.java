package com.yourname.got;

import com.yourname.got.logic.system.Leaderboard;
import com.yourname.got.support.Check;
import com.yourname.got.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** The all-time leaderboard accumulates, sorts, and survives damaged lines. */
class LeaderboardTest {
    @Test
    void accumulatesAndSorts() throws Exception {
        TestWorld.reset(true);
        new File("leaderboard.txt").delete();
        Leaderboard.record("alice", 500, true);
        Leaderboard.record("Bob", 900, false);
        Leaderboard.record("ALICE", 300, false);
        List<Leaderboard.Entry> top = Leaderboard.top(10);
        Check.that(top.size() == 2, "names are merged ignoring case (" + top.size() + ")");
        Check.that(top.get(0).name.equals("alice"), "most wins first");
        Check.that(top.get(0).games == 2 && top.get(0).wins == 1, "games and wins are counted");
        Check.that(top.get(0).bestScore == 500 && top.get(0).totalScore == 800, "best and total scores");
        Check.that(Leaderboard.top(1).size() == 1, "limit is respected");
        Check.assertAll();
        new File("leaderboard.txt").delete();
    }

    @Test
    void damagedLinesAreSkipped() throws Exception {
        TestWorld.reset(true);
        Files.writeString(new File("leaderboard.txt").toPath(), "garbage\ncarl\t1\t1\t10\t10\nbad\tx\t1\t1\t1\n");
        List<Leaderboard.Entry> top = Leaderboard.top(10);
        Check.that(top.size() == 1 && top.get(0).name.equals("carl"), "only the valid line is kept");
        Leaderboard.record("dan", 5, false);
        Check.that(Leaderboard.top(10).size() == 2, "recording still works after a damaged file");
        Check.assertAll();
        new File("leaderboard.txt").delete();
    }
}
