package com.yourname.got.logic.system;

import com.yourname.got.logic.login.PlayerResult;
import com.yourname.got.logic.model.GameRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * The story told at the end of a game: one scripted sequence of shots for every combination of ending and kingdom.
 * Pure text and timing, no graphics, so it can be tested; EndingCinematicScreen draws it.
 */
public final class EndingScript {
    private EndingScript() {}

    /** How the game ended for one particular player. */
    public enum Kind {
        /** The Long Night was survived and this player's kingdom still stands. */
        DAWN("THE DAWN"),
        /** The Long Night was survived by others, but this player's kingdom fell. */
        FALLEN("THE FALLEN"),
        /** No kingdom survived the Long Night. */
        ETERNAL_NIGHT("THE ETERNAL NIGHT"),
        /** Last ruler standing: this player takes the throne. */
        THRONE("THE IRON THRONE"),
        /** Last ruler standing: another player took the throne. */
        CONQUERED("THE CONQUERED");

        public final String headline;
        Kind(String headline) { this.headline = headline; }
    }

    public enum Mood { NIGHT, STORM, FIRE, ASH, DAWN, GOLD }

    public static final class Shot {
        public final String title, text;
        public final float seconds;
        public final Mood mood;
        Shot(String title, String text, float seconds, Mood mood) {
            this.title = title; this.text = text; this.seconds = seconds; this.mood = mood;
        }
    }

    /** What a kingdom is called and how it speaks, for the personal parts of the story. */
    private static final class House {
        final String name, words, ruler, seat, dawn, fall, throne;
        House(String name, String words, String ruler, String seat, String dawn, String fall, String throne) {
            this.name = name; this.words = words; this.ruler = ruler; this.seat = seat;
            this.dawn = dawn; this.fall = fall; this.throne = throne;
        }
    }

    private static House house(String kingdom) {
        String k = kingdom == null ? "" : kingdom.toUpperCase();
        switch (k) {
            case "WINTERFELL": return new House("Stark", "Winter Is Coming", "Warden of the North", "Winterfell",
                "Snow melted from the walls of Winterfell, and the wolves of the North howled at a sun they had almost forgotten.",
                "The great hall of Winterfell stood silent. Winter had come at last, and it had come for the Starks.",
                "The wolf banner rose over the Red Keep, and the North remembered who now ruled the Seven Kingdoms.");
            case "EYRIE": return new House("Arryn", "As High as Honor", "Lord of the Eyrie", "the Eyrie",
                "High above the clouds the Eyrie caught the first light, and the falcons of the Vale took flight into a clear sky.",
                "The Moon Door stood open and the wind carried the last falcons away. The Eyrie fell, and the Vale with it.",
                "From the Eyrie to the Iron Throne the falcon banner flew, as high as honor could lift it.");
            case "IRON_ISLANDS": return new House("Greyjoy", "We Do Not Sow", "Lord Reaper of Pyke", "Pyke",
                "The sea rolled grey and calm around Pyke. The ironborn had held, and the Drowned God was content.",
                "Pyke sank beneath the black waves. What is dead may never die, but the ironborn did not rise again.",
                "The kraken sat the Iron Throne, and the ironborn took what they had never sown.");
            case "CASTERLY_ROCK": return new House("Lannister", "Hear Me Roar", "Lord of Casterly Rock", "Casterly Rock",
                "Gold glittered on the cliffs of Casterly Rock as dawn broke. A Lannister always pays his debts, and the realm owed them much.",
                "The lion's roar went quiet beneath the Rock. All the gold of the West could not buy back one more hour of night.",
                "The lion took the Iron Throne, and every lord in Westeros owed him a debt.");
            case "STORMS_END": return new House("Baratheon", "Ours Is the Fury", "Lord of Storm's End", "Storm's End",
                "The storms that never left Storm's End finally fell silent, and the stag walked out into a clear and quiet morning.",
                "Storm's End, which had broken every siege, was broken at last. Even fury is no answer to the dead.",
                "The stag crowned the Iron Throne, and the fury of the Stormlands became the law of the realm.");
            case "HIGHGARDEN": return new House("Tyrell", "Growing Strong", "Lord of Highgarden", "Highgarden",
                "Spring came early to Highgarden. Roses pushed through the ash and the Reach bloomed over the graves of the dead.",
                "The roses of Highgarden withered under the frost. No garden grows in the Long Night.",
                "The rose climbed the Iron Throne, and the Reach fed a realm that remembered how to grow.");
            case "DRAGONSTONE": return new House("Targaryen", "Fire and Blood", "Prince of Dragonstone", "Dragonstone",
                "Fire and dragonglass had held the dark. The dragon banner flew over Dragonstone as the first sun in a month touched the sea.",
                "The fires of Dragonstone went out one by one. Fire and blood were not enough, and the last dragon fell silent.",
                "The three-headed dragon claimed the Iron Throne, and the world heard fire and blood once more.");
            default: return new House("the realm", "The Night Is Dark", "ruler of the realm", "the keep",
                "The sun rose over a kingdom that had refused to die.",
                "The kingdom fell, and the snow covered what was left.",
                "One ruler remained, and the Iron Throne was theirs.");
        }
    }

