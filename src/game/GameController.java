package game;

import app.AppState;
import app.AppStateManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import score.DropType;
import score.ScoreManager;

// 키 조작 클래스

public class GameController extends KeyAdapter {
    private final GameLoop gameLoop;
    private final ActionController actionController;
    private final GameStateManager gameStateManager;
    private final ScoreManager scoreManager;
    private final AppStateManager appStateManager;
    private final Runnable pauseHandler;
    private final Runnable openSettingAction;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean downPressed = false;
    // 좌우 키를 함께 누르면 마지막으로 누른 방향을 반복 이동에 사용
    private int lastHorizontalDirection = 0;
    // 최초 이동이 중복 실행되는 것을 방지
    private final Set<Integer> pressedKeys = new HashSet<>();
    // 설정 변경 후 이전 키 바인딩을 제거하기 위해 현재 등록 키를 보관
    private final Set<Integer> boundKeyCodes = new HashSet<>();

    private final Timer keyTimer;

    public GameController(GameLoop gameLoop, ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager, AppStateManager appStateManager,
        Runnable openSettingAction, Runnable pauseHandler) {
        this.gameLoop = gameLoop;
        this.actionController = actionController;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
        this.appStateManager = appStateManager;
        this.pauseHandler = pauseHandler;
        this.openSettingAction = openSettingAction;

        // 최초 입력은 즉시 처리하고, 누르고 있을 때만 지연 후 반복
        keyTimer = new Timer(60, e -> responseInput());
        keyTimer.setInitialDelay(250);
    }

    public void shutdown() {
        keyTimer.stop();
        pressedKeys.clear();
    }

