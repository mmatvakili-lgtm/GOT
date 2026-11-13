package com.yourname.got.mediator;

public interface GameMessageListener {
    void onMessage(String message);

    /**
     * True only for the in-game screen. Game events (storage full, expired deals, the Wall ...)
     * are shown only there, never on the login/register/lobby screens.
     */
    default boolean acceptsGameEvents() {
        return false;
    }
}
