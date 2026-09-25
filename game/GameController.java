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