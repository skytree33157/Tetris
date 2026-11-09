package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

class BattleConcurrencyTest {

    @ParameterizedTest(name = "명시적 종료: {0}")
    @ValueSource(booleans = {true, false})
    void terminationStopsGameLoopsAndTimer(
            boolean closeExplicitly) throws Exception {

        try (BattleFixture fixture = new BattleFixture(true)) {
            fixture.start();

            assertTrue(
                    fixture.timerObserved.await(5, TimeUnit.SECONDS),
                    "예약된 타이머 작업이 실행되어야 합니다."
            );

            if (closeExplicitly) {
                fixture.timeLimit.close();
                fixture.battle.close();
            } else {
                fixture.elapsedMillis.set(1_000L);
            }

            fixture.assertWorkersStopped();

            assertEquals(
                    closeExplicitly
                            ? BattleController.EndReason.CANCELLED
                            : BattleController.EndReason.TIME_LIMIT,
                    fixture.battle.getEndReason()
            );
        }
    }

    @Test
    void pauseLongerThanLimitThenResumeFinishesNormally()
            throws Exception {

        try (BattleFixture fixture = new BattleFixture(false)) {
            synchronized (fixture.battle) {
                fixture.start();
                fixture.battle.setPaused(true);
            }

            long pausedElapsed =
                    fixture.battle.getElapsedPlayMillis();

            Thread.sleep(1_200);

            assertTrue(fixture.battle.isPaused());
            assertTrue(fixture.battle.isRunning());
            assertEquals(
                    BattleController.Outcome.IN_PROGRESS,
                    fixture.battle.getOutcome()
            );
            assertEquals(
                    pausedElapsed,
                    fixture.battle.getElapsedPlayMillis()
            );

            fixture.battle.setPaused(false);

            assertTrue(
                    fixture.timerObserved.await(5, TimeUnit.SECONDS),
                    "재개 후 타이머 검사가 실행되어야 합니다."
            );

            fixture.assertWorkersStopped();

            assertEquals(
                    BattleController.EndReason.TIME_LIMIT,
                    fixture.battle.getEndReason()
            );
        }
    }

    @RepeatedTest(10)
    void timeoutAndTopOutRaceKeepsExactlyOneResult()
            throws Exception {

        try (BattleFixture fixture = new BattleFixture(true)) {
            fixture.battle.start();
            fixture.elapsedMillis.set(1_000L);

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch startGate = new CountDownLatch(1);

            FutureTask<Boolean> timeoutTask = new FutureTask<>(() -> {
                ready.countDown();
                assertTrue(startGate.await(5, TimeUnit.SECONDS));

                return fixture.timeLimit.checkAndFinish(100, 50);
            });

            FutureTask<Boolean> topOutTask = new FutureTask<>(() -> {
                ready.countDown();
                assertTrue(startGate.await(5, TimeUnit.SECONDS));

                return fixture.battle.reportGameOver(
                        BattleController.Player.ONE
                );
            });

            Thread timeoutThread =
                    new Thread(timeoutTask, "test-timeout-race");
            Thread topOutThread =
                    new Thread(topOutTask, "test-top-out-race");

            timeoutThread.setDaemon(true);
            topOutThread.setDaemon(true);

            try {
                timeoutThread.start();
                topOutThread.start();

                assertTrue(ready.await(5, TimeUnit.SECONDS));
                startGate.countDown();

                boolean timeoutWon =
                        timeoutTask.get(5, TimeUnit.SECONDS);
                boolean topOutWon =
                        topOutTask.get(5, TimeUnit.SECONDS);

                assertTrue(
                        timeoutWon != topOutWon,
                        "시간 만료와 게임 오버 중 하나만 성공해야 합니다."
                );

                BattleController.Outcome outcome =
                        fixture.battle.getOutcome();
                BattleController.EndReason reason =
                        fixture.battle.getEndReason();

                assertEquals(
                        timeoutWon
                                ? BattleController.Outcome.ONE_WINS
                                : BattleController.Outcome.TWO_WINS,
                        outcome
                );
                assertEquals(
                        timeoutWon
                                ? BattleController.EndReason.TIME_LIMIT
                                : BattleController.EndReason.TOP_OUT,
                        reason
                );

                assertFalse(
                        fixture.timeLimit.checkAndFinish(0, 999)
                );
                assertFalse(
                        fixture.battle.reportGameOver(
                                BattleController.Player.TWO
                        )
                );

                assertEquals(outcome, fixture.battle.getOutcome());
                assertEquals(reason, fixture.battle.getEndReason());
                assertTrue(fixture.one.isStopped());
                assertTrue(fixture.two.isStopped());
            } finally {
                startGate.countDown();

                timeoutThread.interrupt();
                topOutThread.interrupt();

                timeoutThread.join(1_000);
                topOutThread.join(1_000);
            }
        }
    }

