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

        // 최대 레벨에 도달한 뒤에도 드롭 속도는 최솟값을 유지한다.
        for (int i = 0; i < 10; i++) {
            manager.updateLevelUp(linesForLevelUp);
        }

        assertEquals(minDropSpeed, manager.getDropSpeed());
        assertEquals(10, manager.getCurrentLevel());
    }

    @Test // 난이도별 속도 감소 테스트
    void lineLevelUpUsesDifficultySpecificSpeedDecrease() {
        int linesForLevelUp = manager.getLevelSettings()[3];
        int initialDropSpeed = manager.getDropSpeed();
        int[] expectedDecreases = {80, 100, 120};
        difficulty.Difficulty[] difficulties = difficulty.Difficulty.values();

        for (int i = 0; i < difficulties.length; i++) {
            GameStateManager difficultyManager = new GameStateManager();
            difficultyManager.setDifficulty(difficulties[i]);
            difficultyManager.updateLevelUp(linesForLevelUp);

            assertEquals(initialDropSpeed - expectedDecreases[i], difficultyManager.getDropSpeed());
            assertEquals(2, difficultyManager.getCurrentLevel());
        }
    }

    @Test // 슬로우 아이템 적용 테스트
    void slowItemRestoresDropDelayButNeverExceedsMaximum() {
        for (int i = 0; i < 50; i++) {
            manager.updateLevelUp(0);
        }
        assertEquals(500, manager.getDropSpeed());

        manager.applySlowItem(1);
        assertEquals(1000, manager.getDropSpeed());
        manager.applySlowItem(2);
        assertEquals(1000, manager.getDropSpeed());
    }

    @Test // 아이템 스폰 조건 테스트
    void itemSpawnRequiresCrossingATenLineBoundary() {
        assertFalse(manager.shouldSpawnItem(0));

        manager.updateLevelUp(9);
        assertFalse(manager.shouldSpawnItem(0));
        manager.updateLevelUp(1);
        assertTrue(manager.shouldSpawnItem(9));
        assertFalse(manager.shouldSpawnItem(10));

        manager.updateLevelUp(10);
        assertTrue(manager.shouldSpawnItem(10));
        assertFalse(manager.shouldSpawnItem(20));
    }

    @Test // 게임 오버 설정 테스트
    void canSetGameOver() {
        manager.setGameOver(true);

        assertTrue(manager.isGameOver());
    }
}