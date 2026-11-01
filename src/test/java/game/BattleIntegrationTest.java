package game;

import app.AppState;
import app.AppStateManager;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import score.ScoreManager;
import static org.junit.jupiter.api.Assertions.*;

/** 실제 엔진 두 개를 묶어 UI 담당자가 사용하는 연결 경로를 검증한다. */
class BattleIntegrationTest {
    @Test
    void bothRealDropThreadsPauseResumeAndExitTogether() throws Exception {
        try (BattleController battle = new BattleController()) {
            GameStateManager s1 = new GameStateManager() { @Override public int getDropSpeed() { return 120; } };
            GameStateManager s2 = new GameStateManager() { @Override public int getDropSpeed() { return 120; } };
            ScoreManager p1 = new ScoreManager(), p2 = new ScoreManager();
            ActionController one = player(new Board(), s1, p1, 0), two = player(new Board(), s2, p2, 0);
            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);
            battle.start();
            Thread t1 = new Thread(new GameLoop(one, s1, p1)), t2 = new Thread(new GameLoop(two, s2, p2));
            t1.start(); t2.start();
            try {
                long deadline = System.nanoTime() + 2_000_000_000L;
                while ((one.getViewSnapshot().currentBlock().y() < 1 || two.getViewSnapshot().currentBlock().y() < 1)
                        && System.nanoTime() < deadline) Thread.sleep(5);
                battle.setPaused(true);
                var before = battle.getViewSnapshot();
                Thread.sleep(300);
                for (var id : BattleController.Player.values()) {
                    var old = before.players().get(id);
                    var now = battle.getViewSnapshot().players().get(id);
                    assertTrue(old.currentBlock().y() >= 1);
                    assertEquals(old.currentBlock().y(), now.currentBlock().y());
                    assertEquals(old.score(), now.score());
                }
                battle.setPaused(false);
                deadline = System.nanoTime() + 2_000_000_000L;
                while ((one.getViewSnapshot().currentBlock().y() == before.players().get(BattleController.Player.ONE).currentBlock().y()
                        || two.getViewSnapshot().currentBlock().y() == before.players().get(BattleController.Player.TWO).currentBlock().y())
                        && System.nanoTime() < deadline) Thread.sleep(5);
                assertTrue(one.getViewSnapshot().currentBlock().y() > before.players().get(BattleController.Player.ONE).currentBlock().y());
                assertTrue(two.getViewSnapshot().currentBlock().y() > before.players().get(BattleController.Player.TWO).currentBlock().y());
                battle.setPaused(true);
                battle.reportGameOver(BattleController.Player.ONE);
                t1.join(500); t2.join(500);
                assertFalse(t1.isAlive()); assertFalse(t2.isAlive());
            } finally {
                battle.close(); t1.interrupt(); t2.interrupt(); t1.join(1000); t2.join(1000);
            }
        }
    }

    @Test
    void pauseCycleCannotSlipBetweenHeldInputValidationAndMovement() throws Exception {
        var revisionRead = new java.util.concurrent.CountDownLatch(1);
        var pulseComplete = new java.util.concurrent.CountDownLatch(1);
        var armed = new java.util.concurrent.atomic.AtomicBoolean();
        var staleMovement = new java.util.concurrent.atomic.AtomicBoolean();
        GameStateManager state = new GameStateManager();
        ScoreManager score = new ScoreManager();
        ActionController action = new ActionController(new Board(), new OBlock(), state, score,
                Difficulty.NORMAL, GameMode.NORMAL, 0) {
            @Override long pauseRevision() {
                long value = super.pauseRevision();
                if (armed.compareAndSet(true, false)) {
                    revisionRead.countDown();
                    try { pulseComplete.await(300, java.util.concurrent.TimeUnit.MILLISECONDS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                return value;
            }
            @Override public void moveLeftAction() {
                if (pulseComplete.getCount() == 0) staleMovement.set(true);
                super.moveLeftAction();
            }
        };
        try (BattleController battle = new BattleController()) {
            battle.registerPlayer(BattleController.Player.ONE, action);
            battle.start();
            GameController[] input = new GameController[1];
            Thread worker = new Thread(() -> {
                try {
                    if (revisionRead.await(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        battle.setPaused(true);
                        battle.setPaused(false);
                        pulseComplete.countDown();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            try {
                SwingUtilities.invokeAndWait(() -> {
                    input[0] = new GameController(new GameLoop(action, state, score), action, state, score,
                            new AppStateManager(AppState.PLAYING), () -> {}, () -> {}, () -> {});
                    JRootPane root = new JRootPane();
                    input[0].installKeyBindings(root);
                    key(root, KeyEvent.VK_LEFT, false);
                    armed.set(true);
                });
                worker.start();
                assertTrue(pulseComplete.await(3, java.util.concurrent.TimeUnit.SECONDS));
                SwingUtilities.invokeAndWait(() -> {});
                assertFalse(staleMovement.get(), "held movement must not commit using a pre-pause revision");
            } finally {
                SwingUtilities.invokeAndWait(() -> { if (input[0] != null) input[0].shutdown(); });
                worker.interrupt();
                worker.join(1000);
            }
        }
    }

    private static ActionController player(Board board, GameStateManager state, ScoreManager score, int animationMs) {
        return new ActionController(board, new OBlock(), state, score,
                Difficulty.NORMAL, GameMode.ITEM, animationMs);
    }

    @Test
    void pauseIsSharedIdempotentAndCannotReviveAFinishedMatch() throws Exception {
        try (BattleController battle = new BattleController()) {
            ActionController one = player(new Board(), new GameStateManager(), new ScoreManager(), 0);
            ActionController two = player(new Board(), new GameStateManager(), new ScoreManager(), 0);
            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);
            battle.start();
            battle.setPaused(true);
            battle.setPaused(true);
            long elapsed = battle.getElapsedPlayMillis();
            for (ActionController action : List.of(one, two)) {
                assertTrue(action.isPaused());
                action.moveLeftAction();
                action.automaticDropAction();
                action.hardDropWithScoreAction();
                assertEquals(3, action.getCurrentBlock().getX());
                assertEquals(0, action.getCurrentBlock().getY());
                assertEquals(0, action.getScore());
            }
            Thread.sleep(80);
            assertEquals(elapsed, battle.getElapsedPlayMillis());
            // 기존 단일 플레이어 API를 사용해도 대전에서는 양쪽에 적용한다.
            one.setPaused(false);
            assertFalse(two.isPaused());
            assertTrue(two.softDropAction());
            battle.reportGameOver(BattleController.Player.ONE);
            elapsed = battle.getElapsedPlayMillis();
            battle.setPaused(false);
            Thread.sleep(30);
            assertEquals(elapsed, battle.getElapsedPlayMillis());
            assertTrue(one.isStopped());
            assertTrue(two.isStopped());
        }
    }

    @Test
    void viewIsDetachedAndBothPlayersAreAvailableInOneMatchSnapshot() {
        Board board = new Board();
        board.getBoard()[22][0] = 11;
        try (BattleController battle = new BattleController()) {
            ActionController one = player(board, new GameStateManager(), new ScoreManager(), 0);
            ActionController two = player(new Board(), new GameStateManager(), new ScoreManager(), 0);
            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);
            battle.start();
            one.receiveAttackRows(List.of(new int[10], new int[10]));
            var matchView = battle.getViewSnapshot();
            var view = matchView.players().get(BattleController.Player.ONE);
            view.board()[22][0] = 0;
            view.currentBlock().cells()[0][0] = 0;
            view.pendingAttackRows().get(0)[0] = 11;
            assertEquals(11, view.board()[22][0]);
            assertNotEquals(0, view.currentBlock().cells()[0][0]);
            assertEquals(0, view.pendingAttackRows().get(0)[0]);
            one.moveLeftAction();
            assertEquals(3, view.currentBlock().x());
            assertEquals(2, one.getViewSnapshot().currentBlock().x());
            assertEquals(2, matchView.players().size());
            assertThrows(UnsupportedOperationException.class, () -> matchView.players().clear());
        }
    }

    @Test
    void pausedDeletionFreezesItsDisplayTimeAndResumesExactlyOneAttack() throws Exception {
        Board board = new Board();
        for (int row : new int[]{21, 22}) {
            Arrays.fill(board.getBoard()[row], 1);
            board.getBoard()[row][3] = board.getBoard()[row][4] = 0;
        }
        try (BattleController battle = new BattleController()) {
            ActionController one = player(board, new GameStateManager(), new ScoreManager(), 220);
            ActionController two = player(new Board(), new GameStateManager(), new ScoreManager(), 220);
            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);
            battle.start();
            one.hardDropAction();
            battle.setPaused(true);
            var view = one.getViewSnapshot();
            Thread.sleep(300);
            assertEquals(view.animationElapsedMs(), one.getViewSnapshot().animationElapsedMs());
            assertTrue(one.isClearing());
            assertEquals(0, two.getPendingAttackCount());
            battle.setPaused(false);
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (one.isClearing() && System.nanoTime() < deadline) Thread.sleep(10);
            assertFalse(one.isClearing());
            assertEquals(2, two.getPendingAttackCount());
            two.hardDropAction();
            assertEquals(0, two.getPendingAttackCount());
            assertEquals(0, two.getCurrentBlock().getY());
        }
    }

    @Test
    void twoKeyProfilesCoexistAndPauseDiscardsBothHeldInputs() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try (BattleController battle = new BattleController()) {
                GameStateManager s1 = new GameStateManager(), s2 = new GameStateManager();
                ScoreManager p1 = new ScoreManager(), p2 = new ScoreManager();
                ActionController one = player(new Board(), s1, p1, 0), two = player(new Board(), s2, p2, 0);
                battle.registerPlayer(BattleController.Player.ONE, one);
                battle.registerPlayer(BattleController.Player.TWO, two);
                battle.start();
                GameLoop l1 = new GameLoop(one, s1, p1), l2 = new GameLoop(two, s2, p2);
                AppStateManager app = new AppStateManager(AppState.PLAYING);
                GameController c1 = new GameController(l1, one, s1, p1, app, () -> {}, () -> {}, () -> {},
                        new GameController.KeyBindings(KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_W, KeyEvent.VK_SPACE), true);
                GameController c2 = new GameController(l2, two, s2, p2, app, () -> {}, () -> {}, () -> {},
                        new GameController.KeyBindings(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_ENTER), false);
                try {
                    JRootPane root = new JRootPane();
                    c1.installKeyBindings(root);
                    c2.installKeyBindings(root);
                    key(root, KeyEvent.VK_A, false);
                    key(root, KeyEvent.VK_RIGHT, false);
                    assertEquals(2, one.getCurrentBlock().getX());
                    assertEquals(4, two.getCurrentBlock().getX());
                    key(root, KeyEvent.VK_P, false);
                    assertTrue(l1.isPaused());
                    assertTrue(l2.isPaused());
                    key(root, KeyEvent.VK_P, true);
                    key(root, KeyEvent.VK_P, false);
                    key(root, KeyEvent.VK_P, true);
                    assertFalse(l2.isPaused());
                    // 재등록해도 상대 플레이어의 키 연결은 유지한다.
                    c1.installKeyBindings(root);
                    key(root, KeyEvent.VK_RIGHT, true);
                    key(root, KeyEvent.VK_RIGHT, false);
                    assertEquals(5, two.getCurrentBlock().getX());
                    c2.shutdown();
                    key(root, KeyEvent.VK_A, true);
                    key(root, KeyEvent.VK_A, false);
                    assertEquals(1, one.getCurrentBlock().getX());
                } finally { c1.shutdown(); c2.shutdown(); }
            }
        });
    }

    private static void key(JRootPane root, int code, boolean released) {
        Object name = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(code, 0, released));
        assertNotNull(name, "binding for " + code);
        root.getActionMap().get(name).actionPerformed(new ActionEvent(root, 0, "test"));
    }

    @Test
    void settingsPausesBothAndRestoresThePreviousMatchState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try (BattleController battle = new BattleController()) {
                GameStateManager s1 = new GameStateManager(), s2 = new GameStateManager();
                ScoreManager p1 = new ScoreManager(), p2 = new ScoreManager();
                ActionController one = player(new Board(), s1, p1, 0), two = player(new Board(), s2, p2, 0);
                battle.registerPlayer(BattleController.Player.ONE, one);
                battle.registerPlayer(BattleController.Player.TWO, two);
                battle.start();
                GameController controller = new GameController(new GameLoop(one, s1, p1), one, s1, p1,
                        new AppStateManager(AppState.PLAYING), () -> {
                            assertTrue(one.isPaused());
                            assertTrue(two.isPaused());
                        }, () -> {}, () -> {});
                try {
                    JRootPane root = new JRootPane();
                    controller.installKeyBindings(root);
                    for (boolean paused : new boolean[]{false, true}) {
                        battle.setPaused(paused);
                        key(root, KeyEvent.VK_ESCAPE, false);
                        key(root, KeyEvent.VK_ESCAPE, true);
                        assertEquals(paused, one.isPaused());
                        assertEquals(paused, two.isPaused());
                    }
                } finally { controller.shutdown(); }
            }
        });
    }

    @Test
    void externalQuickPauseAndResumeDiscardHeldKeysOnBothPlayers() throws Exception {
        try (BattleController battle = new BattleController()) {
            GameStateManager s1 = new GameStateManager(), s2 = new GameStateManager();
            ScoreManager p1 = new ScoreManager(), p2 = new ScoreManager();
            ActionController one = player(new Board(), s1, p1, 0), two = player(new Board(), s2, p2, 0);
            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);
            battle.start();
            GameController[] controllers = new GameController[2];
            JRootPane[] roots = new JRootPane[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JRootPane root = roots[0] = new JRootPane();
                    var app = new AppStateManager(AppState.PLAYING);
                    controllers[0] = new GameController(new GameLoop(one, s1, p1), one, s1, p1, app,
                            () -> {}, () -> {}, () -> {},
                            new GameController.KeyBindings(KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_W, KeyEvent.VK_SPACE), true);
                    controllers[1] = new GameController(new GameLoop(two, s2, p2), two, s2, p2, app,
                            () -> {}, () -> {}, () -> {},
                            new GameController.KeyBindings(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_ENTER), false);
                    for (var controller : controllers) controller.installKeyBindings(root);
                    key(root, KeyEvent.VK_A, false);
                    key(root, KeyEvent.VK_RIGHT, false);
                });
                battle.setPaused(true);
                battle.setPaused(false);
                Thread.sleep(400); // EDT 반복 입력 타이머가 실제로 실행되도록 별도 스레드에서 대기한다.
                SwingUtilities.invokeAndWait(() -> {
                    assertEquals(2, one.getCurrentBlock().getX());
                    assertEquals(4, two.getCurrentBlock().getX());
                    key(roots[0], KeyEvent.VK_A, false); // OS의 누름 반복도 release 전에는 무시한다.
                    key(roots[0], KeyEvent.VK_RIGHT, false);
                    assertEquals(2, one.getCurrentBlock().getX());
                    assertEquals(4, two.getCurrentBlock().getX());
                    key(roots[0], KeyEvent.VK_A, true);
                    key(roots[0], KeyEvent.VK_A, false);
                    assertEquals(1, one.getCurrentBlock().getX());
                });
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    for (var controller : controllers) if (controller != null) controller.shutdown();
                });
            }
        }
    }

    @Test
    void conflictingBindingsDoNotOverwriteOrRemoveTheFirstPlayer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameStateManager state = new GameStateManager();
            ScoreManager score = new ScoreManager();
            ActionController action = player(new Board(), state, score, 0);
            GameLoop loop = new GameLoop(action, state, score);
            var app = new AppStateManager(AppState.PLAYING);
            GameController first = new GameController(loop, action, state, score, app, () -> {}, () -> {}, () -> {});
            GameController second = new GameController(loop, action, state, score, app, () -> {}, () -> {}, () -> {});
            try {
                JRootPane root = new JRootPane();
                first.installKeyBindings(root);
                assertThrows(IllegalArgumentException.class, () -> second.installKeyBindings(root));
                second.shutdown();
                key(root, KeyEvent.VK_LEFT, false);
                assertEquals(2, action.getCurrentBlock().getX());
            } finally { first.shutdown(); second.shutdown(); action.shutdown(); }
        });
    }
}
