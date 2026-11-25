package game;

import static game.BattleController.Player.ONE;
import static game.BattleController.Player.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import difficulty.Difficulty;
import game.LocalBattleSession.Opponent;

class ComputerBattleIntegrationTest {

    @ParameterizedTest
    @EnumSource(BattleMode.class)
    void computerLocksAPieceWhilePlayerOneKeepsKeyboardControl(BattleMode mode) {
        BattleSettings settings = new BattleSettings(mode, mode == BattleMode.TIME_LIMIT ? 180 : 0);
        try (LocalBattleSession session = new LocalBattleSession(settings, Difficulty.NORMAL, Opponent.CPU)) {
            session.start();

            assertTrue(session.acceptsKeyboardInput(ONE));
            assertFalse(session.acceptsKeyboardInput(TWO));
            assertEquals(settings.getGameMode(), session.getPlayer(TWO).action().getMode());
            awaitComputerLock(session);
            assertFalse(hasLockedPiece(session, ONE));
        }
    }

    @Test
    void pauseFreezesComputerAndMatchClockAndResumingContinuesPlay() throws InterruptedException {
        BattleSettings settings = new BattleSettings(BattleMode.TIME_LIMIT, 180);
        try (LocalBattleSession session = new LocalBattleSession(settings, Difficulty.NORMAL, Opponent.CPU)) {
            BattleController battle = session.getBattleController();
            synchronized (battle) {
                session.start();
                battle.setPaused(true);
            }
            long pausedElapsed = battle.getElapsedPlayMillis();
            var pausedPiece = battle.getViewSnapshot().players().get(TWO).currentBlock();

            Thread.sleep(AiPlayer.DEFAULT_INPUT_INTERVAL_MILLIS + 100);
            assertEquals(pausedElapsed, battle.getElapsedPlayMillis());
            var currentPiece = battle.getViewSnapshot().players().get(TWO).currentBlock();
            assertEquals(pausedPiece.x(), currentPiece.x());
            assertEquals(pausedPiece.y(), currentPiece.y());
            assertFalse(hasLockedPiece(session, TWO));

            battle.setPaused(false);
            awaitComputerLock(session);
            assertTrue(battle.getElapsedPlayMillis() > pausedElapsed);
        }
    }

    @Test
    void stoppingComputerBattlePreservesWinnerAndStopsBothPlayers() {
        BattleSettings settings = new BattleSettings(BattleMode.NORMAL, 0);
        try (LocalBattleSession session = new LocalBattleSession(settings, Difficulty.NORMAL, Opponent.CPU)) {
            session.start();
            BattleController battle = session.getBattleController();
            battle.reportGameOver(TWO);
            session.close();

            assertEquals(BattleController.Outcome.ONE_WINS, battle.getOutcome());
            assertTrue(session.getPlayer(ONE).action().isStopped());
            assertTrue(session.getPlayer(TWO).action().isStopped());
            assertThrows(IllegalStateException.class, session::start);
        }
    }

    @Test
    void ordinaryLocalBattleStillAcceptsBothPlayers() {
        try (LocalBattleSession session =
                new LocalBattleSession(new BattleSettings(BattleMode.NORMAL, 0), Difficulty.NORMAL)) {
            assertTrue(session.acceptsKeyboardInput(ONE));
            assertTrue(session.acceptsKeyboardInput(TWO));
        }
    }

    private static void awaitComputerLock(LocalBattleSession session) {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            while (!hasLockedPiece(session, TWO)) {
                Thread.sleep(20);
            }
        });
    }

    private static boolean hasLockedPiece(LocalBattleSession session, BattleController.Player player) {
        int[][] cells = session.getBattleController().getViewSnapshot().players().get(player).board();
        return Arrays.stream(cells).anyMatch(row -> Arrays.stream(row).anyMatch(cell -> cell != 0));
    }
}
