package game;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

// 키 조작 클래스

public class GameController extends KeyAdapter {
    private GameLoop gameLoop;
    private ActionController actionController;

    public GameController(GameLoop gameLoop, ActionController actionController) {
        this.gameLoop = gameLoop;
        this.actionController = actionController;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // P 키 입력 시 일시정지 상태 변경
        if(e.getKeyCode() == KeyEvent.VK_P) {
            gameLoop.togglePause();
            return;
        }
        // 일시정지 시 키 입력 무시
        if(gameLoop.isPaused()) {
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                actionController.moveLeftAction();
                break;
            case KeyEvent.VK_RIGHT:
                actionController.moveRightAction();
                break;
            case KeyEvent.VK_DOWN:
                actionController.moveDownAction();
                break;
            case KeyEvent.VK_UP:
                actionController.rotateAction();
                break;
            case KeyEvent.VK_SPACE:
                actionController.hardDropAction();
                break;
        }
    }
}