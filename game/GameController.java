package game;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GameController extends KeyAdapter {
    private GameLoop gameLoop;

    public GameController(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // P 키 입력 시 isPaused 전환 후 종료
        if(e.getKeyCode() == KeyEvent.VK_P) {
            gameLoop.togglePause();
            return;
        }
        // 일시정지이면 키 입력 무시
        if(gameLoop.isPaused()) {
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                gameLoop.moveLeftAction();
                break;
            case KeyEvent.VK_RIGHT:
                gameLoop.moveRightAction();
                break;
            case KeyEvent.VK_DOWN:
                gameLoop.moveDownAction();
                break;
            case KeyEvent.VK_UP:
                gameLoop.rotateAction();
                break;
        }
    }
}