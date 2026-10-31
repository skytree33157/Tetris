package game;

import java.util.concurrent.TimeUnit;
import score.ScoreManager;

/** 자동 낙하는 플레이어별로 실행하되, 정지 상태와 시계는 ActionController를 따른다. */
public class GameLoop implements Runnable {
    private final ActionController actionController;
    private final GameStateManager gameStateManager;

    public GameLoop(ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
    }

    public void togglePause() {
        synchronized (actionController.stateLock()) {
            actionController.setPaused(!actionController.isPaused());
        }
    }

    public boolean isPaused() { return actionController.isPaused(); }

    @Override public void run() {
        Object lock = actionController.stateLock();
        synchronized (lock) {
            long nextDrop = actionController.activeTimeNanos()
                    + TimeUnit.MILLISECONDS.toNanos(gameStateManager.getDropSpeed());
            try {
                while (!actionController.isGameOver() && !actionController.isStopped()) {
                    if (actionController.isPaused()) {
                        lock.wait();
                        continue;
                    }
                    long remaining = nextDrop - actionController.activeTimeNanos();
                    if (remaining > 0) {
                        TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                        continue;
                    }
                    actionController.automaticDropAction();
                    // 지연된 프레임을 한꺼번에 보상하지 않고 다음 한 칸을 예약한다.
                    nextDrop = actionController.activeTimeNanos()
                            + TimeUnit.MILLISECONDS.toNanos(gameStateManager.getDropSpeed());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
