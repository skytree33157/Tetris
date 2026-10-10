package game;

import static org.junit.jupiter.api.Assertions.*;

import app.AppState;
import app.AppStateManager;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

// FR-13 게임 중 종료(ESC로 설정 진입), FR-19 게임 중 설정 변경
class GameControllerTest {
    private final JPanel source = new JPanel();

    private GameStateManager manager;
    private ActionController action;
    private GameLoop loop;
    private AppStateManager appState;
    private GameController controller;

    private int settingsOpened;
    private int pauseShown;
    private boolean pausedWhileSettingsOpen;
    private AppState stateWhileSettingsOpen;

    @BeforeEach
    void setUp() {
        manager = new GameStateManager();
        ScoreManager scoreManager = new ScoreManager();
        action = new ActionController(new Board(), new OBlock(), manager, scoreManager,
                Difficulty.NORMAL, GameMode.NORMAL, 0);
        loop = new GameLoop(action, manager, scoreManager);
        appState = new AppStateManager(AppState.PLAYING);
        controller = new GameController(loop, action, manager, scoreManager, appState,
                () -> { // 설정 창이 떠 있는 동안의 상태 기록
                    settingsOpened++;
                    pausedWhileSettingsOpen = loop.isPaused();
                    stateWhileSettingsOpen = appState.getCurrentState();
                },
                () -> pauseShown++);
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
        action.shutdown();
    }

    // 키를 눌렀다 떼는 한 번의 입력 (누르고 있는 동안의 중복 입력은 무시되므로 release까지 전달)
    private void press(int keyCode) {
        controller.keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED));
        controller.keyReleased(new KeyEvent(source, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    @Test // FR-19: ESC로 설정을 여는 동안 일시정지, 닫으면 게임 재개
    void escPausesWhileSettingsOpenAndResumesAfter() {
        press(KeyEvent.VK_ESCAPE);

        assertEquals(1, settingsOpened);
        assertTrue(pausedWhileSettingsOpen);
        assertEquals(AppState.SETTINGS, stateWhileSettingsOpen);
        assertFalse(loop.isPaused());
        assertEquals(AppState.PLAYING, appState.getCurrentState());
    }

    @Test // FR-19: P로 멈춘 상태에서 설정을 열고 닫으면 일시정지 유지
    void escWhilePausedKeepsPausedAfterSettings() {
        press(KeyEvent.VK_P);
        press(KeyEvent.VK_ESCAPE);

        assertEquals(1, settingsOpened);
        assertTrue(loop.isPaused());
        assertEquals(AppState.PAUSED, appState.getCurrentState());
    }

    @Test // P로 일시정지 화면 표시, 다시 P로 재개
    void pTogglesPause() {
        press(KeyEvent.VK_P);
        assertTrue(loop.isPaused());
        assertEquals(AppState.PAUSED, appState.getCurrentState());
        assertEquals(1, pauseShown);

        press(KeyEvent.VK_P);
        assertFalse(loop.isPaused());
        assertEquals(AppState.PLAYING, appState.getCurrentState());
        assertEquals(1, pauseShown);
    }

    @Test // 일시정지 중에는 이동 키 무시
    void movementIgnoredWhilePaused() {
        int startX = action.getCurrentBlock().getX();
        press(KeyEvent.VK_P);

        press(AppSettings.getInstance().getKey(KeyAction.MOVE_LEFT));

        assertEquals(startX, action.getCurrentBlock().getX());
    }

    @Test // FR-13: 게임 오버 후에는 ESC, P 무시
    void escAndPIgnoredAfterGameOver() {
        manager.setGameOver(true);

        press(KeyEvent.VK_ESCAPE);
        press(KeyEvent.VK_P);

        assertEquals(0, settingsOpened);
        assertEquals(0, pauseShown);
        assertFalse(loop.isPaused());
    }
}
