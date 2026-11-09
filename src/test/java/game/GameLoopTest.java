package game;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

class GameLoopTest {
    @Test
    void resumeUsesTheUnspentPartOfTheDropIntervalAndShutdownWakesTheLoop() throws Exception {
        Thread thread = new Thread(loop);
        thread.start();
        try {
            long readyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (thread.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < readyDeadline) Thread.yield();
            assertEquals(Thread.State.TIMED_WAITING, thread.getState());
            Thread.sleep(400);
            action.setPaused(true);
            Thread.sleep(1100);
            assertEquals(0, action.getCurrentBlock().getY());
            action.setPaused(false);
            Thread.sleep(200);
            assertEquals(0, action.getCurrentBlock().getY());
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(550);
            while (action.getCurrentBlock().getY() == 0 && System.nanoTime() < deadline) Thread.sleep(5);
            assertEquals(1, action.getCurrentBlock().getY());
            action.setPaused(true);
            action.shutdown();
            thread.join(500);
            assertFalse(thread.isAlive());
        } finally { action.shutdown(); thread.interrupt(); thread.join(1000); }
    }

    private GameStateManager manager;
    private GameLoop loop;
    private ActionController action;

    @BeforeEach 
    void setUp() {
        manager = new GameStateManager();
        ScoreManager score = new ScoreManager();
        action = new ActionController(new Board(), new OBlock(), manager, score,
                Difficulty.NORMAL, GameMode.NORMAL, 0);
        loop = new GameLoop(action, manager, score);
    }

    @Test // 일시정지 테스트
    void startsUnpausedAndCanTogglePause() {
        
        assertFalse(loop.isPaused());

        loop.togglePause();
        assertTrue(loop.isPaused());

        loop.togglePause();
        assertFalse(loop.isPaused());
    }

    @Test // 스레드 인터럽트 테스트
    void runStopsWhenItsThreadIsInterrupted() throws InterruptedException {

        Thread thread = new Thread(loop);

        // 스레드 실행 -> 인터럽트 -> 1초 대기 -> 종료 확인
        thread.start();
        thread.interrupt();
        thread.join(1000);

        assertFalse(thread.isAlive());
    }

    @Test
    void automaticallyDropsAtTheConfiguredOneSecondInterval() throws InterruptedException {
        Thread thread = new Thread(loop);
        thread.start();
        try {
            Thread.sleep(600);
            assertEquals(0, action.getCurrentBlock().getY());

            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1600);
            while (action.getCurrentBlock().getY() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(1, action.getCurrentBlock().getY());
        } finally {
            thread.interrupt();
            thread.join(1000);
            action.shutdown();
        }
    }

    @Test
    void pauseStopsAutomaticDropAndResumeRestartsIt() throws InterruptedException {
        Thread thread = new Thread(loop);
        loop.togglePause();
        thread.start();
        try {
            Thread.sleep(1200);
            assertEquals(0, action.getCurrentBlock().getY());

            loop.togglePause();
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1800);
            while (action.getCurrentBlock().getY() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(1, action.getCurrentBlock().getY());
        } finally {
            thread.interrupt();
            thread.join(1000);
            action.shutdown();
        }
    }
}
