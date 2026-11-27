package game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import app.AppState;
import app.AppStateManager;
import blocks.core.Block;
import blocks.tetromino.TBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

class GameControllerInputTest {

    private ActionController action;
    private GameController controller;
    private GameLoop loop;
    private ScoreManager score;
    private JRootPane root;
    private Thread loopThread;

    private int pauseCalls;
    private final CountDownLatch exited = new CountDownLatch(1);

    @BeforeEach
    void setUp() throws Exception {
        onEdt(() -> {
            GameStateManager state = new GameStateManager();
            score = new ScoreManager(Difficulty.NORMAL);

            action = new ActionController(
                    new Board(),
                    new TBlock(),
                    state,
                    score,
                    Difficulty.NORMAL,
                    GameMode.NORMAL,
                    0
            );

            loop = new GameLoop(action, state, score);

            GameController.KeyBindings keys =
                    new GameController.KeyBindings(
                            KeyEvent.VK_LEFT,
                            KeyEvent.VK_RIGHT,
                            KeyEvent.VK_DOWN,
                            KeyEvent.VK_UP,
                            KeyEvent.VK_SPACE,
                            KeyEvent.VK_C
                    );

            controller = new GameController(
                    loop,
                    action,
                    state,
                    score,
                    new AppStateManager(AppState.PLAYING),
                    () -> {},
                    () -> pauseCalls++,
                    exited::countDown,
                    keys,
                    true
            );

            root = new JRootPane();
            controller.installKeyBindings(root);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        onEdt(() -> {
            if (controller != null) {
                controller.shutdown();
            }
            if (action != null) {
                action.shutdown();
            }
        });

        if (loopThread != null) {
            loopThread.interrupt();
            loopThread.join(1000);
            assertFalse(loopThread.isAlive());
        }
    }

    @ParameterizedTest
    @MethodSource("gameplayKeys")
    void gameplayKeyIsIgnoredWhileInputDisabled(int keyCode)
            throws Exception {

        onEdt(() -> {
            Block current = action.getCurrentBlock();
            Block next = action.getNextBlock();

            int initialX = current.getX();
            int initialY = current.getY();
            int initialScore = score.getScore();
            int[][] initialShape = copyShape(current.getShape());

            controller.setInputEnabled(false);
            press(keyCode);

            assertSame(current, action.getCurrentBlock());
            assertSame(next, action.getNextBlock());
            assertNull(action.getHeldBlock());

            assertEquals(initialX, current.getX());
            assertEquals(initialY, current.getY());
            assertEquals(initialScore, score.getScore());

            int[][] actualShape = current.getShape();
            assertEquals(initialShape.length, actualShape.length);

            for (int row = 0; row < initialShape.length; row++) {
                assertArrayEquals(initialShape[row], actualShape[row]);
            }
        });
    }

    static IntStream gameplayKeys() {
        return IntStream.of(
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_UP,
                KeyEvent.VK_SPACE,
                KeyEvent.VK_C
        );
    }

    @Test
    void disablingInputBlocksPauseAndEscapeExit() throws Exception {
        onEdt(() -> {
            // 먼저 누르고 있던 Esc 타이머도 취소돼야 한다.
            keyDown(KeyEvent.VK_ESCAPE);
            controller.setInputEnabled(false);

            press(KeyEvent.VK_P);
            keyDown(KeyEvent.VK_ESCAPE);
        });

        // EDT는 계속 동작하게 두고 종료 콜백 발생 여부를 기다린다.
        assertFalse(exited.await(
                GameController.ESCAPE_HOLD_THRESHOLD_MS + 300L,
                TimeUnit.MILLISECONDS
        ));

        onEdt(() -> {
            keyUp(KeyEvent.VK_ESCAPE);

            assertFalse(loop.isPaused());
            assertEquals(0, pauseCalls);

            // 다시 입력을 켜면 P도 정상적으로 사용할 수 있다.
            controller.setInputEnabled(true);
            press(KeyEvent.VK_P);

            assertTrue(loop.isPaused());
            assertEquals(1, pauseCalls);
        });
    }

    @Test
    void releasedKeysWorkAfterInputIsRestored() throws Exception {
        onEdt(() -> {
            controller.setInputEnabled(false);
            press(KeyEvent.VK_LEFT);

            controller.setInputEnabled(true);
            int initialX = action.getCurrentBlock().getX();

            press(KeyEvent.VK_LEFT);

            assertEquals(
                    initialX - 1,
                    action.getCurrentBlock().getX()
            );
            assertFalse(loop.isPaused());
        });
    }

    @Test
    void previouslyHeldKeyDoesNotResumeUntilReleased()
            throws Exception {

        onEdt(() -> {
            keyDown(KeyEvent.VK_LEFT);
            controller.setInputEnabled(false);
        });

        int stoppedX = action.getCurrentBlock().getX();

        // 원래 반복 이동이 발생할 수 있는 시간만큼 기다린다.
        Thread.sleep(400);

        onEdt(() -> {
            assertEquals(stoppedX, action.getCurrentBlock().getX());

            controller.setInputEnabled(true);

            // 손을 떼기 전의 반복 입력은 계속 무시한다.
            keyDown(KeyEvent.VK_LEFT);
            assertEquals(stoppedX, action.getCurrentBlock().getX());

            keyUp(KeyEvent.VK_LEFT);
            press(KeyEvent.VK_LEFT);

            assertEquals(
                    stoppedX - 1,
                    action.getCurrentBlock().getX()
            );
        });
    }

    @Test
    void keyPressedWhileBlockedRequiresReleaseBeforeReuse()
            throws Exception {

        onEdt(() -> {
            int initialX = action.getCurrentBlock().getX();

            controller.setInputEnabled(false);
            keyDown(KeyEvent.VK_LEFT);
            controller.setInputEnabled(true);

            keyDown(KeyEvent.VK_LEFT);
            assertEquals(initialX, action.getCurrentBlock().getX());

            keyUp(KeyEvent.VK_LEFT);
            press(KeyEvent.VK_LEFT);

            assertEquals(
                    initialX - 1,
                    action.getCurrentBlock().getX()
            );
        });
    }

    @Test
    void windowBindingsDisablePressButKeepRelease()
            throws Exception {

        onEdt(() -> {
            int[] keys = {
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_UP,
                KeyEvent.VK_SPACE,
                KeyEvent.VK_C,
                KeyEvent.VK_P,
                KeyEvent.VK_ESCAPE
            };

            controller.setInputEnabled(false);

            for (int key : keys) {
                assertFalse(binding(key, false).isEnabled());
                assertTrue(binding(key, true).isEnabled());
            }

            controller.setInputEnabled(true);

            for (int key : keys) {
                assertTrue(binding(key, false).isEnabled());
                assertTrue(binding(key, true).isEnabled());
            }
        });
    }

    @Test
    void automaticDropContinuesWhileInputIsDisabled()
            throws Exception {

        onEdt(() -> {
            controller.setInputEnabled(false);
            assertFalse(loop.isPaused());
        });

        int initialY = action.getCurrentBlock().getY();

        loopThread = new Thread(loop, "chat-input-test-loop");
        loopThread.setDaemon(true);
        loopThread.start();

        long deadline =
                System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (action.getCurrentBlock().getY() == initialY
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        onEdt(() -> {
            assertTrue(action.getCurrentBlock().getY() > initialY);
            assertFalse(loop.isPaused());
        });
    }

    private Action binding(int keyCode, boolean released) {
        Object name = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(keyCode, 0, released));

        Action boundAction = root.getActionMap().get(name);
        assertNotNull(boundAction);
        return boundAction;
    }

    private void press(int keyCode) {
        keyDown(keyCode);
        keyUp(keyCode);
    }

    private void keyDown(int keyCode) {
        controller.keyPressed(new KeyEvent(
                root, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED
        ));
    }

    private void keyUp(int keyCode) {
        controller.keyReleased(new KeyEvent(
                root, KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED
        ));
    }

    private static int[][] copyShape(int[][] source) {
        int[][] copy = new int[source.length][];

        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }

        return copy;
    }

    private static void onEdt(Runnable task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeAndWait(task);
        }
    }
}