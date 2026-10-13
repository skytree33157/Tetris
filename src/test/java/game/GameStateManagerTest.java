package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateManagerTest {

    private GameStateManager manager;

    @BeforeEach
    void setUp() {
        manager = new GameStateManager();
    }

    @Test // 초기 상태 테스트
          // (초기값 : dropSpeed=1000, countBlock=0, countClearLine=0, totalLinesCleared=0, currentLevel=1, gameOver=false)
    void startsWithDefaultState() {
        assertAll(
                () -> assertEquals(1000, manager.getDropSpeed()),
                () -> assertEquals(0, manager.getCountBlock()),
                () -> assertEquals(0, manager.getCountClearLine()),
                () -> assertEquals(0, manager.getTotalLinesCleared()),
                () -> assertEquals(1, manager.getCurrentLevel()),
                () -> assertFalse(manager.isGameOver())
        );
    }

    @Test // 블록 생성에 따른 레벨업 테스트
    void levelsUpBlocks() {
        int speedDecreaseAmount = manager.getLevelSettings()[0];
        int blocksForLevelUp = manager.getLevelSettings()[2];
        int initialDropSpeed = manager.getLevelSettings()[4];
        
        // 레벨업에 필요한 블록 생성
        for (int i = 0; i < blocksForLevelUp; i++) {
            manager.updateLevelUp(0);
        }
        // 레벨업 후 현재 레벨, 드롭 속도, 블록 카운트 확인
        assertEquals(2, manager.getCurrentLevel());
        assertEquals(initialDropSpeed - speedDecreaseAmount, manager.getDropSpeed());
        assertEquals(0, manager.getCountBlock());
    }

    @Test // 줄 삭제에 따른 레벨업 테스트
    void levelsUpClearedLinesAndKeepsTotal() {
        int speedDecreaseAmount = manager.getLevelSettings()[0];
        int linesForLevelUp = manager.getLevelSettings()[3];
        int initialDropSpeed = manager.getLevelSettings()[4];
        // 레벨업에 필요한 만큼 줄 삭제
        manager.updateLevelUp(linesForLevelUp);
        // 레벨업 후 현재 레벨, 드롭 속도, 삭제한 줄 수, 누적 줄 수 확인
        assertEquals(2, manager.getCurrentLevel());
        assertEquals(initialDropSpeed - speedDecreaseAmount, manager.getDropSpeed());
        assertEquals(0, manager.getCountClearLine());
        assertEquals(linesForLevelUp, manager.getTotalLinesCleared());
    }

    @Test // 최소 드롭 속도 테스트
    void dropSpeedDoesNotGoBelowMinimum() {
        int linesForLevelUp = manager.getLevelSettings()[3];
        int minDropSpeed = manager.getLevelSettings()[1];
        
        // 매우 큰 수만큼 레벨업 시도 후 드롭 속도 확인
        for (int i = 0; i < 100000; i++) {
            manager.updateLevelUp(linesForLevelUp);
        }

        assertEquals(minDropSpeed, manager.getDropSpeed());
    }

    @Test // 게임 오버 설정 테스트
    void canSetGameOver() {
        manager.setGameOver(true);

        assertTrue(manager.isGameOver());
    }
}