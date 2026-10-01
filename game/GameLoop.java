package game;

import score.DropType;
import score.ScoreManager;
// 스레드 및 일시정지 클래스

public class GameLoop implements Runnable {

    private ActionController actionController;
    private GameStateManager gameStateManager;
    private ScoreManager scoreManager;
    private volatile boolean isPaused = false;

    public GameLoop(ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
    }

    public void togglePause() {
        isPaused = !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void run() {
        while (!gameStateManager.isGameOver()) { //gameover = true일 때까지 반복
            try {
                Thread.sleep(gameStateManager.getDropSpeed());
                if (!isPaused) {
                    boolean isMovedDown = actionController.moveDownAction();
                    if(isMovedDown) {
                        // AUTO Drop 점수 계산
                        int currentLevel = gameStateManager.getCurrentLevel();
                        scoreManager.addDropScore(1, DropType.AUTO, currentLevel);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}