    private static void assertThreadStopped(Thread thread)
            throws InterruptedException {
        thread.join(3_000);

        assertFalse(
                thread.isAlive(),
                thread.getName() + " 스레드가 종료되어야 합니다."
        );
    }

    private static final class BattleFixture implements AutoCloseable {

        final BattleController battle = new BattleController();
        final AtomicLong elapsedMillis = new AtomicLong();

        final AtomicReference<Thread> timerThread =
                new AtomicReference<>();
        final CountDownLatch timerObserved = new CountDownLatch(1);

        final ActionController one;
        final ActionController two;
        final BattleTimeLimit timeLimit;

        final Thread oneThread;
        final Thread twoThread;

        BattleFixture(boolean useControlledTime) {
            GameStateManager oneState = new GameStateManager();
            GameStateManager twoState = new GameStateManager();

            ScoreManager oneScore = new ScoreManager();
            ScoreManager twoScore = new ScoreManager();

            one = createPlayer(oneState, oneScore);
            two = createPlayer(twoState, twoScore);

            battle.registerPlayer(BattleController.Player.ONE, one);
            battle.registerPlayer(BattleController.Player.TWO, two);

            BattleSettings settings =
                    new BattleSettings(BattleMode.TIME_LIMIT, 1);

            timeLimit = useControlledTime
                    ? new BattleTimeLimit(
                            settings, battle, elapsedMillis::get)
                    : new BattleTimeLimit(settings, battle);

            oneThread = new Thread(
                    new GameLoop(one, oneState, oneScore),
                    "test-battle-player-one"
            );
            twoThread = new Thread(
                    new GameLoop(two, twoState, twoScore),
                    "test-battle-player-two"
            );

            oneThread.setDaemon(true);
            twoThread.setDaemon(true);
        }

        private static ActionController createPlayer(
                GameStateManager state,
                ScoreManager score) {
            return new ActionController(
                    new Board(),
                    new OBlock(),
                    state,
                    score,
                    Difficulty.NORMAL,
                    GameMode.NORMAL,
                    0
            );
        }

        void start() {
            battle.start();

            timeLimit.start(
                    () -> {
                        timerThread.compareAndSet(
                                null,
                                Thread.currentThread()
                        );
                        timerObserved.countDown();
                        return one.getScore();
                    },
                    two::getScore
            );

            oneThread.start();
            twoThread.start();
        }

        void assertWorkersStopped() throws InterruptedException {
            assertThreadStopped(oneThread);
            assertThreadStopped(twoThread);

            Thread observedTimer = timerThread.get();
            assertNotNull(observedTimer);
            assertThreadStopped(observedTimer);

            assertTrue(one.isStopped());
            assertTrue(two.isStopped());
        }

        @Override
        public void close() throws InterruptedException {
            timeLimit.close();
            battle.close();

            oneThread.interrupt();
            twoThread.interrupt();

            oneThread.join(1_000);
            twoThread.join(1_000);

            Thread observedTimer = timerThread.get();
            if (observedTimer != null) {
                observedTimer.join(1_000);
            }
        }
    }
}