    public void installKeyBindings(JRootPane rootPane) {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        
        // 키 설정을 다시 적용할 때 이전 입력 및 액션 매핑을 먼저 정리
        for (int keyCode : boundKeyCodes) {
            inputMap.remove(KeyStroke.getKeyStroke(keyCode, 0, false));
            inputMap.remove(KeyStroke.getKeyStroke(keyCode, 0, true));
            actionMap.remove("game.key.pressed." + keyCode);
            actionMap.remove("game.key.released." + keyCode);
        }
        boundKeyCodes.clear();

        AppSettings settings = AppSettings.getInstance();
        boundKeyCodes.add(settings.getKey(KeyAction.MOVE_LEFT));
        boundKeyCodes.add(settings.getKey(KeyAction.MOVE_RIGHT));
        boundKeyCodes.add(settings.getKey(KeyAction.MOVE_DOWN));
        boundKeyCodes.add(settings.getKey(KeyAction.ROTATE));
        boundKeyCodes.add(KeyEvent.VK_P);
        boundKeyCodes.add(KeyEvent.VK_ESCAPE);
        boundKeyCodes.add(KeyEvent.VK_SPACE);

        // 창 내부 어느 컴포넌트에 포커스가 있어도 누름/해제 이벤트를 받도록 등록
        for (int keyCode : boundKeyCodes) {
            String pressedAction = "game.key.pressed." + keyCode;
            String releasedAction = "game.key.released." + keyCode;
            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressedAction);
            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releasedAction);
            actionMap.put(pressedAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    handleKeyPressed(keyCode);
                }
            });
            actionMap.put(releasedAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    handleKeyReleased(keyCode);
                }
            });
        }
        // 설정 화면 등에서 키 해제 이벤트를 놓쳤을 수 있으므로 입력 상태 초기화
        pressedKeys.clear();
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
        // 양쪽 키가 눌려 있으면 마지막으로 눌린 쪽만 반복해 상쇄 이동 방지
        if (leftPressed && rightPressed) {
            if (lastHorizontalDirection < 0) {
                actionController.moveLeftAction();
            } else {
                actionController.moveRightAction();
            }
        } else if (leftPressed) {
            actionController.moveLeftAction();
        } else if (rightPressed) {
            actionController.moveRightAction();
        }
        if(downPressed) {
            moveDownWithScore();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        handleKeyPressed(e.getKeyCode());
    }

    private void handleKeyPressed(int keyCode) {
        // 누르고 있는 동안 전달되는 중복 keyPressed는 새 입력으로 처리하지 않음
        if (!pressedKeys.add(keyCode)) {
            return;
        }

        // Pause 기능
        if(keyCode == KeyEvent.VK_P) {
            if (gameStateManager.isGameOver()) {
                return;
            }
            
            // 일시정지 상태 변경
            boolean wasPaused = gameLoop.isPaused();
            gameLoop.togglePause();
            appStateManager.transitionTo(
                    wasPaused ? AppState.PLAYING : AppState.PAUSED
            );
            if (!wasPaused) {
                leftPressed = false;
                rightPressed = false;
                downPressed = false;
                keyTimer.stop();
                pauseHandler.run();
            return;
            }
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (gameStateManager.isGameOver()) {
                return;
            }
            boolean wasPaused = gameLoop.isPaused();
            if (!wasPaused) {
                gameLoop.togglePause();
            }
            appStateManager.transitionTo(AppState.SETTINGS);

            // modal 창이 포커스 가져가면 keyReleased를 못 받으므로,
            // 누르고 있던 이동 키 초기화
            leftPressed = false;
            rightPressed = false;
            downPressed = false;
            keyTimer.stop();
            pressedKeys.clear();

            openSettingAction.run(); // modal 블록

            // P로 이미 일시정지된 상태에서 ESC로 설정을 열었다가 닫으면 
            // PLAYING으로 돌아가지 않고 PAUSED를 유지
            if (!wasPaused) {
                gameLoop.togglePause();
                appStateManager.transitionTo(AppState.PLAYING);
            } else {
                appStateManager.transitionTo(AppState.PAUSED);
            }
            return;
        }

        // 일시정지, 게임 오버 시 키 입력 무시
        if(gameLoop.isPaused()||gameStateManager.isGameOver()) {
            return;
        }


        AppSettings settings = AppSettings.getInstance();
        // 이동 키는 눌린 순간 한 번 이동한 뒤, 타이머로 꾹 누르기 반복을 시작
        if(keyCode==settings.getKey(KeyAction.MOVE_LEFT)){
            leftPressed = true;
            lastHorizontalDirection = -1;
            actionController.moveLeftAction();
            keyTimer.restart();
        }
        else if(keyCode==settings.getKey(KeyAction.MOVE_RIGHT)){
            rightPressed = true;
            lastHorizontalDirection = 1;
            actionController.moveRightAction();
            keyTimer.restart();
        }
        else if(keyCode==settings.getKey(KeyAction.MOVE_DOWN)){
            downPressed = true;
            moveDownWithScore();
            keyTimer.restart();
        }
        else if(keyCode==settings.getKey(KeyAction.ROTATE)){
            actionController.rotateAction();
        }
        else if(keyCode==KeyEvent.VK_SPACE){
            int distance = actionController.hardDropAction();
            int currentLevel = gameStateManager.getCurrentLevel();
            // ScoreManager에 하드드롭 점수 전달
            scoreManager.addDropScore(distance, DropType.HARD, currentLevel);
        }
    }

    // 게임 재시작
    public void resumeGame() {
        if (gameLoop.isPaused()) {
            gameLoop.togglePause();
            appStateManager.transitionTo(AppState.PLAYING);
        }
    }

    // 키 해제 시 이동 중지
    @Override 
    public void keyReleased(KeyEvent e) {
        handleKeyReleased(e.getKeyCode());
    }

    private void handleKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        if (keyCode == KeyEvent.VK_P) {
            return;
        }

        AppSettings settings = AppSettings.getInstance();
        if (keyCode == settings.getKey(KeyAction.MOVE_LEFT)) {
            leftPressed = false;
        } else if (keyCode == settings.getKey(KeyAction.MOVE_RIGHT)) {
            rightPressed = false;
        } else if (keyCode == settings.getKey(KeyAction.MOVE_DOWN)) {
            downPressed = false;
        }

        if (!leftPressed && !rightPressed && !downPressed) {
            keyTimer.stop();
        } else {
            keyTimer.restart();
        }
    }

    private void moveDownWithScore() {
        // 실제 하강한 경우에만 소프트 드롭 점수를 부여
        boolean isMovedDown = actionController.moveDownAction();
        if (isMovedDown) {
            int currentLevel = gameStateManager.getCurrentLevel();
            scoreManager.addDropScore(1, DropType.SOFT, currentLevel);
        }
    }
}