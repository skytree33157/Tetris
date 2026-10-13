package game;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

class GameLoopTest {
    private GameStateManager manager;
    private GameLoop loop;

    @BeforeEach 
    void setUp() {
        manager = new GameStateManager();
        loop = new GameLoop(
                new ActionController(new Board(), new OBlock(), manager, new ScoreManager(),
                        Difficulty.NORMAL, GameMode.NORMAL, 0),
                manager, new ScoreManager());
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
}