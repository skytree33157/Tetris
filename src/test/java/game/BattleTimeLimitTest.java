package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

class BattleTimeLimitTest {

    private BattleController battle;
    private AtomicLong elapsedMillis;

    @BeforeEach
    void setUp() {
        battle = new BattleController();
        elapsedMillis = new AtomicLong();

        battle.registerPlayer(
                BattleController.Player.ONE,
                createPlayer()
        );
        battle.registerPlayer(
                BattleController.Player.TWO,
                createPlayer()
        );
    }

    @AfterEach
    void tearDown() {
        battle.close();
    }

    private ActionController createPlayer() {
        return new ActionController(
                new Board(),
                new OBlock(),
                new GameStateManager(),
                new ScoreManager(),
                Difficulty.NORMAL,
                GameMode.NORMAL,
                0
        );
    }

    private BattleTimeLimit createTimeLimit(
            BattleMode mode,
            int seconds) {
        return new BattleTimeLimit(
                new BattleSettings(mode, seconds),
                battle,
                elapsedMillis::get
        );
    }

    @ParameterizedTest
    @CsvSource({
        "179999, false",
        "180000, true",
        "180001, true"
    })
    void finishesOnlyWhenTimeLimitIsReached(
            long elapsed,
            boolean expectedFinished) {
        BattleTimeLimit timeLimit =
                createTimeLimit(BattleMode.TIME_LIMIT, 180);

        battle.start();
        elapsedMillis.set(elapsed);

        assertEquals(
                expectedFinished,
                timeLimit.checkAndFinish(100, 50)
        );

        if (expectedFinished) {
            assertEquals(
                    BattleController.Outcome.ONE_WINS,
                    battle.getOutcome()
            );
            assertEquals(
                    BattleController.EndReason.TIME_LIMIT,
                    battle.getEndReason()
            );
            assertFalse(battle.isRunning());
        } else {
            assertEquals(
                    BattleController.Outcome.IN_PROGRESS,
                    battle.getOutcome()
            );
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = BattleMode.class,
            names = {"NORMAL", "ITEM"}
    )
    void untimedModesDoNotFinishByTime(BattleMode mode) {
        BattleTimeLimit timeLimit = createTimeLimit(mode, 0);

        battle.start();
        elapsedMillis.set(1_000_000L);

        assertFalse(timeLimit.checkAndFinish(100, 50));
        assertEquals(
                BattleController.Outcome.IN_PROGRESS,
                battle.getOutcome()
        );
    }

    @Test
    void doesNotFinishWhilePaused() {
        BattleTimeLimit timeLimit =
                createTimeLimit(BattleMode.TIME_LIMIT, 180);

        battle.start();
        battle.setPaused(true);
        elapsedMillis.set(180_000L);

        assertFalse(timeLimit.checkAndFinish(100, 50));
        assertEquals(
                BattleController.Outcome.IN_PROGRESS,
                battle.getOutcome()
        );
    }
}