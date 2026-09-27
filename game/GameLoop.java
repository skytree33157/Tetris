package game;

// 스레드 및 일시정지 클래스

public class GameLoop implements Runnable {

    private ActionController actionController;
    private GameStateManager gameStateManager;
    private boolean isPaused = false;

    public GameLoop(ActionController actionController, GameStateManager gameStateManager) {
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
    }

    public void togglePause() {
        isPaused = !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(gameStateManager.getDropSpeed());
                if (!isPaused) {
                    actionController.moveDownAction();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}