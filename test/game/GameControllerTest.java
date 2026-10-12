package game;

import app.AppState;
import app.AppStateManager;
import blocks.tetromino.OBlock;
import board.Board;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    private AppSettings settings;

    @BeforeEach
    void setUp() {
        settings = AppSettings.getInstance();
        settings.resetToDefaults();
    }

    @AfterEach
    void tearDown() {
        settings.resetToDefaults();
    }

    @Test // 컨트롤러 종료 시 동작 확인
    void shutdown() {
        TestFixture fixture = new TestFixture();
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));
        fixture.controller.shutdown();
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));

        assertEquals(2, fixture.actions.leftMoves);
        fixture.controller.shutdown();
    }

    @Test // 키 바인딩 설정 테스트
    void installKeyBindings() {
        settings.setKey(KeyAction.MOVE_LEFT, KeyEvent.VK_A);
        GameController controller = new TestFixture().controller;
        JRootPane rootPane = new JRootPane();
        controller.installKeyBindings(rootPane);
        // 키 바인딩이 잘 설정됐는지 확인
        assertEquals("game.key.pressed." + KeyEvent.VK_A,
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false)));
        assertNotNull(rootPane.getActionMap().get("game.key.released." + KeyEvent.VK_A));

        settings.setKey(KeyAction.MOVE_LEFT, KeyEvent.VK_B);
        controller.installKeyBindings(rootPane);
        // 새로운 키 바인딩이 잘 설정됐는지 확인
        assertNull(rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false)));
        assertNull(rootPane.getActionMap().get("game.key.pressed." + KeyEvent.VK_A));
        assertEquals("game.key.pressed." + KeyEvent.VK_B,
                rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0, false)));
    }

    @Test // 키 입력 처리 테스트
    void keyPressed() {
        TestFixture fixture = new TestFixture();
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_DOWN));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_UP));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_SPACE));

        assertEquals(1, fixture.actions.leftMoves);
        assertEquals(1, fixture.actions.downMoves);
        assertEquals(1, fixture.actions.rotations);
        assertEquals(7, fixture.scoreManager.getScore());
        fixture.controller.shutdown();
    }

    @Test // 게임 재개 테스트
    void resumeGame() {
        TestFixture fixture = new TestFixture();
        fixture.gameLoop.togglePause();
        fixture.appStateManager.transitionTo(AppState.PAUSED);

        fixture.controller.resumeGame();

        assertFalse(fixture.gameLoop.isPaused());
        assertEquals(AppState.PLAYING, fixture.appStateManager.getCurrentState());
    }

    @Test // 키 릴리즈 처리 테스트
    void keyReleased() {
        TestFixture fixture = new TestFixture();
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));
        fixture.controller.keyReleased(fixture.key(KeyEvent.VK_LEFT));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_LEFT));

        assertEquals(2, fixture.actions.leftMoves);
        fixture.controller.shutdown();
    }

    @Test // 일시정지 테스트
    void pauseKeyTogglesGameStateOnlyOncePerPress() {
        TestFixture fixture = new TestFixture();
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_P));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_P));
        // P 키를 두 번 눌러도 게임이 한 번만 일시정지되는지 확인
        assertTrue(fixture.gameLoop.isPaused());
        assertEquals(AppState.PAUSED, fixture.appStateManager.getCurrentState());
        assertEquals(1, fixture.pauseCalls);
        // P 키를 릴리즈하고 다시 눌러 게임이 재개되는지 확인
        fixture.controller.keyReleased(fixture.key(KeyEvent.VK_P));
        fixture.controller.keyPressed(fixture.key(KeyEvent.VK_P));
        assertFalse(fixture.gameLoop.isPaused());
        assertEquals(AppState.PLAYING, fixture.appStateManager.getCurrentState());
        fixture.controller.shutdown();
    }

    private static class TestFixture {
        private final GameStateManager gameStateManager = new GameStateManager();
        private final ScoreManager scoreManager = new ScoreManager();
        private final TestActionController actions = new TestActionController(gameStateManager, scoreManager);
        private final GameLoop gameLoop = new GameLoop(actions, gameStateManager, scoreManager);
        private final AppStateManager appStateManager = new AppStateManager(AppState.PLAYING);
        private int pauseCalls;
        private final GameController controller = new GameController(
                gameLoop,
                actions,
                gameStateManager,
                scoreManager,
                appStateManager,
                () -> { },
                () -> pauseCalls++
        );

        private KeyEvent key(int keyCode) {
            return new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        }
    }

    private static class TestActionController extends ActionController {
        private int leftMoves;
        private int downMoves;
        private int rotations;

        private TestActionController(GameStateManager manager, ScoreManager scoreManager) {
            super(new Board(), new OBlock(), manager, scoreManager);
        }

        @Override
        public synchronized void moveLeftAction() {
            leftMoves++;
        }

        @Override
        public synchronized boolean moveDownAction() {
            downMoves++;
            return true;
        }

        @Override
        public synchronized void rotateAction() {
            rotations++;
        }

        @Override
        public synchronized int hardDropAction() {
            return 3;
        }
    }
}