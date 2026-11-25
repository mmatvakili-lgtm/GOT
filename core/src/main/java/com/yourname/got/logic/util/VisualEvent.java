package com.yourname.got.logic.util;

/**
 * A replay event that only drives the battlefield picture or the army panel (it has no line in the text log).
 * pauseSeconds() is how long the replay waits after it at normal speed (0 = the next event follows at once).
 * Older replay files simply do not contain these events and play as before.
 */
public interface VisualEvent extends BattleEvent {
    default float pauseSeconds() { return 0f; }
}
