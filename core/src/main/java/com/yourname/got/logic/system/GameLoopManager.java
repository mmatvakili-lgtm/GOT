package com.yourname.got.logic.system;

import com.badlogic.gdx.Gdx;
import com.yourname.got.logic.login.GameUser;
import com.yourname.got.logic.model.GameOfThrones;
import com.yourname.got.logic.system.KingdomUpdater.UpdateResult;
import com.yourname.got.mediator.GameMediator;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ticks the game every few seconds.
 *
 * The thread itself only sleeps. Every tick is executed on the libGDX render thread
 * (via Gdx.app.postRunnable), the same thread that handles button clicks and drawing, so
 * the game state is never modified from two threads at once. If the previous tick has not
 * run yet (e.g. the window is paused) the new tick is skipped instead of piling up.
 * Without libGDX (headless tests) the tick simply runs on the loop thread.
 */
public class GameLoopManager extends Thread {
    private final GameUserList userList;
    private final GameMediator mediator;
    private final int tickMillis = 5000;
    private final AtomicBoolean tickPending = new AtomicBoolean(false);
    private volatile boolean stopRequested = false;
    private final GameOfThrones game;
    private final int userId;

    public GameLoopManager(GameUserList users, GameMediator mediator) {
        this.userList = users;
        this.mediator = mediator;
        GameUser first = (users != null && !users.isEmpty()) ? users.get(0) : null;
        this.game = (first != null) ? first.getGame() : null;
        this.userId = (first != null) ? first.getId() : -1;
        setName("GameLoop");
    }

    /** True if this loop already drives the given game for the given user. */
    public boolean isRunningFor(GameOfThrones g, int uid) {
        return isAlive() && !stopRequested && g == this.game && uid == this.userId;
    }

    /** Asks the loop to end as soon as possible. */
    public void requestStop() {
        stopRequested = true;
        interrupt();
    }

    @Override
    public void run() {
        if (game == null) {
            System.err.println("GameLoopManager: Game instance is null. Loop will not start.");
            return;
        }

        while (!game.isFinished() && !stopRequested) {
            if (Gdx.app != null) {
                if (tickPending.compareAndSet(false, true)) {
                    Gdx.app.postRunnable(() -> {
                        try {
                            if (!stopRequested) tick();
                        } finally {
                            tickPending.set(false);
                        }
                    });
                }
            } else {
                tick();
            }

            try {
                Thread.sleep(tickMillis);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        System.out.println("Game loop has gracefully ended.");
    }

    /** One game tick. Always runs on a single thread (see class comment). */
    private void tick() {
        try {
            GameUpdater.updateGlobalState(game);

            userList.forEach(user -> {
                UpdateResult result = GameUpdater.updateUser(user, this.mediator);
                if (result != null && result.visualsChanged) {
                    mediator.triggerUIRefresh(user.getId());
                }
            });

            if (game.getBazar() != null) {
                // Expired deals used to be noticed only when somebody opened the market panel.
                game.getBazar().checkBazar(game.getSpeed());
                game.getBazar().autoSeedGenericOffersIfDue(game.getGameTime());
            }
            com.yourname.got.logic.login.GameFileUtility.flushPendingSave();
        } catch (Exception e) {
            System.err.println("!!! Error during game tick !!!");
            e.printStackTrace();
        }
    }
}
