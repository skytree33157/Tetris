package game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import difficulty.Difficulty;

class LocalBattleSessionTest {

    @ParameterizedTest
    @EnumSource(BattleMode.class)
    void appliesModeAndDifficultyToBothPlayers(BattleMode mode) {
        int seconds = mode == BattleMode.TIME_LIMIT ? 180 : 0;
        BattleSettings settings = new BattleSettings(mode, seconds);

        try (LocalBattleSession session =
                new LocalBattleSession(settings, Difficulty.HARD)) {

            for (BattleController.Player player
                    : BattleController.Player.values()) {
                LocalBattleSession.PlayerGame game =
                        session.getPlayer(player);

                assertEquals(
                        settings.getGameMode(),
                        game.action().getMode()
                );
                assertEquals(
                        Difficulty.HARD,
                        game.state().getDifficulty()
                );
            }
        }
    }

    @Test
    void closingRunningSessionStopsBothPlayers() {
        BattleSettings settings =
                new BattleSettings(BattleMode.NORMAL, 0);

        try (LocalBattleSession session =
                new LocalBattleSession(settings, Difficulty.NORMAL)) {

            session.start();
            session.close();

            assertFalse(
                    session.getBattleController().isRunning()
            );
            assertEquals(
                    BattleController.Outcome.CANCELLED,
                    session.getBattleController().getOutcome()
            );

            for (BattleController.Player player
                    : BattleController.Player.values()) {
                assertTrue(
                        session.getPlayer(player).action().isStopped()
                );
            }

            assertThrows(
                    IllegalStateException.class,
                    session::start
            );
        }
    }

    @Test
    void timeLimitAutomaticallyEndsRunningSession() {
        BattleSettings settings =
                new BattleSettings(BattleMode.TIME_LIMIT, 1);

        try (LocalBattleSession session =
                new LocalBattleSession(settings, Difficulty.NORMAL)) {

            session.start();

            assertTimeoutPreemptively(
                    Duration.ofSeconds(5),
                    () -> {
                        while (session.getBattleController().isRunning()) {
                            Thread.sleep(10);
                        }
                    }
            );

            assertEquals(
                    BattleController.EndReason.TIME_LIMIT,
                    session.getBattleController().getEndReason()
            );

            for (BattleController.Player player
                    : BattleController.Player.values()) {
                assertTrue(
                        session.getPlayer(player).action().isStopped()
                );
            }
        }
    }

    @Test
    void closingFinishedSessionKeepsTopOutResult() {
        BattleSettings settings =
                new BattleSettings(BattleMode.TIME_LIMIT, 180);

        try (LocalBattleSession session =
                new LocalBattleSession(settings, Difficulty.NORMAL)) {

            session.start();

            session.getBattleController().reportGameOver(
                    BattleController.Player.ONE
            );

            session.close();

            assertEquals(
                    BattleController.Outcome.TWO_WINS,
                    session.getBattleController().getOutcome()
            );
            assertEquals(
                    BattleController.EndReason.TOP_OUT,
                    session.getBattleController().getEndReason()
            );
        }
    }

    @ParameterizedTest
    @CsvSource({
        "NORMAL, true",
        "NORMAL, false",
        "ITEM, true",
        "ITEM, false",
        "TIME_LIMIT, true",
        "TIME_LIMIT, false"
    })
    void finishedMatchDoesNotChangeScoreboard(
            BattleMode mode,
            boolean hasExistingRecords,
            @TempDir Path tempDir) throws IOException {

        Path scoreFile = tempDir.resolve("scores.txt");

        byte[] originalRecords =
                "기존기록,60000,HARD,ITEM\n"
                        .getBytes(StandardCharsets.UTF_8);

        if (hasExistingRecords) {
            Files.write(scoreFile, originalRecords);
        }

        String previousDataDir = System.getProperty("tetris.dataDir");

        try {
            System.setProperty("tetris.dataDir", tempDir.toString());

            int seconds = mode == BattleMode.TIME_LIMIT ? 180 : 0;
            BattleSettings settings = new BattleSettings(mode, seconds);

            try (LocalBattleSession session =
                    new LocalBattleSession(settings, Difficulty.NORMAL)) {

                session.start();

                session.getPlayer(BattleController.Player.ONE)
                        .action().softDropAction();
                session.getPlayer(BattleController.Player.TWO)
                        .action().softDropAction();

                session.getBattleController().reportGameOver(
                        BattleController.Player.TWO
                );
            }

            if (hasExistingRecords) {
                assertArrayEquals(
                        originalRecords,
                        Files.readAllBytes(scoreFile)
                );
            } else {
                assertFalse(Files.exists(scoreFile));
            }
        } finally {
            if (previousDataDir == null) {
                System.clearProperty("tetris.dataDir");
            } else {
                System.setProperty("tetris.dataDir", previousDataDir);
            }
        }
    }
}