package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

class BattleTimeLimitTimerTest {

    private BattleController battle;
    private BattleTimeLimit timeLimit;
    private AtomicLong elapsedMillis;
    private ActionController one;
    private ActionController two;

    @BeforeEach
    void setUp() {
        battle = new BattleController();
        elapsedMillis = new AtomicLong();

        one = createPlayer();
        two = createPlayer();

        battle.registerPlayer(BattleController.Player.ONE, one);
        battle.registerPlayer(BattleController.Player.TWO, two);

        timeLimit = new BattleTimeLimit(
                new BattleSettings(BattleMode.TIME_LIMIT, 180),
                battle,
                elapsedMillis::get
        );
    }

    @AfterEach
    void tearDown() {
        timeLimit.close();
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

    @Test
    void timerAutomaticallyFinishesExpiredMatch()
            throws InterruptedException {
        battle.start();
        elapsedMillis.set(180_000L);

        CountDownLatch checked = new CountDownLatch(1);

        timeLimit.start(
                () -> {
                    checked.countDown();
                    return 100;
                },
                () -> 50
        );

        // 타이머가 실제로 검사 작업을 실행했는지 확인
        assertTrue(checked.await(5, TimeUnit.SECONDS));

        // 타이머와 같은 경기 잠금을 사용하는 조회이므로
        // 진행 중인 검사 작업이 끝난 뒤 결과를 읽음
        assertEquals(
                BattleController.Outcome.ONE_WINS,
                battle.getOutcome()
        );
        assertEquals(
                BattleController.EndReason.TIME_LIMIT,
                battle.getEndReason()
        );
        assertFalse(battle.isRunning());
        assertTrue(one.isStopped());
        assertTrue(two.isStopped());
    }

    @Test
    void closingTimerPreventsFurtherTimeLimitChecks() {
        battle.start();
        timeLimit.start(() -> 100, () -> 50);

        timeLimit.close();
        elapsedMillis.set(180_000L);

        assertFalse(timeLimit.checkAndFinish(100, 50));
        assertTrue(battle.isRunning());
        assertEquals(
                BattleController.Outcome.IN_PROGRESS,
                battle.getOutcome()
        );

        assertThrows(
                IllegalStateException.class,
                () -> timeLimit.start(() -> 100, () -> 50)
        );
    }

    @Test
    void cannotStartTimerBeforeMatchStarts() {
        assertThrows(
                IllegalStateException.class,
                () -> timeLimit.start(() -> 100, () -> 50)
        );
    }
}