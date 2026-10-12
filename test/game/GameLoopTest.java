package game;

import blocks.tetromino.OBlock;
import board.Board;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

class GameLoopTest {
    private GameStateManager manager;
    private GameLoop loop;
    private ScoreManager scoreManager;

    @BeforeEach 
    void setUp() {
        manager = new GameStateManager();
        scoreManager = new ScoreManager();
        loop = new GameLoop(new ActionController(new Board(), new OBlock(), manager, scoreManager), manager, scoreManager);
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

    @Test // 자동 낙하 점수 테스트
    void awardsAutoDropPointsOnlyWhenBlockMoves() {
        assertSingleTickScore(true, 1);
        assertSingleTickScore(false, 0);
    }

    // 단일 틱 점수 검증 헬퍼 메서드
    private void assertSingleTickScore(boolean moved, int expectedScore) {
        // 단일 틱 동안 블록이 이동했는지 여부에 따라 점수를 검증
        GameStateManager tickManager = new GameStateManager() {
            @Override
            public int getDropSpeed() {
                return 1;
            }
        };
        ScoreManager tickScoreManager = new ScoreManager();
        int[] moveAttempts = {0};
        ActionController actionController = new ActionController(
                new Board(), new OBlock(), tickManager, tickScoreManager) {
            @Override
            public synchronized boolean moveDownAction() {
                moveAttempts[0]++;
                tickManager.setGameOver(true);
                return moved;
            }
        };
        new GameLoop(actionController, tickManager, tickScoreManager).run();

        assertEquals(1, moveAttempts[0]);
        assertEquals(expectedScore, tickScoreManager.getScore());
    }
}