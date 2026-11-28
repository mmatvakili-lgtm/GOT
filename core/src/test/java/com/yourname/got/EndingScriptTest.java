package com.yourname.got;

import com.yourname.got.logic.login.PlayerResult;
import com.yourname.got.logic.model.GameRecord;
import com.yourname.got.logic.system.EndingScript;
import com.yourname.got.support.Check;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Every ending exists for every kingdom, is personal and has a sensible length. */
class EndingScriptTest {
    @Test
    void everyKingdomHasEveryEnding() {
        String[] kingdoms = {"WINTERFELL", "EYRIE", "IRON_ISLANDS", "CASTERLY_ROCK", "STORMS_END", "HIGHGARDEN", "DRAGONSTONE"};
        String[] words = {"Winter Is Coming", "As High as Honor", "We Do Not Sow", "Hear Me Roar", "Ours Is the Fury", "Growing Strong", "Fire and Blood"};
        for (int i = 0; i < kingdoms.length; i++) {
            for (EndingScript.Kind kind : EndingScript.Kind.values()) {
                List<EndingScript.Shot> shots = EndingScript.build(kind, kingdoms[i], "alice", 2, 1, 3);
                Check.that(shots.size() >= 4, kingdoms[i] + "/" + kind + " has several shots");
                float total = EndingScript.totalSeconds(shots);
                Check.that(total >= 20f && total <= 60f, kingdoms[i] + "/" + kind + " lasts a sensible time (" + total + "s)");
                boolean hasWords = false;
                for (EndingScript.Shot s : shots) {
                    Check.that(s.seconds > 0 && s.text != null && !s.text.isEmpty() && s.title != null, "shot is complete");
                    if (s.title.toUpperCase().contains(words[i].toUpperCase())) hasWords = true;
                }
                Check.that(hasWords, kingdoms[i] + "/" + kind + " ends with the house words");
            }
        }
        Check.assertAll();
    }

    @Test
    void endingMatchesHowTheGameEnded() {
        Check.that(EndingScript.kindFor(GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT, PlayerResult.PlayerStatus.SURVIVED) == EndingScript.Kind.DAWN, "survivor sees the dawn");
        Check.that(EndingScript.kindFor(GameRecord.GameResult.SURVIVED_THE_LONG_NIGHT, PlayerResult.PlayerStatus.DEFEATED) == EndingScript.Kind.FALLEN, "fallen player sees the fall");
        Check.that(EndingScript.kindFor(GameRecord.GameResult.WHITE_WALKERS_WON, PlayerResult.PlayerStatus.DEFEATED) == EndingScript.Kind.ETERNAL_NIGHT, "nobody survived");
        Check.that(EndingScript.kindFor(GameRecord.GameResult.LAST_PLAYER_STANDING, PlayerResult.PlayerStatus.WINNER) == EndingScript.Kind.THRONE, "winner takes the throne");
        Check.that(EndingScript.kindFor(GameRecord.GameResult.LAST_PLAYER_STANDING, PlayerResult.PlayerStatus.DEFEATED) == EndingScript.Kind.CONQUERED, "loser is conquered");
        Check.that(EndingScript.kindFor(GameRecord.GameResult.LAST_PLAYER_STANDING, null) == EndingScript.Kind.CONQUERED, "an unknown player is not the winner");
        Check.assertAll();
    }

    @Test
    void unknownKingdomAndNamesDoNotBreakTheScript() {
        Check.that(!EndingScript.build(EndingScript.Kind.DAWN, null, null, 1, 1, 1).isEmpty(), "null kingdom and name are fine");
        Check.that(!EndingScript.build(EndingScript.Kind.THRONE, "SOMEWHERE", "", 11, 1, 1).isEmpty(), "unknown kingdom is fine");
        Check.assertAll();
    }
}
