package game;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import score.DropType;
import score.ScoreManager;

// 키 조작 클래스

public class GameController extends KeyAdapter {
    private GameLoop gameLoop;
    private ActionController actionController;
    private GameStateManager gameStateManager;
    private ScoreManager scoreManager;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean downPressed = false;

    private Timer keyTimer;

    public GameController(GameLoop gameLoop, ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        this.gameLoop = gameLoop;
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;

        keyTimer=new Timer(100, e->responseInput());
        keyTimer.start();
    }

    // 꾹 누르고 있는 키에 대한 입력 처리
    private void responseInput() {
        if(gameLoop.isPaused()||gameStateManager.isGameOver()) {
            return;
        }
        if(leftPressed) {
            actionController.moveLeftAction();
        }
        if(rightPressed) {
            actionController.moveRightAction();
        }
        if(downPressed) {
            boolean isMovedDown = actionController.moveDownAction();
            if(isMovedDown) {
                // Soft Drop 점수 계산
                int currentLevel = gameStateManager.getCurrentLevel();
                scoreManager.addDropScore(1, DropType.SOFT, currentLevel);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // P 키 입력 시 일시정지 상태 변경
        if(e.getKeyCode() == KeyEvent.VK_P) {
            gameLoop.togglePause();
            return;
        }
        // 일시정지, 게임 오버 시 키 입력 무시
        if(gameLoop.isPaused()||gameStateManager.isGameOver()) {
            return;
        }

        switch (e.getKeyCode()) {
            // left, right down : 처음 눌렀을 때 즉시 반응
            case KeyEvent.VK_LEFT:
                if(!leftPressed){
                    actionController.moveLeftAction();
                }
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
                if(!rightPressed){
                    actionController.moveRightAction();
                }
                rightPressed = true;
                break;
            case KeyEvent.VK_DOWN:
                if(!downPressed){
                    boolean isMovedDown = actionController.moveDownAction();
                    if(isMovedDown) {
                        // Soft Drop 점수 계산
                        int currentLevel = gameStateManager.getCurrentLevel();
                        scoreManager.addDropScore(1, DropType.SOFT, currentLevel);
            }
                }
                downPressed = true;
                break;
            case KeyEvent.VK_UP:
                actionController.rotateAction();
                break;
            case KeyEvent.VK_SPACE:
                int distance = actionController.hardDropAction();
                int currentLevel = gameStateManager.getCurrentLevel();
                // ScoreManager에 하드드롭 점수 전달
                scoreManager.addDropScore(distance, DropType.HARD, currentLevel);
                break;
        }
    }

    // 키 해제 시 이동 중지
    @Override 
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                break;
            case KeyEvent.VK_DOWN:
                downPressed = false;
                break;
        }
    }
}