    /** Which ending this player gets. {@code status} may be null for a player who is not in the record. */
    public static Kind kindFor(GameRecord.GameResult result, PlayerResult.PlayerStatus status) {
        if (result == GameRecord.GameResult.WHITE_WALKERS_WON) return Kind.ETERNAL_NIGHT;
        if (result == GameRecord.GameResult.LAST_PLAYER_STANDING) {
            return status == PlayerResult.PlayerStatus.WINNER ? Kind.THRONE : Kind.CONQUERED;
        }
        // survived the Long Night (or time ran out): survivors see the dawn, the others their fall
        return status == PlayerResult.PlayerStatus.DEFEATED ? Kind.FALLEN : Kind.DAWN;
    }

    /** The shots for one player's ending. {@code survivors} and {@code players} only colour the narration. */
    public static List<Shot> build(Kind kind, String kingdom, String player, int rank, int survivors, int players) {
        House h = house(kingdom);
        String who = (player == null || player.isEmpty()) ? "the ruler" : player;
        List<Shot> s = new ArrayList<>();
        switch (kind) {
            case DAWN:
                s.add(new Shot("THE LONG NIGHT", "For thirty days the sky was black, and the dead walked in the snow.", 6f, Mood.NIGHT));
                s.add(new Shot(h.seat.toUpperCase(), "At " + h.seat + ", the " + h.ruler + " stood among the last living soldiers of House " + h.name + ".", 6.5f, Mood.STORM));
                s.add(new Shot("THE NIGHT KING", "He raised his arms to call the dead for the last time. The line held. He fell silent, and his army crumbled to snow.", 7f, Mood.FIRE));
                s.add(new Shot("THE DAWN", h.dawn, 7f, Mood.DAWN));
                s.add(new Shot("HOUSE " + h.name.toUpperCase(), (survivors < players
                    ? (players - survivors) + " kingdom" + (players - survivors == 1 ? "" : "s") + " did not see this morning. "
                    : "Every kingdom saw this morning. ") + "Hail " + who + ", " + h.ruler + ".", 6.5f, Mood.GOLD));
                s.add(new Shot("\"" + h.words.toUpperCase() + "\"", "The realm endures.", 5f, Mood.GOLD));
                break;
            case FALLEN:
                s.add(new Shot("THE LONG NIGHT", "The dead came out of the snow in numbers no army could count.", 6f, Mood.NIGHT));
                s.add(new Shot(h.seat.toUpperCase(), h.fall, 7f, Mood.ASH));
                s.add(new Shot("HOUSE " + h.name.toUpperCase(), "The banners burned. " + who + " ranked " + rank + ordinal(rank) + " when the last gate fell.", 6.5f, Mood.ASH));
                s.add(new Shot("THE DAWN, WITHOUT YOU", "Others lived to see the sun rise. They will tell the story of " + h.name + ".", 6.5f, Mood.DAWN));
                s.add(new Shot("\"" + h.words.toUpperCase() + "\"", "Their words are all that remain.", 5f, Mood.ASH));
                break;
            case ETERNAL_NIGHT:
                s.add(new Shot("THE LONG NIGHT", "No kingdom stood when the dawn should have come.", 6f, Mood.NIGHT));
                s.add(new Shot(h.seat.toUpperCase(), h.fall, 7f, Mood.ASH));
                s.add(new Shot("THE NIGHT KING", "He walked through the gates of the last keep and the dead knelt around him.", 6.5f, Mood.STORM));
                s.add(new Shot("THE SUN NEVER ROSE", "The seven kingdoms, and everyone who had fought over them, became part of the snow.", 7f, Mood.NIGHT));
                s.add(new Shot("\"" + h.words.toUpperCase() + "\"", "Winter has come, and it will not leave.", 5.5f, Mood.ASH));
                break;
            case THRONE:
                s.add(new Shot("THE LAST RIVAL FALLS", "Every banner but one lay in the dust. " + who + " stood alone.", 6f, Mood.STORM));
                s.add(new Shot(h.seat.toUpperCase(), "From " + h.seat + " the " + h.ruler + " rode to King's Landing.", 6.5f, Mood.FIRE));
                s.add(new Shot("THE IRON THRONE", h.throne, 7.5f, Mood.GOLD));
                s.add(new Shot("HOUSE " + h.name.toUpperCase(), "Hail " + who + ", ruler of the Seven Kingdoms.", 6f, Mood.GOLD));
                s.add(new Shot("\"" + h.words.toUpperCase() + "\"", "The game of thrones is won.", 5f, Mood.GOLD));
                break;
            case CONQUERED:
            default:
                s.add(new Shot("THE LAST BATTLE", "One by one the great houses fell, until a single banner remained.", 6f, Mood.STORM));
                s.add(new Shot(h.seat.toUpperCase(), h.fall, 7f, Mood.ASH));
                s.add(new Shot("THE IRON THRONE", "Another sits the throne. " + who + " ranked " + rank + ordinal(rank) + " in the game of thrones.", 6.5f, Mood.GOLD));
                s.add(new Shot("\"" + h.words.toUpperCase() + "\"", "House " + h.name + " will be remembered.", 5f, Mood.ASH));
                break;
        }
        return s;
    }

    public static float totalSeconds(List<Shot> shots) {
        float t = 0;
        for (Shot x : shots) t += x.seconds;
        return t;
    }

    private static String ordinal(int n) {
        int m = n % 100;
        if (m >= 11 && m <= 13) return "th";
        switch (n % 10) { case 1: return "st"; case 2: return "nd"; case 3: return "rd"; default: return "th"; }
    }
}
