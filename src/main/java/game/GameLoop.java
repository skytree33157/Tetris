package game;

import score.ScoreManager;
// 스레드 및 일시정지 클래스

public class GameLoop implements Runnable {

    private ActionController actionController;
    private GameStateManager gameStateManager;
    private volatile boolean isPaused = false;

    public GameLoop(ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
    }

    public synchronized void togglePause() {
        isPaused = !isPaused;

        //FR-37
        actionController.setPaused(isPaused);
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void run() {
        // 승자는 개인 게임 오버가 아니어도 경기 종료 시 컨트롤러가 멈추므로 함께 확인한다.
        while (!gameStateManager.isGameOver() && !actionController.isStopped()) {
            try {
                Thread.sleep(gameStateManager.getDropSpeed());
                if (!isPaused) {
                    actionController.automaticDropAction();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
