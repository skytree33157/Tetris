package game;

import app.AppState;
import app.AppStateManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import score.DropType;
import score.ScoreManager;

// 키 조작 클래스

public class GameController extends KeyAdapter {
    private GameLoop gameLoop;
    private ActionController actionController;
    private GameStateManager gameStateManager;
    private ScoreManager scoreManager;
    private AppStateManager appStateManager;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean downPressed = false;

    private Timer keyTimer;

    public GameController(GameLoop gameLoop, ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager, AppStateManager appStateManager) {
        this.gameLoop = gameLoop;
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
        this.appStateManager = appStateManager;


        //ToDo : StartMenu의 select()와 연결
        keyTimer=new Timer(100, e->responseInput());
        keyTimer.start();
    }

    // 꾹 누르고 있는 키에 대한 입력 처리
    private void responseInput() {
        if (gameLoop.isPaused()) {
            return;
        }
        if (gameStateManager.isGameOver()) {
            keyTimer.stop();
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
        // Pause 기능
        if(e.getKeyCode() == KeyEvent.VK_P) {
            if (gameStateManager.isGameOver()) {
                return;
            }
            
            // 일시정지 상태 변경
            boolean wasPaused = gameLoop.isPaused();
            gameLoop.togglePause();
            appStateManager.transitionTo(
                    wasPaused ? AppState.PLAYING : AppState.PAUSED
            );
            return;
        }

        // 일시정지, 게임 오버 시 키 입력 무시
        if(gameLoop.isPaused()||gameStateManager.isGameOver()) {
            return;
        }


        AppSettings settings = AppSettings.getInstance();
        int keyCode = e.getKeyCode();

        // left, right, down : 처음 눌렀을 때 즉시 반응
        if(keyCode==settings.getKey(KeyAction.MOVE_LEFT)){
            if(!leftPressed){
                    actionController.moveLeftAction();
                }
                leftPressed = true;
        }
        else if(keyCode==settings.getKey(KeyAction.MOVE_RIGHT)){
            if(!rightPressed){
                    actionController.moveRightAction();
                }
                rightPressed = true;
        }
        else if(keyCode==settings.getKey(KeyAction.MOVE_DOWN)){
            if(!downPressed){
                    boolean isMovedDown = actionController.moveDownAction();
                    if(isMovedDown) {
                        // Soft Drop 점수 계산
                        int currentLevel = gameStateManager.getCurrentLevel();
                        scoreManager.addDropScore(1, DropType.SOFT, currentLevel);
            }
                }
                downPressed = true;
        }
        else if(keyCode==settings.getKey(KeyAction.ROTATE)){
            actionController.rotateAction();
        }
//Todo : KeyAction에 HARDDROP 추가 시 수정------
        else if(keyCode==KeyEvent.VK_SPACE){
//--------------------------------------------
            int distance = actionController.hardDropAction();
            int currentLevel = gameStateManager.getCurrentLevel();
            // ScoreManager에 하드드롭 점수 전달
            scoreManager.addDropScore(distance, DropType.HARD, currentLevel);
        }
    }

    // 키 해제 시 이동 중지
    @Override 
    public void keyReleased(KeyEvent e) {
        AppSettings settings = AppSettings.getInstance();
        int keyCode = e.getKeyCode();

        if (keyCode == settings.getKey(KeyAction.MOVE_LEFT)) {
            leftPressed = false;
        } else if (keyCode == settings.getKey(KeyAction.MOVE_RIGHT)) {
            rightPressed = false;
        } else if (keyCode == settings.getKey(KeyAction.MOVE_DOWN)) {
            downPressed = false;
        }
    }
}